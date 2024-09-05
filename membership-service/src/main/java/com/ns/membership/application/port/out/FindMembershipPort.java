package com.ns.membership.application.port.out;

import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.domain.Membership;

public interface FindMembershipPort {

    MembershipJpaEntity findMembership(
            Membership.MembershipId membershipId
    );

    MembershipJpaEntity findMembershipByAccountOrEmail(
            Membership.MembershipAccount account,
            Membership.MembershipEmail email
    );

    MembershipJpaEntity findMembershipByAccountAndPassword(
            Membership.MembershipAccount account,
            Membership.MembershipPassword password
    );

    MembershipJpaEntity findMembershipByEmail(
            Membership.MembershipEmail email
    );

    MembershipJpaEntity findMembershipByAccount(
            Membership.MembershipAccount account
    );

    MembershipJpaEntity findMembershipByName(
            Membership.MembershipName name
    );

}
