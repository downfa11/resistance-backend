package com.ns.business.application.port.out;


import com.ns.business.adpater.out.persistance.UserDataJpaEntity;
import com.ns.business.domain.UserData;

public interface RegisterUserDataPort {

    UserDataJpaEntity createUserData(
            UserData.UserId userId,
            UserData.UserName userName,
            UserData.UserGold userGold,
            UserData.UserHighscore userHighscore,
            UserData.UserEnergy userEnergy,
            UserData.UserScenario userScenario,
            UserData.UserHead userHead,
            UserData.UserBody userBody,
            UserData.UserArm userArm,
            UserData.UserHealth userHealth,
            UserData.UserAttack userAttack,
            UserData.UserCritical userCritical,
            UserData.UserDurability userDurability
    );
}
