package com.guenbon.jochuckhub.repository.goal;

import com.guenbon.jochuckhub.entity.Goal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface QueryDslGoalRepository {

    Page<Goal> findGoalRecords(Long teamId, Long memberId, String type, String sortDirection,
                               Long opponentTeamId, LocalDateTime startDate, LocalDateTime endDate,
                               Long relatedMemberId, Pageable pageable);
}
