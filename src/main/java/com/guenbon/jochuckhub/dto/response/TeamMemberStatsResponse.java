package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Position;
import com.guenbon.jochuckhub.entity.TeamRole;
import lombok.Getter;

import java.util.Set;

@Getter
public class TeamMemberStatsResponse {

    private final Long id;
    private final String name;
    private final Position mainPosition;
    private final Set<Position> subPositions;
    private final TeamRole role;
    private final long goals;
    private final long assists;
    private final long appearances;

    public TeamMemberStatsResponse(TeamMemberStatsProjection projection, Set<Position> subPositions,
                                   long goals, long assists, long appearances) {
        this.id = projection.getMemberId();
        this.name = projection.getName();
        this.mainPosition = projection.getMainPosition();
        this.subPositions = Set.copyOf(subPositions);
        this.role = projection.getRole();
        this.goals = goals;
        this.assists = assists;
        this.appearances = appearances;
    }
}
