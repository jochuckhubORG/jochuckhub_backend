package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.dto.response.MatchResponse;
import com.guenbon.jochuckhub.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("""
            SELECT new com.guenbon.jochuckhub.dto.response.MatchResponse(
                m.id,
                home.id, home.name,
                opponent.id, opponent.name, opponent.virtual,
                m.matchDate, m.durationMinutes, m.location,
                creator.name, m.voteDeadline, m.version
            )
            FROM Match m
            JOIN m.homeTeam home
            JOIN m.opponentTeam opponent
            JOIN m.createdBy creator
            WHERE home.id = :teamId OR opponent.id = :teamId
            ORDER BY m.matchDate DESC
            """)
    List<MatchResponse> findResponsesByTeamId(@Param("teamId") Long teamId);

    @Query("""
            SELECT m FROM Match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.opponentTeam
            JOIN FETCH m.createdBy
            WHERE m.id = :matchId
            """)
    Optional<Match> findByIdWithDetails(@Param("matchId") Long matchId);
}
