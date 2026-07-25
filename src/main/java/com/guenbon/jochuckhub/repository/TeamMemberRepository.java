package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.dto.response.TeamMemberStatsProjection;
import com.guenbon.jochuckhub.dto.response.TeamSummaryResponse;
import com.guenbon.jochuckhub.entity.TeamMember;
import com.guenbon.jochuckhub.entity.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamIdAndMemberId(Long teamId, Long memberId);

    boolean existsByTeamIdAndMemberId(Long teamId, Long memberId);

    boolean existsByTeamIdAndMemberIdAndRoleIn(Long teamId, Long memberId, List<TeamRole> roles);

    List<TeamMember> findAllByTeamId(Long teamId);

    List<TeamMember> findAllByMemberId(Long memberId);

    @Query("""
            SELECT new com.guenbon.jochuckhub.dto.response.TeamSummaryResponse(
                t.id, t.name, t.virtual, COUNT(allTeamMembers.id)
            )
            FROM TeamMember myTeamMember
            JOIN myTeamMember.team t
            JOIN t.teamMembers allTeamMembers
            WHERE myTeamMember.member.id = :memberId
            AND t.virtual = false
            AND t.deleted = false
            GROUP BY t.id, t.name, t.virtual
            ORDER BY t.createdAt DESC
            """)
    List<TeamSummaryResponse> findTeamSummariesByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT new com.guenbon.jochuckhub.dto.response.TeamMemberStatsProjection(
                m.id, m.name, m.mainPosition, tm.role
            )
            FROM TeamMember tm
            JOIN tm.member m
            WHERE tm.team.id = :teamId
            ORDER BY m.name ASC
            """)
    List<TeamMemberStatsProjection> findTeamMemberStatsByTeamId(@Param("teamId") Long teamId);
}
