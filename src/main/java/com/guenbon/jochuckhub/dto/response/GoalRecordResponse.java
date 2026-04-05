package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Goal;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class GoalRecordResponse {

    private final Long matchId;
    private final LocalDateTime matchDate;
    private final Long opponentTeamId;
    private final String opponentTeamName;
    private final String type; // "GOAL" or "ASSIST"
    private final Long relatedMemberId;   // 골이면 어시스터, 어시스트면 골 넣은 선수
    private final String relatedMemberName;

    public GoalRecordResponse(Goal goal, Long requestedMemberId) {
        this.matchId = goal.getMatch().getId();
        this.matchDate = goal.getMatch().getMatchDate();
        this.opponentTeamId = goal.getMatch().getOpponentTeam().getId();
        this.opponentTeamName = goal.getMatch().getOpponentTeam().getName();

        boolean isScorer = goal.getScorer() != null && goal.getScorer().getId().equals(requestedMemberId);
        this.type = isScorer ? "GOAL" : "ASSIST";

        if (isScorer) {
            this.relatedMemberId = goal.getAssister() != null ? goal.getAssister().getId() : null;
            this.relatedMemberName = goal.getAssister() != null ? goal.getAssister().getName() : null;
        } else {
            this.relatedMemberId = goal.getScorer() != null ? goal.getScorer().getId() : null;
            this.relatedMemberName = goal.getScorer() != null ? goal.getScorer().getName() : null;
        }
    }
}
