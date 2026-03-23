package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Goal;
import lombok.Getter;

@Getter
public class GoalResponse {

    private final Long id;
    private final boolean opponentGoal;
    private final Long scorerId;
    private final String scorerName;
    private final Long assisterId;
    private final String assisterName;

    public GoalResponse(Goal goal) {
        this.id = goal.getId();
        this.opponentGoal = goal.isOpponentGoal();
        this.scorerId = goal.getScorer() != null ? goal.getScorer().getId() : null;
        this.scorerName = goal.getScorer() != null ? goal.getScorer().getName() : null;
        this.assisterId = goal.getAssister() != null ? goal.getAssister().getId() : null;
        this.assisterName = goal.getAssister() != null ? goal.getAssister().getName() : null;
    }
}
