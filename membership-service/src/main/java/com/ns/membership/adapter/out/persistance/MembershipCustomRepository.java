package com.ns.membership.adapter.out.persistance;

import java.util.Optional;

public interface MembershipCustomRepository {
    Optional<MembershipJpaEntity> findByMembershipId(Long membershipId);
    Optional<MembershipJpaEntity> findByAddress(String address);
    Optional<MembershipJpaEntity> findByEmail(String email);
    Optional<MembershipJpaEntity> findByAccount(String account);
    Optional<MembershipJpaEntity> findByName(String name);
    Optional<MembershipJpaEntity> findByAccountAndPassword(String account, String password);
    Optional<MembershipJpaEntity> findByAccountOrEmail(String account, String email);
}
