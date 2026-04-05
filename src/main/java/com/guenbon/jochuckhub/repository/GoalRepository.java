package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findAllByMatchId(Long matchId);

    void deleteAllByMatchId(Long matchId);

    /**
     * 선수 개인 기록 조회 (골+어시스트 통합). 결과는 matchDate DESC.
     * null 파라미터는 해당 조건 미적용.
     * relatedMemberId: scorer 또는 assister가 해당 멤버인 기록만 반환
     */
    @Query("""
            SELECT g FROM Goal g
            JOIN FETCH g.match m
            JOIN FETCH m.opponentTeam
            LEFT JOIN FETCH g.scorer
            LEFT JOIN FETCH g.assister
            WHERE m.homeTeam.id = :teamId
            AND g.opponentGoal = false
            AND (g.scorer.id = :memberId OR g.assister.id = :memberId)
            AND (:opponentTeamId IS NULL OR m.opponentTeam.id = :opponentTeamId)
            AND (:startDate IS NULL OR m.matchDate >= :startDate)
            AND (:endDate IS NULL OR m.matchDate <= :endDate)
            AND (:relatedMemberId IS NULL OR g.scorer.id = :relatedMemberId OR g.assister.id = :relatedMemberId)
            ORDER BY m.matchDate DESC
            """)
    List<Goal> findGoalRecordsByMemberAndFilters(
            @Param("teamId") Long teamId,
            @Param("memberId") Long memberId,
            @Param("opponentTeamId") Long opponentTeamId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("relatedMemberId") Long relatedMemberId
    );

    /** 팀 경기 기준 멤버별 골 집계 — [memberId, count] */
    @Query("""
            SELECT g.scorer.id, COUNT(g)
            FROM Goal g JOIN g.match m
            WHERE m.homeTeam.id = :teamId
            AND g.opponentGoal = false
            AND g.scorer IS NOT NULL
            GROUP BY g.scorer.id
            """)
    List<Object[]> countGoalsByTeam(@Param("teamId") Long teamId);

    /** 팀 경기 기준 멤버별 어시스트 집계 — [memberId, count] */
    @Query("""
            SELECT g.assister.id, COUNT(g)
            FROM Goal g JOIN g.match m
            WHERE m.homeTeam.id = :teamId
            AND g.opponentGoal = false
            AND g.assister IS NOT NULL
            GROUP BY g.assister.id
            """)
    List<Object[]> countAssistsByTeam(@Param("teamId") Long teamId);
}
