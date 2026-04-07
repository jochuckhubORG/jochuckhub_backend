package com.guenbon.jochuckhub.repository.goal;

import com.guenbon.jochuckhub.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long>, QueryDslGoalRepository {

    List<Goal> findAllByMatchId(Long matchId);

    void deleteAllByMatchId(Long matchId);

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
