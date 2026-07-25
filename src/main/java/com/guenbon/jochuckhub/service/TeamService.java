package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.CreateTeamRequest;
import com.guenbon.jochuckhub.dto.request.CreateVirtualTeamRequest;
import com.guenbon.jochuckhub.dto.request.UpdateTeamRequest;
import com.guenbon.jochuckhub.dto.response.TeamDetailResponse;
import com.guenbon.jochuckhub.dto.response.TeamSummaryResponse;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.Team;
import com.guenbon.jochuckhub.entity.TeamMember;
import com.guenbon.jochuckhub.entity.TeamRole;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.exception.TeamNotFoundException;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamMemberRepository;
import com.guenbon.jochuckhub.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public TeamDetailResponse createTeam(CreateTeamRequest request, CustomUserDetails requester) {
        Member member = memberRepository.findById(requester.getMemberId())
                .orElseThrow(MemberNotFoundException::new);

        Team team = teamRepository.save(Team.builder()
                .name(request.getName())
                .virtual(false)
                .build());

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .member(member)
                .role(TeamRole.OWNER)
                .build());

        return new TeamDetailResponse(teamRepository.findByIdAndDeletedFalse(team.getId())
                .orElseThrow(TeamNotFoundException::new), TeamRole.OWNER);
    }

    public List<TeamSummaryResponse> getTeams(Long memberId) {
        return teamMemberRepository.findAllByMemberId(memberId).stream()
                .map(TeamMember::getTeam)
                .filter(team -> !team.isVirtual() && !team.isDeleted())
                .map(TeamSummaryResponse::new)
                .toList();
    }

    public List<TeamSummaryResponse> searchTeams(String name, Long myTeamId) {
        List<Team> teams = myTeamId != null
                ? teamRepository.searchByNameForTeam(name, myTeamId)
                : teamRepository.searchRealTeamsByName(name);
        return teams.stream().map(TeamSummaryResponse::new).toList();
    }

    @Transactional
    public void joinTeam(Long teamId, Long memberId) {
        Team team = teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(TeamNotFoundException::new);
        if (team.isVirtual()) {
            throw new IllegalArgumentException("Virtual teams cannot be joined.");
        }
        if (teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId)) {
            throw new IllegalArgumentException("Already a team member.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .member(member)
                .role(TeamRole.PLAYER)
                .build());
    }

    @Transactional
    public TeamSummaryResponse createVirtualTeam(CreateVirtualTeamRequest request, CustomUserDetails requester) {
        Long myTeamId = request.getMyTeamId();
        verifyOwnerOrManager(myTeamId, requester.getMemberId());

        Team virtualTeam = teamRepository.save(Team.builder()
                .name(request.getName())
                .virtual(true)
                .createdByTeamId(myTeamId)
                .build());

        return new TeamSummaryResponse(virtualTeam);
    }

    public TeamDetailResponse getTeam(Long teamId, Long memberId) {
        Team team = teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(TeamNotFoundException::new);
        TeamRole role = teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId)
                .map(TeamMember::getRole)
                .orElse(null);
        return new TeamDetailResponse(team, role);
    }

    @Transactional
    public TeamDetailResponse updateTeam(Long teamId, UpdateTeamRequest request, CustomUserDetails requester) {
        Team team = teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(TeamNotFoundException::new);
        verifyOwner(teamId, requester.getMemberId());

        team.updateName(request.getName());
        return new TeamDetailResponse(team, TeamRole.OWNER);
    }

    @Transactional
    public void deleteTeam(Long teamId, CustomUserDetails requester) {
        Team team = teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(TeamNotFoundException::new);
        verifyOwner(teamId, requester.getMemberId());
        team.deactivate();
    }

    private void verifyOwner(Long teamId, Long memberId) {
        boolean isOwner = teamMemberRepository.existsByTeamIdAndMemberIdAndRoleIn(
                teamId, memberId, List.of(TeamRole.OWNER));
        if (!isOwner) {
            throw new ForbiddenException("Only the team owner can perform this action.");
        }
    }

    public void verifyOwnerOrManager(Long teamId, Long memberId) {
        boolean isOwnerOrManager = teamMemberRepository.existsByTeamIdAndMemberIdAndRoleIn(
                teamId, memberId, List.of(TeamRole.OWNER, TeamRole.MANAGER));
        if (!isOwnerOrManager) {
            throw new ForbiddenException("Only the team owner or manager can perform this action.");
        }
    }
}
