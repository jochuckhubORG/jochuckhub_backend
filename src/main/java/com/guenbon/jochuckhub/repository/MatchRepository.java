package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("SELECT m FROM Match m WHERE m.homeTeam.id = :teamId OR m.opponentTeam.id = :teamId")
    List<Match> findAllByTeamId(@Param("teamId") Long teamId);
}
