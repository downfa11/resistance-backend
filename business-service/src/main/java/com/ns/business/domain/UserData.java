package com.ns.business.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserData {
    @Getter private Long userId;
    @Getter private String name;
    @Getter private int gold;
    @Getter private int highscore;
    @Getter private int energy;
    @Getter private int scenario;
    @Getter private String head;
    @Getter private String body;
    @Getter private String arm;
    @Getter private int health;
    @Getter private int attack;
    @Getter private int critical;
    @Getter private int durability;

    public static UserData generateUserData(UserId userId, UserName userName, UserGold userGold, UserHighscore userHighscore,
                                            UserEnergy userEnergy, UserScenario userScenario, UserHead userHead, UserBody userBody, UserArm userArm,
                                            UserHealth userHealth, UserAttack userAttack, UserCritical userCritical, UserDurability userDurability){
        return new UserData(userId.userId, userName.userName, userGold.userGold, userHighscore.userHighscore,
                userEnergy.userEnergy, userScenario.userScenario, userHead.userHead, userBody.userBody,
                userArm.userArm, userHealth.userHealth, userAttack.userAttack, userCritical.userCritical, userDurability.userDurability);
    }

    @Value
    public static class UserId {
        private Long userId;

        public UserId(Long userId) {
            this.userId = userId;
        }
    }

    @Value
    public static class UserName {
        private String userName;

        public UserName(String userName) {
            this.userName = userName;
        }
    }

    @Value
    public static class UserGold {
        private int userGold;

        public UserGold(int userGold) {
            this.userGold = userGold;
        }
    }

    @Value
    public static class UserHighscore {
        private int userHighscore;

        public UserHighscore(int userHighscore) {
            this.userHighscore = userHighscore;
        }
    }

    @Value
    public static class UserEnergy {
        private int userEnergy;

        public UserEnergy(int userEnergy) {
            this.userEnergy = userEnergy;
        }
    }

    @Value
    public static class UserScenario {
        private int userScenario;

        public UserScenario(int userScenario) {
            this.userScenario = userScenario;
        }
    }

    @Value
    public static class UserHead {
        private String userHead;

        public UserHead(String userHead) {
            this.userHead = userHead;
        }
    }

    @Value
    public static class UserBody {
        private String userBody;

        public UserBody(String userBody) {
            this.userBody = userBody;
        }
    }

    @Value
    public static class UserArm {
        private String userArm;

        public UserArm(String userArm) {
            this.userArm = userArm;
        }
    }

    @Value
    public static class UserHealth {
        private int userHealth;

        public UserHealth(int userHealth) {
            this.userHealth = userHealth;
        }
    }

    @Value
    public static class UserAttack {
        private int userAttack;

        public UserAttack(int userAttack) {
            this.userAttack = userAttack;
        }
    }

    @Value
    public static class UserCritical {
        private int userCritical;

        public UserCritical(int userCritical) {
            this.userCritical = userCritical;
        }
    }

    @Value
    public static class UserDurability {
        private int userDurability;

        public UserDurability(int userDurability) {
            this.userDurability = userDurability;
        }
    }
}