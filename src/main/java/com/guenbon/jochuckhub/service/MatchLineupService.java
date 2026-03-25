package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.request.SaveLineupRequest;
import com.guenbon.jochuckhub.dto.response.MatchLineupResponse;
import com.guenbon.jochuckhub.entity.*;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.repository.MatchLineupEntryRepository;
import com.guenbon.jochuckhub.repository.MatchRepository;
import com.guenbon.jochuckhub.repository.MatchVoteRepository;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchLineupService {

    private static final int QUARTERS = 4;
    private static final int PLAYERS_PER_QUARTER = 10;

    // 4-3-3 포메이션: LB CB CB RB / CDM CM CM / LW ST RW
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
        Match match = findMatch(matchId);

        // OWNER/MANAGER 권한 확인
        if (!teamMemberRepository.existsByTeamIdAndMemberIdAndRoleIn(
                match.getHomeTeam().getId(), requesterId,
                List.of(TeamRole.OWNER, TeamRole.MANAGER))) {
            throw new ForbiddenException("OWNER 또는 MANAGER만 라인업을 생성할 수 있습니다.");
        }

        // 투표 마감 확인
        if (LocalDateTime.now().isBefore(match.getEffectiveVoteDeadline())) {
            throw new IllegalArgumentException("투표가 아직 마감되지 않았습니다.");
        }

        // 참석 투표 인원 조회
        List<MatchVote> attendVotes = matchVoteRepository.findAllByMatchIdAndAttendStatus(matchId, AttendStatus.ATTEND);
        int n = attendVotes.size();

        // 인원 유효 범위: 14 ≤ N ≤ 20 (3x + 2y = 40, x + y = N)
        if (n < 14) {
            throw new IllegalArgumentException(
                    "인원이 너무 적어서 자동구성할 수 없습니다. (최소 14명 필요, 현재 " + n + "명)");
        }
        if (n > 20) {
            throw new IllegalArgumentException(
                    "인원이 너무 많아서 자동구성할 수 없습니다. (최대 20명 가능, 현재 " + n + "명)");
        }

        // 기존 라인업 삭제 후 재생성
        lineupEntryRepository.deleteByMatchId(matchId);

        // Phase 1: 출석율 점수 기준 내림차순 정렬
        Long homeTeamId = match.getHomeTeam().getId();
        List<ScoredMember> scoredMembers = attendVotes.stream()
                .map(vote -> {
                    Member member = vote.getMember();
                    int score = matchVoteRepository
                            .findTop8ByMemberIdAndMatchHomeTeamIdOrderByMatchMatchDateDesc(member.getId(), homeTeamId)
                            .stream().mapToInt(MatchVote::getScore).sum();
                    return new ScoredMember(member, score);
                })
                .sorted(Comparator.comparingInt(ScoredMember::score).reversed())
                .toList();

        // Phase 1: 쿼터 배정 (상위 threeQuarterCount명이 3쿼터, 나머지가 2쿼터)
        int threeQuarterCount = 40 - 2 * n; // x = 40 - 2N
        List<List<Integer>> quarterAssignments = assignQuarters(n, threeQuarterCount);

        // 쿼터별 플레이어 목록 구성
        List<List<ScoredMember>> quarterPlayers = new ArrayList<>();
        for (int q = 0; q < QUARTERS; q++) quarterPlayers.add(new ArrayList<>());
        for (int i = 0; i < scoredMembers.size(); i++) {
            for (int q : quarterAssignments.get(i)) {
                quarterPlayers.get(q).add(scoredMembers.get(i));
            }
        }

        // Phase 2: 각 쿼터별 헝가리안 알고리즘으로 포지션 배정
        List<MatchLineupEntry> entries = new ArrayList<>();
        for (int q = 0; q < QUARTERS; q++) {
            List<ScoredMember> players = quarterPlayers.get(q);
            int[] assignment = solveAssignment(players);
            for (int i = 0; i < players.size(); i++) {
                entries.add(MatchLineupEntry.builder()
                        .match(match)
                        .quarter(q + 1)
                        .member(players.get(i).member())
                        .position(FORMATION.get(assignment[i]))
                        .build());
            }
        }

        lineupEntryRepository.saveAll(entries);
        return buildResponse(matchId, entries);
    }

    public MatchLineupResponse getLineup(Long matchId) {
        findMatch(matchId);
        List<MatchLineupEntry> entries = lineupEntryRepository.findAllByMatchId(matchId);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("라인업이 아직 생성되지 않았습니다.");
        }
        return buildResponse(matchId, entries);
    }

    @Transactional
    public MatchLineupResponse saveLineup(Long matchId, SaveLineupRequest request, Long requesterId) {
        Match match = findMatch(matchId);

        // OWNER/MANAGER 권한 확인
        if (!teamMemberRepository.existsByTeamIdAndMemberIdAndRoleIn(
                match.getHomeTeam().getId(), requesterId,
                List.of(TeamRole.OWNER, TeamRole.MANAGER))) {
            throw new ForbiddenException("OWNER 또는 MANAGER만 라인업을 저장할 수 있습니다.");
        }

        // 투표 마감 확인
        if (LocalDateTime.now().isBefore(match.getEffectiveVoteDeadline())) {
            throw new IllegalArgumentException("투표가 아직 마감되지 않았습니다.");
        }

        // 쿼터 번호 유효성 확인 (1~4 각각 1개씩)
        List<Integer> quarterNumbers = request.getQuarters().stream()
                .map(SaveLineupRequest.QuarterEntry::getQuarter)
                .sorted()
                .toList();
        if (!quarterNumbers.equals(List.of(1, 2, 3, 4))) {
            throw new IllegalArgumentException("1~4쿼터 데이터가 각각 1개씩 있어야 합니다.");
        }

        // 기존 라인업 삭제 후 저장
        lineupEntryRepository.deleteByMatchId(matchId);

        List<MatchLineupEntry> entries = new ArrayList<>();
        for (SaveLineupRequest.QuarterEntry quarterEntry : request.getQuarters()) {
            for (SaveLineupRequest.PlayerEntry playerEntry : quarterEntry.getPlayers()) {
                Member member = memberRepository.findById(playerEntry.getMemberId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "존재하지 않는 멤버입니다: " + playerEntry.getMemberId()));
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

    /**
     * 각 플레이어에게 뛸 쿼터를 배정한다.
     * 남은 슬롯이 많은 쿼터를 우선 배정하여 쿼터 간 균형을 맞춘다.
     */
    private List<List<Integer>> assignQuarters(int n, int threeQuarterCount) {
        int[] remaining = {PLAYERS_PER_QUARTER, PLAYERS_PER_QUARTER, PLAYERS_PER_QUARTER, PLAYERS_PER_QUARTER};
        List<List<Integer>> assignments = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int playCount = (i < threeQuarterCount) ? 3 : 2;
            Integer[] quarterOrder = {0, 1, 2, 3};
            // 남은 슬롯 많은 쿼터 우선, 동점이면 인덱스 오름차순
            Arrays.sort(quarterOrder, (a, b) ->
                    remaining[a] != remaining[b] ? remaining[b] - remaining[a] : a - b);

            List<Integer> chosen = new ArrayList<>();
            for (int j = 0; j < playCount; j++) {
                chosen.add(quarterOrder[j]);
                remaining[quarterOrder[j]]--;
            }
            assignments.add(chosen);
        }
        return assignments;
    }

    /**
     * 10명 × 10포지션 슬롯에 대해 헝가리안 알고리즘으로 최대 만족도 배정을 수행한다.
     * 만족도: 주 포지션=2, 부 포지션=1, 해당 없음=0
     */
    private int[] solveAssignment(List<ScoredMember> players) {
        int n = players.size();
        int[][] cost = new int[n][n];
        for (int i = 0; i < n; i++) {
            Member member = players.get(i).member();
            for (int j = 0; j < n; j++) {
                Position pos = FORMATION.get(j);
                int satisfaction;
                if (pos == member.getMainPosition()) {
                    satisfaction = 2;
                } else if (member.getSubPositions().contains(pos)) {
                    satisfaction = 1;
                } else {
                    satisfaction = 0;
                }
                cost[i][j] = 2 - satisfaction; // 최소 비용으로 변환 (만족도 최대화 → 비용 최소화)
            }
        }
        return hungarian(cost);
    }

    /**
     * 헝가리안 알고리즘 O(n³) - 최소 비용 완전 이분 매칭
     */
    private int[] hungarian(int[][] cost) {
        int n = cost.length;
        int[] u = new int[n + 1], v = new int[n + 1], p = new int[n + 1], way = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            int[] minVal = new int[n + 1];
            boolean[] used = new boolean[n + 1];
            Arrays.fill(minVal, Integer.MAX_VALUE);

            do {
                used[j0] = true;
                int i0 = p[j0], delta = Integer.MAX_VALUE, j1 = -1;
                for (int j = 1; j <= n; j++) {
                    if (!used[j]) {
                        int cur = cost[i0 - 1][j - 1] - u[i0] - v[j];
                        if (cur < minVal[j]) {
                            minVal[j] = cur;
                            way[j] = j0;
                        }
                        if (minVal[j] < delta) {
                            delta = minVal[j];
                            j1 = j;
                        }
                    }
                }
                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minVal[j] -= delta;
                    }
                }
                j0 = j1;
            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] assignment = new int[n];
        for (int j = 1; j <= n; j++) {
            if (p[j] != 0) assignment[p[j] - 1] = j - 1;
        }
        return assignment;
    }

    private MatchLineupResponse buildResponse(Long matchId, List<MatchLineupEntry> entries) {
        Map<Integer, List<MatchLineupResponse.PlayerAssignment>> map = new TreeMap<>();
        for (MatchLineupEntry entry : entries) {
            Member member = entry.getMember();
            Position pos = entry.getPosition();
            String fit;
            if (pos == member.getMainPosition()) fit = "MAIN";
            else if (member.getSubPositions().contains(pos)) fit = "SUB";
            else fit = "OTHER";

            map.computeIfAbsent(entry.getQuarter(), k -> new ArrayList<>())
                    .add(new MatchLineupResponse.PlayerAssignment(
                            member.getId(), member.getName(), pos, fit));
        }

        List<MatchLineupResponse.QuarterLineup> quarters = map.entrySet().stream()
                .map(e -> new MatchLineupResponse.QuarterLineup(e.getKey(), e.getValue()))
                .toList();

        return new MatchLineupResponse(matchId, quarters);
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));
    }

    private record ScoredMember(Member member, int score) {}
}
