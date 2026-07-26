package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Match;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MatchResponse {

    private final Long id;
    private final Long version;
    private final TeamInfo homeTeam;
    private final TeamInfo opponentTeam;
    private final LocalDateTime matchDate;
    private final int durationMinutes;
    private final LocalDateTime matchEndTime;
    private final String location;
    private final String createdBy;
    private final LocalDateTime voteDeadline;

    public MatchResponse(Match match) {
        this.id = match.getId();
        this.version = match.getVersion();
        this.homeTeam = new TeamInfo(match.getHomeTeam().getId(), match.getHomeTeam().getName(), false);
        this.opponentTeam = new TeamInfo(match.getOpponentTeam().getId(), match.getOpponentTeam().getName(),
                match.getOpponentTeam().isVirtual());
        this.matchDate = match.getMatchDate();
        this.durationMinutes = match.getDurationMinutes();
        this.matchEndTime = match.getMatchEndTime();
        this.location = match.getLocation();
        this.createdBy = match.getCreatedBy().getName();
        this.voteDeadline = match.getEffectiveVoteDeadline();
    }

    public MatchResponse(Long id,
                         Long homeTeamId, String homeTeamName,
                         Long opponentTeamId, String opponentTeamName, boolean opponentTeamVirtual,
                         LocalDateTime matchDate, int durationMinutes, String location,
                         String createdBy, LocalDateTime voteDeadline, Long version) {
        this.id = id;
        this.version = version;
        this.homeTeam = new TeamInfo(homeTeamId, homeTeamName, false);
        this.opponentTeam = new TeamInfo(opponentTeamId, opponentTeamName, opponentTeamVirtual);
        this.matchDate = matchDate;
        this.durationMinutes = durationMinutes;
        this.matchEndTime = matchDate.plusMinutes(durationMinutes);
        this.location = location;
        this.createdBy = createdBy;
        this.voteDeadline = voteDeadline == null || voteDeadline.isAfter(matchDate.minusHours(1))
                ? matchDate.minusHours(1)
                : voteDeadline;
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
