package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.dto.response.MatchVoteResponse;
import com.guenbon.jochuckhub.entity.AttendStatus;
import com.guenbon.jochuckhub.entity.MatchVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchVoteRepository extends JpaRepository<MatchVote, Long> {

    Optional<MatchVote> findByMatchIdAndMemberId(Long matchId, Long memberId);

    List<MatchVote> findAllByMatchId(Long matchId);

    @Query("SELECT v.member.id FROM MatchVote v WHERE v.match.id = :matchId AND v.attendStatus = :attendStatus")
    List<Long> findMemberIdsByMatchIdAndAttendStatus(@Param("matchId") Long matchId,
                                                      @Param("attendStatus") AttendStatus attendStatus);

    @Query("""
            SELECT new com.guenbon.jochuckhub.dto.response.MatchVoteResponse(
                v.id, m.id, member.id, member.name, v.attendStatus, v.actualStatus
            )
            FROM MatchVote v
            JOIN v.match m
            JOIN v.member member
            WHERE m.id = :matchId
            """)
    List<MatchVoteResponse> findResponsesByMatchId(@Param("matchId") Long matchId);

    List<MatchVote> findTop8ByMemberIdAndMatchHomeTeamIdOrderByMatchMatchDateDesc(Long memberId, Long homeTeamId);

    @EntityGraph(attributePaths = "member")
    List<MatchVote> findAllByMatchIdAndAttendStatus(Long matchId, AttendStatus attendStatus);

    @Query(value = """
            SELECT recent.member_id, SUM(recent.score)
            FROM (
                SELECT mv.member_id,
                       CASE
                           WHEN mv.attend_status = 'ABSENT' THEN 0
                           WHEN mv.actual_status IS NULL THEN 2
                           WHEN mv.actual_status = 'LATE' THEN 1
                           WHEN mv.actual_status = 'NO_SHOW' THEN -1
                       END AS score,
                       ROW_NUMBER() OVER (PARTITION BY mv.member_id ORDER BY mr.match_date DESC) AS row_number
                FROM match_vote mv
                JOIN match_record mr ON mr.id = mv.match_id
                WHERE mv.member_id IN (:memberIds)
                  AND mr.home_team_id = :teamId
                  AND mr.match_date < :before
            ) recent
            WHERE recent.row_number <= 8
            GROUP BY recent.member_id
            """, nativeQuery = true)
    List<Object[]> sumRecentAttendanceScoresByMemberIds(@Param("memberIds") List<Long> memberIds,
                                                         @Param("teamId") Long teamId,
                                                         @Param("before") LocalDateTime before);
}
