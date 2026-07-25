package com.guenbon.jochuckhub.repository.goal;

import com.guenbon.jochuckhub.entity.Goal;
import com.guenbon.jochuckhub.entity.QGoal;
import com.guenbon.jochuckhub.entity.QMatch;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class QueryDslGoalRepositoryImpl implements QueryDslGoalRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Goal> findGoalRecords(Long teamId, Long memberId, String type, String sortDirection,
                                      Long opponentTeamId, LocalDateTime startDate, LocalDateTime endDate,
                                      Long relatedMemberId, Pageable pageable) {
        QGoal goal = QGoal.goal;
        QMatch match = QMatch.match;
        BooleanBuilder condition = buildCondition(goal, match, teamId, memberId, type,
                opponentTeamId, startDate, endDate, relatedMemberId);
        OrderSpecifier<LocalDateTime> order = "ASC".equals(sortDirection)
                ? match.matchDate.asc() : match.matchDate.desc();

        List<Goal> content = queryFactory
                .selectFrom(goal)
                .join(goal.match, match).fetchJoin()
                .join(match.opponentTeam).fetchJoin()
                .leftJoin(goal.scorer).fetchJoin()
                .leftJoin(goal.assister).fetchJoin()
                .where(condition)
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(goal.count())
                .from(goal)
                .join(goal.match, match)
                .where(condition)
                .fetchOne();
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    private BooleanBuilder buildCondition(QGoal goal, QMatch match, Long teamId, Long memberId,
                                          String type, Long opponentTeamId, LocalDateTime startDate,
                                          LocalDateTime endDate, Long relatedMemberId) {
        BooleanBuilder condition = new BooleanBuilder();
        condition.and(match.homeTeam.id.eq(teamId));
        condition.and(goal.opponentGoal.isFalse());
        condition.and(goal.scorer.id.eq(memberId).or(goal.assister.id.eq(memberId)));

        if ("GOAL".equals(type)) condition.and(goal.scorer.id.eq(memberId));
        if ("ASSIST".equals(type)) condition.and(goal.assister.id.eq(memberId));
        if (opponentTeamId != null) condition.and(match.opponentTeam.id.eq(opponentTeamId));
        if (startDate != null) condition.and(match.matchDate.goe(startDate));
        if (endDate != null) condition.and(match.matchDate.loe(endDate));
        if (relatedMemberId != null) {
            if ("GOAL".equals(type)) condition.and(goal.assister.id.eq(relatedMemberId));
            else if ("ASSIST".equals(type)) condition.and(goal.scorer.id.eq(relatedMemberId));
            else condition.and(goal.scorer.id.eq(relatedMemberId).or(goal.assister.id.eq(relatedMemberId)));
        }
        return condition;
    }
}
