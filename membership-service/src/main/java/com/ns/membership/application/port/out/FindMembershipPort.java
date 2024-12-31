package com.ns.membership.application.port.out;

import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.domain.Membership;
import java.util.Optional;

public interface FindMembershipPort {

    Optional<MembershipJpaEntity> findMembership(Membership.MembershipId membershipId);

    Optional<MembershipJpaEntity> findMembershipByAccountOrEmail(Membership.MembershipAccount account, Membership.MembershipEmail email);

    Optional<MembershipJpaEntity> findMembershipByAccountAndPassword(Membership.MembershipAccount account, Membership.MembershipPassword password);

    Optional<MembershipJpaEntity> findMembershipByEmail(Membership.MembershipEmail email);

    Optional<MembershipJpaEntity> findMembershipByAccount(Membership.MembershipAccount account);

    Optional<MembershipJpaEntity> findMembershipByName(Membership.MembershipName name);

}
