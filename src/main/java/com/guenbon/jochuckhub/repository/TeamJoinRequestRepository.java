package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.JoinRequestStatus;
import com.guenbon.jochuckhub.entity.TeamJoinRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {

    Optional<TeamJoinRequest> findByTeamIdAndMemberId(Long teamId, Long memberId);

    Optional<TeamJoinRequest> findByIdAndTeamId(Long id, Long teamId);

    @EntityGraph(attributePaths = "member")
    List<TeamJoinRequest> findAllByTeamIdAndStatusOrderByCreatedAtAsc(Long teamId, JoinRequestStatus status);
}
