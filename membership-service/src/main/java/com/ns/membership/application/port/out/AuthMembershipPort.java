package com.ns.membership.application.port.out;

import com.ns.membership.domain.Membership;

public interface AuthMembershipPort {
    String generateJwtToken(
            Membership.MembershipId membershipID
    );

    String generateRefreshToken(
            Membership.MembershipId membershipId
    );

    boolean validateJwtToken(String jwtToken);

    Membership.MembershipId parseMembershipIdFromToken(String jwtToken);
}

