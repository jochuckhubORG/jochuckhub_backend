package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Position;
import com.guenbon.jochuckhub.entity.TeamMember;
import com.guenbon.jochuckhub.entity.TeamRole;
import lombok.Getter;

import java.util.Set;

@Getter
public class TeamMemberStatsResponse {

    private final Long id;
    private final String username;
    private final String name;
    private final Position mainPosition;
    private final Set<Position> subPositions;
    private final TeamRole role;
    private final long goals;
    private final long assists;
    private final long appearances;

    public TeamMemberStatsResponse(TeamMember teamMember, long goals, long assists, long appearances) {
        this.id = teamMember.getMember().getId();
        this.username = teamMember.getMember().getUsername();
        this.name = teamMember.getMember().getName();
        this.mainPosition = teamMember.getMember().getMainPosition();
        this.subPositions = teamMember.getMember().getSubPositions();
        this.role = teamMember.getRole();
        this.goals = goals;
        this.assists = assists;
        this.appearances = appearances;
    }
}
