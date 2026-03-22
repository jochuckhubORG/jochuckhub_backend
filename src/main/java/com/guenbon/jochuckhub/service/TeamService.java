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
        if (teamRepository.existsByNameAndVirtualFalse(request.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 팀 이름입니다.");
        }

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

        // 저장 후 연관관계 반영을 위해 다시 조회
        return new TeamDetailResponse(teamRepository.findById(team.getId())
                .orElseThrow(TeamNotFoundException::new));
    }

    public List<TeamSummaryResponse> getTeams() {
        return teamRepository.findAll().stream()
                .map(TeamSummaryResponse::new)
                .toList();
    }

    /**
     * 팀 이름으로 검색: 실제 팀 전체 + myTeamId가 만든 가상 팀
     */
    public List<TeamSummaryResponse> searchTeams(String name, Long myTeamId) {
        return teamRepository.searchByNameForTeam(name, myTeamId).stream()
                .map(TeamSummaryResponse::new)
                .toList();
    }

    @Transactional
    public TeamSummaryResponse createVirtualTeam(CreateVirtualTeamRequest request, CustomUserDetails requester) {
        Long myTeamId = request.getMyTeamId();

        verifyOwnerOrManager(myTeamId, requester.getMemberId());

        if (teamRepository.existsByNameAndVirtualFalse(request.getName())) {
            throw new IllegalArgumentException("이미 실제 팀으로 등록된 이름입니다. 해당 팀을 상대로 선택하세요.");
        }
        if (teamRepository.existsByNameAndVirtualTrueAndCreatedByTeamId(request.getName(), myTeamId)) {
            throw new IllegalArgumentException("이미 등록한 가상 팀 이름입니다.");
        }

        Team virtualTeam = teamRepository.save(Team.builder()
                .name(request.getName())
                .virtual(true)
                .createdByTeamId(myTeamId)
                .build());

        return new TeamSummaryResponse(virtualTeam);
    }

    public TeamDetailResponse getTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(TeamNotFoundException::new);
        return new TeamDetailResponse(team);
    }

    @Transactional
    public TeamDetailResponse updateTeam(Long teamId, UpdateTeamRequest request, CustomUserDetails requester) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(TeamNotFoundException::new);

        verifyOwner(teamId, requester.getMemberId());

        if (!team.getName().equals(request.getName()) && teamRepository.existsByNameAndVirtualFalse(request.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 팀 이름입니다.");
        }

        team.updateName(request.getName());
        return new TeamDetailResponse(team);
    }

    @Transactional
    public void deleteTeam(Long teamId, CustomUserDetails requester) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException();
        }

        verifyOwner(teamId, requester.getMemberId());

        teamRepository.deleteById(teamId);
    }

    private void verifyOwner(Long teamId, Long memberId) {
        boolean isOwner = teamMemberRepository.existsByTeamIdAndMemberIdAndRoleIn(
                teamId, memberId, List.of(TeamRole.OWNER));
        if (!isOwner) {
            throw new ForbiddenException("팀 Owner만 수행할 수 있는 작업입니다.");
        }
    }

    public void verifyOwnerOrManager(Long teamId, Long memberId) {
        boolean isOwnerOrManager = teamMemberRepository.existsByTeamIdAndMemberIdAndRoleIn(
                teamId, memberId, List.of(TeamRole.OWNER, TeamRole.MANAGER));
        if (!isOwnerOrManager) {
            throw new ForbiddenException("팀 Owner 또는 Manager만 수행할 수 있는 작업입니다.");
        }
    }
}
