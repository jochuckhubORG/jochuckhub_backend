package com.guenbon.jochuckhub.repository.goal;

import com.guenbon.jochuckhub.entity.Goal;
import com.guenbon.jochuckhub.entity.QGoal;
import com.guenbon.jochuckhub.entity.QMatch;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class QueryDslGoalRepositoryImpl implements QueryDslGoalRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Goal> findGoalRecords(Long teamId, Long memberId, String type, String sortDirection,
                                      Long opponentTeamId, LocalDateTime startDate, LocalDateTime endDate,
                                      Long relatedMemberId) {
        QGoal goal = QGoal.goal;
        QMatch match = QMatch.match;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(match.homeTeam.id.eq(teamId));
        builder.and(goal.opponentGoal.isFalse());
        builder.and(goal.scorer.id.eq(memberId).or(goal.assister.id.eq(memberId)));

        if ("GOAL".equalsIgnoreCase(type)) {
            builder.and(goal.scorer.id.eq(memberId));
        } else if ("ASSIST".equalsIgnoreCase(type)) {
            builder.and(goal.assister.id.eq(memberId));
        }

        if (opponentTeamId != null) builder.and(match.opponentTeam.id.eq(opponentTeamId));
        if (startDate != null) builder.and(match.matchDate.goe(startDate));
        if (endDate != null) builder.and(match.matchDate.loe(endDate));
        if (relatedMemberId != null) {
            if ("GOAL".equalsIgnoreCase(type)) {
                builder.and(goal.assister.id.eq(relatedMemberId));
            } else if ("ASSIST".equalsIgnoreCase(type)) {
                builder.and(goal.scorer.id.eq(relatedMemberId));
            } else {
                builder.and(goal.scorer.id.eq(relatedMemberId).or(goal.assister.id.eq(relatedMemberId)));
            }
        }

        OrderSpecifier<LocalDateTime> order = "ASC".equalsIgnoreCase(sortDirection)
                ? match.matchDate.asc()
                : match.matchDate.desc();

        return queryFactory
                .selectFrom(goal)
                .join(goal.match, match).fetchJoin()
                .join(match.opponentTeam).fetchJoin()
                .leftJoin(goal.scorer).fetchJoin()
                .leftJoin(goal.assister).fetchJoin()
                .where(builder)
                .orderBy(order)
                .fetch();
    }
}
