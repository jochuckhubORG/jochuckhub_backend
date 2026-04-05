package com.guenbon.jochuckhub.repository.goal;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryDslProblemRepositoryImpl implements QueryDslGoalRepository{

    private final JPAQueryFactory queryFactory;


}
