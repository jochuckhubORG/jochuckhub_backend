package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Position;
import com.guenbon.jochuckhub.entity.TeamRole;
import lombok.Getter;

@Getter
public class TeamMemberStatsProjection {

    private final Long memberId;
    private final String name;
    private final Position mainPosition;
    private final TeamRole role;

    public TeamMemberStatsProjection(Long memberId, String name, Position mainPosition, TeamRole role) {
        this.memberId = memberId;
        this.name = name;
        this.mainPosition = mainPosition;
        this.role = role;
    }
}
