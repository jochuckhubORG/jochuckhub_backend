package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByNameAndVirtualFalse(String name);

    boolean existsByNameAndVirtualTrueAndCreatedByTeamId(String name, Long createdByTeamId);

    /**
     * 팀 이름으로 검색: 실제 팀 전체 + 특정 팀이 만든 가상 팀
     */
    @Query("SELECT t FROM Team t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "AND (t.virtual = false OR (t.virtual = true AND t.createdByTeamId = :myTeamId))")
    List<Team> searchByNameForTeam(@Param("name") String name, @Param("myTeamId") Long myTeamId);
}
