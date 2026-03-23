package com.guenbon.jochuckhub.dto.request;

import lombok.Getter;

@Getter
public class GoalRequest {

    private boolean opponentGoal;

    // opponentGoal = false일 때 필수
    private Long scorerMemberId;

    // 선택 (홈팀 골일 때만)
    private Long assisterMemberId;
}
