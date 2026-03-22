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
    private final MemberResponse owner;
    private final List<MemberResponse> managers;
    private final int memberCount;

    public TeamDetailResponse(Team team) {
        this.id = team.getId();
        this.name = team.getName();
        this.memberCount = team.getTeamMembers().size();

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
