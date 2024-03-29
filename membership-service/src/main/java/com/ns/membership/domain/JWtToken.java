package com.ns.membership.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JWtToken {

    @Getter
    private final String membershipId;
    @Getter
    private final String jwtToken;
    @Getter
    private final String refreshToken;


    public static JWtToken generateJwtToken(
            MembershipId membershipId,
            MembershipJwtToken membershipJwtToken,
            MembershipRefreshToken membershipRefreshToken) {

        return new JWtToken(
                membershipId.membershipId,
                membershipJwtToken.jwtToken,
                membershipRefreshToken.refreshToken
        );
    }

    @Value
    public static class MembershipId {
        public MembershipId(String value) {
            this.membershipId = value;
        }

        String membershipId;
    }

    @Value
    public static class MembershipJwtToken {
        public MembershipJwtToken(String jwtToken) {
            this.jwtToken = jwtToken;
        }

        static String jwtToken;
    }

    @Value
    public static class MembershipRefreshToken {
        public MembershipRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        String refreshToken;
    }
}
