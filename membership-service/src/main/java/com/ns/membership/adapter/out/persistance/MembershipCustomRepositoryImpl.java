package com.ns.membership.adapter.out.persistance;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.springframework.stereotype.Repository;

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
                .where(membership.membershipId.eq(membershipId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<MembershipJpaEntity> findByAddress(String address) {
        QMembershipJpaEntity membership = QMembershipJpaEntity.membershipJpaEntity;

        MembershipJpaEntity result = jpaQueryFactory
                .selectFrom(membership)
                .where(membership.address.eq(address))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<MembershipJpaEntity> findByEmail(String email) {
        QMembershipJpaEntity membership = QMembershipJpaEntity.membershipJpaEntity;

        MembershipJpaEntity result = jpaQueryFactory
                .selectFrom(membership)
                .where(membership.email.eq(email))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<MembershipJpaEntity> findByAccount(String account) {
        QMembershipJpaEntity membership = QMembershipJpaEntity.membershipJpaEntity;

        MembershipJpaEntity result = jpaQueryFactory
                .selectFrom(membership)
                .where(membership.account.eq(account))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<MembershipJpaEntity> findByName(String name) {
        QMembershipJpaEntity membership = QMembershipJpaEntity.membershipJpaEntity;

        MembershipJpaEntity result = jpaQueryFactory
                .selectFrom(membership)
                .where(membership.name.eq(name))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<MembershipJpaEntity> findByAccountAndPassword(String account, String password) {
        QMembershipJpaEntity membership = QMembershipJpaEntity.membershipJpaEntity;

        MembershipJpaEntity result = jpaQueryFactory
                .selectFrom(membership)
                .where(membership.account.eq(account).and(membership.password.eq(password)))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<MembershipJpaEntity> findByAccountOrEmail(String account, String email) {
        QMembershipJpaEntity membership = QMembershipJpaEntity.membershipJpaEntity;
        MembershipJpaEntity result = jpaQueryFactory
                .selectFrom(membership)
                .where(membership.account.eq(account).or(membership.email.eq(email)))
                .fetchOne();

        return Optional.ofNullable(result);
    }

}