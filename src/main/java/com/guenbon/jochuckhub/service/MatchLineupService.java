package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.request.SaveLineupRequest;
import com.guenbon.jochuckhub.dto.response.MatchLineupResponse;
import com.guenbon.jochuckhub.entity.AttendStatus;
import com.guenbon.jochuckhub.entity.Match;
import com.guenbon.jochuckhub.entity.MatchLineupEntry;
import com.guenbon.jochuckhub.entity.MatchVote;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.Position;
import com.guenbon.jochuckhub.entity.TeamRole;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MatchNotFoundException;
import com.guenbon.jochuckhub.repository.MatchLineupEntryRepository;
import com.guenbon.jochuckhub.repository.MatchRepository;
import com.guenbon.jochuckhub.repository.MatchVoteRepository;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchLineupService {

    private static final int QUARTERS = 4;
    private static final int PLAYERS_PER_QUARTER = 10;
    private static final List<Position> FORMATION = List.of(
            Position.LB, Position.CB, Position.CB, Position.RB,
            Position.CDM, Position.CM, Position.CM,
            Position.LW, Position.ST, Position.RW
    );

    private final MatchRepository matchRepository;
    private final MatchVoteRepository matchVoteRepository;
    private final MemberRepository memberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MatchLineupEntryRepository lineupEntryRepository;

    @Transactional
    public MatchLineupResponse generateLineup(Long matchId, Long requesterId) {
        Match match = findExistingMatch(matchId);
        verifyOwnerOrManager(match, requesterId);
        verifyVoteClosed(match);

        List<MatchVote> attendVotes = matchVoteRepository.findAllByMatchIdAndAttendStatus(matchId, AttendStatus.ATTEND);
        int attendeeCount = attendVotes.size();
        if (attendeeCount < 14 || attendeeCount > 20) {
            throw new IllegalArgumentException("자동 라인업은 참석 인원이 14명 이상 20명 이하여야 합니다. 현재 " + attendeeCount + "명");
        }

        lineupEntryRepository.deleteByMatchId(matchId);
        Long homeTeamId = match.getHomeTeam().getId();
        Map<Long, Integer> attendanceScores = findAttendanceScores(attendVotes, homeTeamId, match.getMatchDate());
        List<ScoredMember> scoredMembers = attendVotes.stream()
                .map(vote -> new ScoredMember(vote.getMember(),
                        attendanceScores.getOrDefault(vote.getMember().getId(), 0)))
                .sorted(Comparator.comparingInt(ScoredMember::score).reversed())
                .toList();

        List<List<Integer>> quarterAssignments = assignQuarters(attendeeCount, 40 - 2 * attendeeCount);
        List<List<ScoredMember>> quarterPlayers = new ArrayList<>();
        for (int quarter = 0; quarter < QUARTERS; quarter++) {
            quarterPlayers.add(new ArrayList<>());
        }
        for (int index = 0; index < scoredMembers.size(); index++) {
            for (int quarter : quarterAssignments.get(index)) {
                quarterPlayers.get(quarter).add(scoredMembers.get(index));
            }
        }

        List<MatchLineupEntry> entries = new ArrayList<>();
        for (int quarter = 0; quarter < QUARTERS; quarter++) {
            List<ScoredMember> players = quarterPlayers.get(quarter);
            int[] assignment = solveAssignment(players);
            for (int index = 0; index < players.size(); index++) {
                entries.add(MatchLineupEntry.builder()
                        .match(match)
                        .quarter(quarter + 1)
                        .member(players.get(index).member())
                        .position(FORMATION.get(assignment[index]))
                        .build());
            }
        }
        lineupEntryRepository.saveAll(entries);
        return buildResponse(matchId, entries);
    }

    public MatchLineupResponse getLineup(Long matchId, Long requesterId) {
        Match match = findExistingMatch(matchId);
        verifyHomeTeamMember(match, requesterId);
        List<MatchLineupEntry> entries = lineupEntryRepository.findAllByMatchId(matchId);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("라인업이 아직 생성되지 않았습니다.");
        }
        return buildResponse(matchId, entries);
    }

    @Transactional
    public MatchLineupResponse saveLineup(Long matchId, SaveLineupRequest request, Long requesterId) {
        Match match = findExistingMatch(matchId);
        verifyOwnerOrManager(match, requesterId);
        verifyVoteClosed(match);

        List<Integer> quarters = request.getQuarters().stream()
                .map(SaveLineupRequest.QuarterEntry::getQuarter)
                .sorted()
                .toList();
        if (!quarters.equals(List.of(1, 2, 3, 4))) {
            throw new IllegalArgumentException("1~4쿼터 데이터가 각각 1개씩 있어야 합니다.");
        }

        validateManualLineup(match, request);
        lineupEntryRepository.deleteByMatchId(matchId);
        List<MatchLineupEntry> entries = new ArrayList<>();
        for (SaveLineupRequest.QuarterEntry quarterEntry : request.getQuarters()) {
            for (SaveLineupRequest.PlayerEntry playerEntry : quarterEntry.getPlayers()) {
                Member member = memberRepository.findById(playerEntry.getMemberId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 멤버입니다: " + playerEntry.getMemberId()));
                entries.add(MatchLineupEntry.builder()
                        .match(match)
                        .quarter(quarterEntry.getQuarter())
                        .member(member)
                        .position(playerEntry.getPosition())
                        .build());
            }
        }
        lineupEntryRepository.saveAll(entries);
        return buildResponse(matchId, entries);
    }

    private Map<Long, Integer> findAttendanceScores(List<MatchVote> attendVotes, Long homeTeamId,
                                                     LocalDateTime before) {
        List<Long> memberIds = attendVotes.stream().map(vote -> vote.getMember().getId()).toList();
        Map<Long, Integer> scores = new HashMap<>();
        for (Object[] row : matchVoteRepository.sumRecentAttendanceScoresByMemberIds(memberIds, homeTeamId, before)) {
            scores.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return scores;
    }

    private void validateManualLineup(Match match, SaveLineupRequest request) {
        Long homeTeamId = match.getHomeTeam().getId();
        Set<Long> homeTeamMemberIds = new HashSet<>(teamMemberRepository.findMemberIdsByTeamId(homeTeamId));
        Set<Long> attendMemberIds = new HashSet<>(
                matchVoteRepository.findMemberIdsByMatchIdAndAttendStatus(match.getId(), AttendStatus.ATTEND));
        Map<Position, Integer> expectedPositionCounts = formationPositionCounts();

        for (SaveLineupRequest.QuarterEntry quarterEntry : request.getQuarters()) {
            Set<Long> quarterMemberIds = new HashSet<>();
            Map<Position, Integer> positionCounts = new EnumMap<>(Position.class);
            for (SaveLineupRequest.PlayerEntry playerEntry : quarterEntry.getPlayers()) {
                Long memberId = playerEntry.getMemberId();
                if (!homeTeamMemberIds.contains(memberId)) {
                    throw new IllegalArgumentException("A lineup member must belong to the home team.");
                }
                if (!attendMemberIds.contains(memberId)) {
                    throw new IllegalArgumentException("A lineup member must have an ATTEND vote.");
                }
                if (!quarterMemberIds.add(memberId)) {
                    throw new IllegalArgumentException("A member cannot be assigned twice in the same quarter.");
                }
                positionCounts.merge(playerEntry.getPosition(), 1, Integer::sum);
            }
            if (!expectedPositionCounts.equals(positionCounts)) {
                throw new IllegalArgumentException("Each quarter must use the 4-3-3 formation positions.");
            }
        }
    }

    private Map<Position, Integer> formationPositionCounts() {
        Map<Position, Integer> counts = new EnumMap<>(Position.class);
        for (Position position : FORMATION) {
            counts.merge(position, 1, Integer::sum);
        }
        return counts;
    }

    private List<List<Integer>> assignQuarters(int attendeeCount, int threeQuarterCount) {
        int[] remaining = {PLAYERS_PER_QUARTER, PLAYERS_PER_QUARTER, PLAYERS_PER_QUARTER, PLAYERS_PER_QUARTER};
        List<List<Integer>> assignments = new ArrayList<>();
        for (int index = 0; index < attendeeCount; index++) {
            int playCount = index < threeQuarterCount ? 3 : 2;
            Integer[] order = {0, 1, 2, 3};
            Arrays.sort(order, (left, right) -> remaining[left] != remaining[right]
                    ? remaining[right] - remaining[left]
                    : left - right);
            List<Integer> chosen = new ArrayList<>();
            for (int count = 0; count < playCount; count++) {
                chosen.add(order[count]);
                remaining[order[count]]--;
            }
            assignments.add(chosen);
        }
        return assignments;
    }

    private int[] solveAssignment(List<ScoredMember> players) {
        int playerCount = players.size();
        int[][] cost = new int[playerCount][playerCount];
        for (int player = 0; player < playerCount; player++) {
            Member member = players.get(player).member();
            for (int slot = 0; slot < playerCount; slot++) {
                Position position = FORMATION.get(slot);
                int satisfaction = position == member.getMainPosition() ? 2
                        : member.getSubPositions().contains(position) ? 1 : 0;
                cost[player][slot] = 2 - satisfaction;
            }
        }
        return hungarian(cost);
    }

    private int[] hungarian(int[][] cost) {
        int size = cost.length;
        int[] u = new int[size + 1], v = new int[size + 1], p = new int[size + 1], way = new int[size + 1];
        for (int row = 1; row <= size; row++) {
            p[0] = row;
            int column = 0;
            int[] minimum = new int[size + 1];
            boolean[] used = new boolean[size + 1];
            Arrays.fill(minimum, Integer.MAX_VALUE);
            do {
                used[column] = true;
                int rowIndex = p[column], delta = Integer.MAX_VALUE, nextColumn = -1;
                for (int candidate = 1; candidate <= size; candidate++) {
                    if (!used[candidate]) {
                        int current = cost[rowIndex - 1][candidate - 1] - u[rowIndex] - v[candidate];
                        if (current < minimum[candidate]) {
                            minimum[candidate] = current;
                            way[candidate] = column;
                        }
                        if (minimum[candidate] < delta) {
                            delta = minimum[candidate];
                            nextColumn = candidate;
                        }
                    }
                }
                for (int candidate = 0; candidate <= size; candidate++) {
                    if (used[candidate]) {
                        u[p[candidate]] += delta;
                        v[candidate] -= delta;
                    } else {
                        minimum[candidate] -= delta;
                    }
                }
                column = nextColumn;
            } while (p[column] != 0);
            do {
                int previous = way[column];
                p[column] = p[previous];
                column = previous;
            } while (column != 0);
        }
        int[] assignment = new int[size];
        for (int column = 1; column <= size; column++) {
            if (p[column] != 0) assignment[p[column] - 1] = column - 1;
        }
        return assignment;
    }

    private MatchLineupResponse buildResponse(Long matchId, List<MatchLineupEntry> entries) {
        Map<Integer, List<MatchLineupResponse.PlayerAssignment>> assignments = new TreeMap<>();
        for (MatchLineupEntry entry : entries) {
            Member member = entry.getMember();
            Position position = entry.getPosition();
            String fit = position == member.getMainPosition() ? "MAIN"
                    : member.getSubPositions().contains(position) ? "SUB" : "OTHER";
            assignments.computeIfAbsent(entry.getQuarter(), ignored -> new ArrayList<>())
                    .add(new MatchLineupResponse.PlayerAssignment(member.getId(), member.getName(), position, fit));
        }
        List<MatchLineupResponse.QuarterLineup> quarters = assignments.entrySet().stream()
                .map(entry -> new MatchLineupResponse.QuarterLineup(entry.getKey(), entry.getValue()))
                .toList();
        return new MatchLineupResponse(matchId, quarters);
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));
    }

    private Match findExistingMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(MatchNotFoundException::new);
    }

    private void verifyHomeTeamMember(Match match, Long requesterId) {
        if (!teamMemberRepository.existsByTeamIdAndMemberId(match.getHomeTeam().getId(), requesterId)) {
            throw new ForbiddenException("Only home team members can view the lineup.");
        }
    }

    private void verifyOwnerOrManager(Match match, Long requesterId) {
        if (!teamMemberRepository.existsByTeamIdAndMemberIdAndRoleIn(match.getHomeTeam().getId(), requesterId,
                List.of(TeamRole.OWNER, TeamRole.MANAGER))) {
            throw new ForbiddenException("OWNER 또는 MANAGER만 라인업을 관리할 수 있습니다.");
        }
    }

    private void verifyVoteClosed(Match match) {
        if (LocalDateTime.now().isBefore(match.getEffectiveVoteDeadline())) {
            throw new IllegalArgumentException("투표가 아직 마감되지 않았습니다.");
        }
    }

    private record ScoredMember(Member member, int score) {}
}
