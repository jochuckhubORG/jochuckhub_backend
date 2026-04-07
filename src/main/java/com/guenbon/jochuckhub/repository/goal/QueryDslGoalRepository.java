package com.guenbon.jochuckhub.repository.goal;

import com.guenbon.jochuckhub.entity.Goal;

import java.time.LocalDateTime;
import java.util.List;

public interface QueryDslGoalRepository {

    List<Goal> findGoalRecords(Long teamId, Long memberId, String type, String sortDirection,
                               Long opponentTeamId, LocalDateTime startDate, LocalDateTime endDate,
                               Long relatedMemberId);
}
