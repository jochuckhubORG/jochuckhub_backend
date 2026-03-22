package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.TeamMember;
import com.guenbon.jochuckhub.entity.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    Optional<TeamMember> findByTeamIdAndMemberId(Long teamId, Long memberId);
    boolean existsByTeamIdAndMemberId(Long teamId, Long memberId);
    boolean existsByTeamIdAndMemberIdAndRoleIn(Long teamId, Long memberId, List<TeamRole> roles);
    List<TeamMember> findAllByTeamId(Long teamId);
}
