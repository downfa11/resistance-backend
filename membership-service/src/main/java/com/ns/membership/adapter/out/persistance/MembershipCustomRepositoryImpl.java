package com.ns.membership.adapter.out.persistance;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MembershipCustomRepositoryImpl implements MembershipCustomRepository{
    private final JPAQueryFactory jpaQueryFactory;
    private final QMembershipJpaEntity qMembershipJpaEntity;

    public MembershipCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
        this.qMembershipJpaEntity=QMembershipJpaEntity.membershipJpaEntity;
    }

    @Override
    public Optional<MembershipJpaEntity> findByMembershipId(Long membershipId) {
        QMembershipJpaEntity membership = QMembershipJpaEntity.membershipJpaEntity;

        MembershipJpaEntity result = jpaQueryFactory
                .selectFrom(membership)
                .leftJoin(membership.friends).fetchJoin()
                .leftJoin(membership.wantedFriends).fetchJoin()
                .where(membership.membershipId.eq(membershipId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
