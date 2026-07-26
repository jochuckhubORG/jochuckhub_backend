package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.dto.response.MatchVoteResponse;
import com.guenbon.jochuckhub.entity.AttendStatus;
import com.guenbon.jochuckhub.entity.MatchVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchVoteRepository extends JpaRepository<MatchVote, Long> {

    Optional<MatchVote> findByMatchIdAndMemberId(Long matchId, Long memberId);

    List<MatchVote> findAllByMatchId(Long matchId);

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

    List<MatchVote> findAllByMatchIdAndAttendStatus(Long matchId, AttendStatus attendStatus);
}
