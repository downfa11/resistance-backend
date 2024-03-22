package com.ns.membership.application.port.out;

import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.domain.Membership;

public interface ModifyMembershipPort {

    MembershipJpaEntity modifyMembership(
            Membership.MembershipId membershipId,
            Membership.MembershipName membershipName,
            Membership.MembershipAddress membershipAddress,
            Membership.MembershipEmail membershipEmail,
            Membership.MembershipIsValid membershipIsValid,
            Membership.Friends friends,
            Membership.WantedFriends wantedFriends,
            Membership.RefreshToken refreshToken);
}
