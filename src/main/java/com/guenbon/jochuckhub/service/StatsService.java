package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.response.GoalRecordResponse;
import com.guenbon.jochuckhub.dto.response.PageResponse;
import com.guenbon.jochuckhub.dto.response.TeamMemberStatsProjection;
import com.guenbon.jochuckhub.dto.response.TeamMemberStatsResponse;
import com.guenbon.jochuckhub.entity.Position;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.exception.TeamNotFoundException;
import com.guenbon.jochuckhub.repository.MatchLineupEntryRepository;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamMemberRepository;
import com.guenbon.jochuckhub.repository.TeamRepository;
import com.guenbon.jochuckhub.repository.goal.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private static final int PAGE_SIZE = 20;

    private final TeamMemberRepository teamMemberRepository;
    private final GoalRepository goalRepository;
    private final MatchLineupEntryRepository matchLineupEntryRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    public List<TeamMemberStatsResponse> getTeamMemberStats(Long teamId, Long requesterId) {
        if (!teamRepository.existsByIdAndDeletedFalse(teamId)) {
            throw new TeamNotFoundException();
        }
        if (!teamMemberRepository.existsByTeamIdAndMemberId(teamId, requesterId)) {
            throw new ForbiddenException("Only team members can view member statistics.");
        }

        List<TeamMemberStatsProjection> members = teamMemberRepository.findTeamMemberStatsByTeamId(teamId);
        List<Long> memberIds = members.stream().map(TeamMemberStatsProjection::getMemberId).toList();
        Map<Long, Set<Position>> subPositionMap = getSubPositionMap(memberIds);
        Map<Long, Long> goalMap = toMap(goalRepository.countGoalsByTeam(teamId));
        Map<Long, Long> assistMap = toMap(goalRepository.countAssistsByTeam(teamId));
        Map<Long, Long> appearanceMap = toMap(matchLineupEntryRepository.countAppearancesByTeam(teamId));

        return members.stream()
                .map(member -> new TeamMemberStatsResponse(
                        member,
                        subPositionMap.getOrDefault(member.getMemberId(), Set.of()),
                        goalMap.getOrDefault(member.getMemberId(), 0L),
                        assistMap.getOrDefault(member.getMemberId(), 0L),
                        appearanceMap.getOrDefault(member.getMemberId(), 0L)
                ))
                .toList();
    }

    public PageResponse<GoalRecordResponse> getGoalRecords(
            Long memberId, Long teamId,
            String type, String sortDirection,
            Long opponentTeamId,
            LocalDate startDate, LocalDate endDate,
            Long relatedMemberId, Long requesterId, int page) {

        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }
        if (!teamRepository.existsByIdAndDeletedFalse(teamId)) {
            throw new TeamNotFoundException();
        }
        if (!teamMemberRepository.existsByTeamIdAndMemberId(teamId, requesterId)) {
            throw new ForbiddenException("Only team members can view goal records.");
        }
        if (type != null && !"GOAL".equalsIgnoreCase(type) && !"ASSIST".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("type must be GOAL or ASSIST.");
        }
        if (!"ASC".equalsIgnoreCase(sortDirection) && !"DESC".equalsIgnoreCase(sortDirection)) {
            throw new IllegalArgumentException("sortDirection must be ASC or DESC.");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate.");
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        return PageResponse.from(goalRepository.findGoalRecords(
                        teamId, memberId, type == null ? null : type.toUpperCase(), sortDirection.toUpperCase(),
                        opponentTeamId, startDateTime, endDateTime, relatedMemberId, PageRequest.of(page, PAGE_SIZE)),
                goal -> new GoalRecordResponse(goal, memberId));
    }

    private Map<Long, Set<Position>> getSubPositionMap(List<Long> memberIds) {
        Map<Long, Set<Position>> result = new HashMap<>();
        if (memberIds.isEmpty()) {
            return result;
        }
        for (Object[] row : memberRepository.findSubPositionsByMemberIds(memberIds)) {
            Long memberId = (Long) row[0];
            Position position = (Position) row[1];
            if (position != null) {
                result.computeIfAbsent(memberId, ignored -> new HashSet<>()).add(position);
            }
        }
        return result;
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
