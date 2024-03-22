package com.ns.business.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDataMembership {
    @Getter
    private final Long membershipId;
    @Getter private final String name;
    @Getter private final int highScore;

    @Getter private final String head;
    @Getter private final String body;
    @Getter private final String arm;

    @Getter private final int health;
    @Getter private final int attack;
    @Getter private final int critical;
    @Getter private final int durability;

    public static UserDataMembership generateUserData(UserMemberId userId, UserMemberName userName, UserMemberHighscore userHighscore,
                                                       UserMemberHead userHead, UserMemberBody userBody, UserMemberArm userArm,
                                                      UserMemberHealth userHealth, UserMemberAttack userAttack, UserMemberCritical userCritical, UserMemberDurability userDurability){
        return new UserDataMembership(userId.userId, userName.userName, userHighscore.userHighscore,
                userHead.userHead, userBody.userBody,
                userArm.userArm, userHealth.userHealth, userAttack.userAttack, userCritical.userCritical, userDurability.userDurability);
    }
    @Value
    public static class UserMemberId {
        private Long userId;

        public UserMemberId(Long UserMemberId) {
            this.userId = UserMemberId;
        }
    }

    @Value
    public static class UserMemberName {
        private String userName;

        public UserMemberName(String userName) {
            this.userName = userName;
        }
    }

    @Value
    public static class UserMemberHighscore {
        private int userHighscore;

        public UserMemberHighscore(int userHighscore) {
            this.userHighscore = userHighscore;
        }
    }

    @Value
    public static class UserMemberHead {
        private String userHead;

        public UserMemberHead(String userHead) {
            this.userHead = userHead;
        }
    }

    @Value
    public static class UserMemberBody {
        private String userBody;

        public UserMemberBody(String userBody) {
            this.userBody = userBody;
        }
    }

    @Value
    public static class UserMemberArm {
        private String userArm;

        public UserMemberArm(String userArm) {
            this.userArm = userArm;
        }
    }

    @Value
    public static class UserMemberHealth {
        private int userHealth;

        public UserMemberHealth(int userHealth) {
            this.userHealth = userHealth;
        }
    }

    @Value
    public static class UserMemberAttack {
        private int userAttack;

        public UserMemberAttack(int userAttack) {
            this.userAttack = userAttack;
        }
    }

    @Value
    public static class UserMemberCritical {
        private int userCritical;

        public UserMemberCritical(int userCritical) {
            this.userCritical = userCritical;
        }
    }

    @Value
    public static class UserMemberDurability {
        private int userDurability;

        public UserMemberDurability(int userDurability) {
            this.userDurability = userDurability;
        }
    }
}