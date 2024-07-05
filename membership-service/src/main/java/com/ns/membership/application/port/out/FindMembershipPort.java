package com.ns.membership.application.port.out;

import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.domain.Membership;

public interface FindMembershipPort {

    MembershipJpaEntity findMembership(
            Membership.MembershipId membershipId
    );

    MembershipJpaEntity findMembershipByEmailAndAddress(
            Membership.MembershipAddress address,
            Membership.MembershipEmail email
    );

    MembershipJpaEntity findMembershipByEmail(
            Membership.MembershipEmail email
    );

    MembershipJpaEntity findMembershipByAddress(
            Membership.MembershipAddress address
    );

}
