package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.MatchLineupEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchLineupEntryRepository extends JpaRepository<MatchLineupEntry, Long> {
    List<MatchLineupEntry> findAllByMatchId(Long matchId);
    void deleteByMatchId(Long matchId);

    /** 팀 경기 기준 멤버별 출전경기 수 집계 — [memberId, count] */
    @Query("""
            SELECT e.member.id, COUNT(DISTINCT e.match.id)
            FROM MatchLineupEntry e
            WHERE e.match.homeTeam.id = :teamId
            GROUP BY e.member.id
            """)
    List<Object[]> countAppearancesByTeam(@Param("teamId") Long teamId);
}
