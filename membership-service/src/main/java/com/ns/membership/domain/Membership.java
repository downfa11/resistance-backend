package com.ns.membership.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

import java.util.List;
import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Membership {

    @Getter
    private final String membershipId;
    @Getter private final String name;
    @Getter private final String email;
    @Getter private final String address;
    @Getter private final boolean isValid;
    @Getter private final Set<Long> friends;
    @Getter private final Set<Long> wantedFriends;

    @Getter private final String refreshToken;


    public static Membership generateMember(
            MembershipId membershipId,
            MembershipName membershipName,
            MembershipAddress membershipAddress,
            MembershipEmail membershipEmail,
            MembershipIsValid membershipIsValid,
            Friends friends,
            WantedFriends wantedFriends,
            RefreshToken refreshToken){

        return new Membership(
                membershipId.getMembershipId(),
                membershipName.getNameValue(),
                membershipEmail.getEmailValue(),
                membershipAddress.getAddressValue(),
                membershipIsValid.isValidValue(),
                friends.getFriends(),
                wantedFriends.getWantedfriends(),
                refreshToken.getRefreshToken()
        );
    }
    @Value
    public static class MembershipId{
        public MembershipId(String value){
            this.membershipId = value;
        }

        String membershipId;
    }

    @Value
    public static class MembershipName{
        public MembershipName(String value){
            this.nameValue = value;
        }

        String nameValue;
    }
    @Value
    public static class MembershipAddress{
        public MembershipAddress(String value){
            this.addressValue = value;
        }

        String addressValue;
    }
    @Value
    public static class MembershipIsValid{
        public MembershipIsValid(boolean value){
            this.isValidValue = value;
        }

        boolean isValidValue;
    }

    @Value
    public static class MembershipEmail{
        public MembershipEmail(String value){
            this.emailValue = value;
        }

        String emailValue;
    }
    @Value
    public static class RefreshToken{
        public RefreshToken(String value){
            this.refreshToken = value;
        }

        String refreshToken;
    }
    @Value
    public static class Friends {
        private Set<Long> friends;

        public Friends(Set<Long> friends) {
            this.friends = friends;
        }
    }

    @Value
    public static class WantedFriends {
        private Set<Long> wantedfriends;

        public WantedFriends(Set<Long> wantedfriends) {
            this.wantedfriends = wantedfriends;
        }
    }

}


