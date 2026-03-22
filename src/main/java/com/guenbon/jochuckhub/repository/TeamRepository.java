package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByName(String name);
}
