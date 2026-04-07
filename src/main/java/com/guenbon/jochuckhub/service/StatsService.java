package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.response.GoalRecordResponse;
import com.guenbon.jochuckhub.dto.response.TeamMemberStatsResponse;
import com.guenbon.jochuckhub.entity.TeamMember;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.exception.TeamNotFoundException;
import com.guenbon.jochuckhub.repository.goal.GoalRepository;
import com.guenbon.jochuckhub.repository.MatchLineupEntryRepository;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamMemberRepository;
import com.guenbon.jochuckhub.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final TeamMemberRepository teamMemberRepository;
    private final GoalRepository goalRepository;
    private final MatchLineupEntryRepository matchLineupEntryRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    /**
     * 팀원 목록 + 팀 기준 개인 통계 (골/어시스트/출전경기)
     */
    public List<TeamMemberStatsResponse> getTeamMemberStats(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException();
        }

        List<TeamMember> teamMembers = teamMemberRepository.findAllByTeamId(teamId);

        Map<Long, Long> goalMap = toMap(goalRepository.countGoalsByTeam(teamId));
        Map<Long, Long> assistMap = toMap(goalRepository.countAssistsByTeam(teamId));
        Map<Long, Long> appearanceMap = toMap(matchLineupEntryRepository.countAppearancesByTeam(teamId));

        return teamMembers.stream()
                .map(tm -> new TeamMemberStatsResponse(
                        tm,
                        goalMap.getOrDefault(tm.getMember().getId(), 0L),
                        assistMap.getOrDefault(tm.getMember().getId(), 0L),
                        appearanceMap.getOrDefault(tm.getMember().getId(), 0L)
                ))
                .toList();
    }

    /**
     * 선수 개인 기록 조회 (골/어시스트, 다중 필터)
     *
     * @param memberId        조회할 선수 ID
     * @param teamId          소속 팀 ID (homeTeam 기준)
     * @param type            null=전체, "GOAL"=골만, "ASSIST"=어시스트만
     * @param sortDirection   "DESC"(기본, 최신순) 또는 "ASC"(오래된순)
     * @param opponentTeamId  특정 상대팀 ID
     * @param startDate       날짜 범위 시작 (포함)
     * @param endDate         날짜 범위 종료 (포함)
     * @param relatedMemberId 해당 멤버와 함께한 기록만 (골→어시스터, 어시스트→득점자)
     */
    public List<GoalRecordResponse> getGoalRecords(
            Long memberId, Long teamId,
            String type, String sortDirection,
            Long opponentTeamId,
            LocalDate startDate, LocalDate endDate,
            Long relatedMemberId) {

        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException();
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        return goalRepository.findGoalRecords(
                        teamId, memberId, type, sortDirection, opponentTeamId, startDateTime, endDateTime, relatedMemberId)
                .stream()
                .map(g -> new GoalRecordResponse(g, memberId))
                .toList();
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
