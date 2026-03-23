package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Goal;
import lombok.Getter;

import java.util.List;

@Getter
public class MatchResultResponse {

    private final Long matchId;
    private final int homeScore;
    private final int opponentScore;
    private final List<GoalResponse> goals;

    public MatchResultResponse(Long matchId, List<Goal> goals) {
        this.matchId = matchId;
        this.homeScore = (int) goals.stream().filter(g -> !g.isOpponentGoal()).count();
        this.opponentScore = (int) goals.stream().filter(Goal::isOpponentGoal).count();
        this.goals = goals.stream().map(GoalResponse::new).toList();
    }
}
