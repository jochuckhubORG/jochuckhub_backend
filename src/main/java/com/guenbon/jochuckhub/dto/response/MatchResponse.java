package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Match;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MatchResponse {

    private final Long id;
    private final TeamInfo homeTeam;
    private final TeamInfo opponentTeam;
    private final LocalDateTime matchDate;
    private final String location;
    private final String createdBy;
    private final LocalDateTime voteDeadline;

    public MatchResponse(Match match) {
        this.id = match.getId();
        this.homeTeam = new TeamInfo(match.getHomeTeam().getId(), match.getHomeTeam().getName(), false);
        this.opponentTeam = new TeamInfo(match.getOpponentTeam().getId(), match.getOpponentTeam().getName(),
                match.getOpponentTeam().isVirtual());
        this.matchDate = match.getMatchDate();
        this.location = match.getLocation();
        this.createdBy = match.getCreatedBy().getName();
        this.voteDeadline = match.getEffectiveVoteDeadline();
    }

    @Getter
    public static class TeamInfo {
        private final Long id;
        private final String name;
        private final boolean virtual;

        public TeamInfo(Long id, String name, boolean virtual) {
            this.id = id;
            this.name = name;
            this.virtual = virtual;
        }
    }
}
