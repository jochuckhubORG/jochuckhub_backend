package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Team;
import lombok.Getter;

@Getter
public class TeamSummaryResponse {

    private final Long id;
    private final String name;
    private final int memberCount;

    public TeamSummaryResponse(Team team) {
        this.id = team.getId();
        this.name = team.getName();
        this.memberCount = team.getTeamMembers().size();
    }
}
