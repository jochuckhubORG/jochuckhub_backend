package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Team;
import com.guenbon.jochuckhub.entity.TeamMember;
import com.guenbon.jochuckhub.entity.TeamRole;
import lombok.Getter;

import java.util.List;

@Getter
public class TeamDetailResponse {

    private final Long id;
    private final String name;
    private final boolean virtual;
    private final MemberResponse owner;
    private final List<MemberResponse> managers;
    private final int memberCount;
    private final TeamRole currentUserRole; // null이면 팀 미소속

    public TeamDetailResponse(Team team, TeamRole currentUserRole) {
        this.id = team.getId();
        this.name = team.getName();
        this.virtual = team.isVirtual();
        this.memberCount = team.getTeamMembers().size();
        this.currentUserRole = currentUserRole;

        this.owner = team.getTeamMembers().stream()
                .filter(tm -> tm.getRole() == TeamRole.OWNER)
                .map(TeamMember::getMember)
                .map(MemberResponse::new)
                .findFirst()
                .orElse(null);

        this.managers = team.getTeamMembers().stream()
                .filter(tm -> tm.getRole() == TeamRole.MANAGER)
                .map(TeamMember::getMember)
                .map(MemberResponse::new)
                .toList();
    }
}
