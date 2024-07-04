package com.ns.business.adpater.out.persistance;

import com.ns.business.application.port.out.FindUserDataPort;
import com.ns.business.application.port.out.ModifyUserDataPort;
import com.ns.business.application.port.out.RegisterUserDataPort;
import com.ns.business.domain.UserData;
import com.ns.common.PersistanceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@PersistanceAdapter
@Slf4j
public class UserDataPersistanceAdapter implements RegisterUserDataPort, FindUserDataPort, ModifyUserDataPort {

    private final UserDataRepository userDataRepository;


    @Override
    public UserDataJpaEntity createUserData(UserData.UserId userId, UserData.UserName userName, UserData.UserGold userGold, UserData.UserHighscore userHighscore, UserData.UserEnergy userEnergy, UserData.UserScenario userScenario, UserData.UserHead userHead, UserData.UserBody userBody, UserData.UserArm userArm, UserData.UserHealth userHealth, UserData.UserAttack userAttack, UserData.UserCritical userCritical, UserData.UserDurability userDurability) {
        UserDataJpaEntity jpaEntity = new UserDataJpaEntity(
                userId.getUserId(),
                userName.getUserName(),
                userGold.getUserGold(),
                userHighscore.getUserHighscore(),
                userEnergy.getUserEnergy(),
                userScenario.getUserScenario(),
                userHead.getUserHead(),
                userBody.getUserBody(),
                userArm.getUserArm(),
                userHealth.getUserHealth(),
                userAttack.getUserAttack(),
                userCritical.getUserCritical(),
                userDurability.getUserDurability()
        );
        userDataRepository.save(jpaEntity);

        return new UserDataJpaEntity(
                jpaEntity.getId(),
                userName.getUserName(),
                userGold.getUserGold(),
                userHighscore.getUserHighscore(),
                userEnergy.getUserEnergy(),
                userScenario.getUserScenario(),
                userHead.getUserHead(),
                userBody.getUserBody(),
                userArm.getUserArm(),
                userHealth.getUserHealth(),
                userAttack.getUserAttack(),
                userCritical.getUserCritical(),
                userDurability.getUserDurability()
        );
    }

    @Override
    public UserDataJpaEntity modifyUserData(UserData.UserId userId,UserData.UserGold userGold, UserData.UserHighscore userHighscore, UserData.UserEnergy userEnergy, UserData.UserScenario userScenario, UserData.UserHead userHead, UserData.UserBody userBody, UserData.UserArm userArm, UserData.UserHealth userHealth, UserData.UserAttack userAttack, UserData.UserCritical userCritical, UserData.UserDurability userDurability) {
        UserDataJpaEntity entity = userDataRepository.getById(userId.getUserId());

        entity.setGold(userGold.getUserGold());
        entity.setHighscore(userHighscore.getUserHighscore());
        entity.setEnergy(userEnergy.getUserEnergy());
        entity.setScenario(userScenario.getUserScenario());

        entity.setHead(userHead.getUserHead());
        entity.setBody(userBody.getUserBody());
        entity.setArm(userArm.getUserArm());

        entity.setHealth(userHealth.getUserHealth());
        entity.setAttack(userAttack.getUserAttack());
        entity.setCritical(userCritical.getUserCritical());
        entity.setDurability(userDurability.getUserDurability());
        userDataRepository.save(entity);

        return new UserDataJpaEntity(
                entity.getId(),
                entity.getName(),
                userGold.getUserGold(),
                userHighscore.getUserHighscore(),
                userEnergy.getUserEnergy(),
                userScenario.getUserScenario(),
                userHead.getUserHead(),
                userBody.getUserBody(),
                userArm.getUserArm(),
                userHealth.getUserHealth(),
                userAttack.getUserAttack(),
                userCritical.getUserCritical(),
                userDurability.getUserDurability()
        );

    }
    @Override
    public UserDataJpaEntity findUserData(UserData.UserId userId) {
        return userDataRepository.findByUserId(userId.getUserId());
    }

}
