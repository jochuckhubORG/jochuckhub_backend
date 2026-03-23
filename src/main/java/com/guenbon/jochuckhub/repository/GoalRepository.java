package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findAllByMatchId(Long matchId);

    void deleteAllByMatchId(Long matchId);
}
