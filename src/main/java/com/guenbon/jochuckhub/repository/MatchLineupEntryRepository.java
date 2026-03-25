package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.MatchLineupEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchLineupEntryRepository extends JpaRepository<MatchLineupEntry, Long> {
    List<MatchLineupEntry> findAllByMatchId(Long matchId);
    void deleteByMatchId(Long matchId);
}
