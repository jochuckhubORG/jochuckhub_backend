package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.CreateTeamRequest;
import com.guenbon.jochuckhub.dto.request.CreateVirtualTeamRequest;
import com.guenbon.jochuckhub.dto.request.ProcessJoinRequest;
import com.guenbon.jochuckhub.dto.request.UpdateTeamRequest;
import com.guenbon.jochuckhub.dto.response.TeamDetailResponse;
import com.guenbon.jochuckhub.dto.response.TeamJoinRequestResponse;
import com.guenbon.jochuckhub.dto.response.TeamSummaryResponse;
import com.guenbon.jochuckhub.entity.JoinRequestStatus;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.Team;
import com.guenbon.jochuckhub.entity.TeamJoinRequest;
import com.guenbon.jochuckhub.entity.TeamMember;
import com.guenbon.jochuckhub.entity.TeamRole;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.exception.TeamNotFoundException;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamJoinRequestRepository;
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
    private final TeamJoinRequestRepository teamJoinRequestRepository;
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
        return teamMemberRepository.findTeamSummariesByMemberId(memberId);
    }

    public List<TeamSummaryResponse> searchTeams(String name, Long myTeamId, Long requesterId) {
        if (myTeamId != null) {
            findActiveTeam(myTeamId);
            if (!teamMemberRepository.existsByTeamIdAndMemberId(myTeamId, requesterId)) {
                throw new ForbiddenException("Only team members can search that team's virtual teams.");
            }
        }
        List<Team> teams = myTeamId != null
                ? teamRepository.searchByNameForTeam(name, myTeamId)
                : teamRepository.searchRealTeamsByName(name);
        return teams.stream().map(TeamSummaryResponse::new).toList();
    }

    @Transactional
    public TeamJoinRequestResponse requestToJoinTeam(Long teamId, Long memberId) {
        Team team = findActiveTeam(teamId);
        if (team.isVirtual()) {
            throw new IllegalArgumentException("Virtual teams cannot be joined.");
        }
        if (teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId)) {
            throw new IllegalArgumentException("Already a team member.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        TeamJoinRequest joinRequest = teamJoinRequestRepository.findByTeamIdAndMemberId(teamId, memberId)
                .map(existing -> {
                    existing.resubmit();
                    return existing;
                })
                .orElseGet(() -> teamJoinRequestRepository.save(TeamJoinRequest.builder()
                        .team(team)
                        .member(member)
                        .status(JoinRequestStatus.PENDING)
                        .build()));
        return new TeamJoinRequestResponse(joinRequest);
    }

    public List<TeamJoinRequestResponse> getPendingJoinRequests(Long teamId, Long requesterId) {
        findActiveTeam(teamId);
        verifyOwnerOrManager(teamId, requesterId);
        return teamJoinRequestRepository.findAllByTeamIdAndStatusOrderByCreatedAtAsc(teamId, JoinRequestStatus.PENDING)
                .stream()
                .map(TeamJoinRequestResponse::new)
                .toList();
    }

    @Transactional
    public TeamJoinRequestResponse processJoinRequest(Long teamId, Long requestId,
                                                       ProcessJoinRequest request, Long requesterId) {
        findActiveTeam(teamId);
        verifyOwnerOrManager(teamId, requesterId);
        TeamJoinRequest joinRequest = teamJoinRequestRepository.findByIdAndTeamId(requestId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Join request not found."));

        if (request.getApproved()) {
            if (teamMemberRepository.existsByTeamIdAndMemberId(teamId, joinRequest.getMember().getId())) {
                throw new IllegalArgumentException("The applicant is already a team member.");
            }
            joinRequest.approve();
            teamMemberRepository.save(TeamMember.builder()
                    .team(joinRequest.getTeam())
                    .member(joinRequest.getMember())
                    .role(TeamRole.PLAYER)
                    .build());
        } else {
            joinRequest.reject();
        }
        return new TeamJoinRequestResponse(joinRequest);
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
        Team team = findActiveTeam(teamId);
        TeamRole role = teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId)
                .map(TeamMember::getRole)
                .orElse(null);
        return new TeamDetailResponse(team, role);
    }

    @Transactional
    public TeamDetailResponse updateTeam(Long teamId, UpdateTeamRequest request, CustomUserDetails requester) {
        Team team = findActiveTeam(teamId);
        verifyOwner(teamId, requester.getMemberId());
        team.updateName(request.getName());
        return new TeamDetailResponse(team, TeamRole.OWNER);
    }

    @Transactional
    public void deleteTeam(Long teamId, CustomUserDetails requester) {
        Team team = findActiveTeam(teamId);
        verifyOwner(teamId, requester.getMemberId());
        team.deactivate();
    }

    private Team findActiveTeam(Long teamId) {
        return teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(TeamNotFoundException::new);
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
