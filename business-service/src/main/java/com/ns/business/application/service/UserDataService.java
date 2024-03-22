package com.ns.business.application.service;

import com.ns.business.adpater.out.persistance.UserDataJpaEntity;
import com.ns.business.adpater.out.persistance.UserDataMapper;
import com.ns.business.application.port.in.FindUserDataUseCase;
import com.ns.business.application.port.in.ModifyUserDataUseCase;
import com.ns.business.application.port.in.RegisterUserDataUseCase;
import com.ns.business.application.port.in.command.FindUserDataCommand;
import com.ns.business.application.port.in.command.ModifyUserDataCommand;
import com.ns.business.application.port.in.command.RegisterUserDataCommand;
import com.ns.business.application.port.out.FindUserDataPort;
import com.ns.business.application.port.out.ModifyUserDataPort;
import com.ns.business.application.port.out.RegisterUserDataPort;
import com.ns.business.domain.UserData;
import com.ns.common.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UserDataService implements RegisterUserDataUseCase, ModifyUserDataUseCase, FindUserDataUseCase {

    private final RegisterUserDataPort registerUserDataPort;
    private final ModifyUserDataPort modifyUserDataPort;
    private final FindUserDataPort findUserDataPort;
    private final UserDataMapper userDataMapper;

    @Override
    @Transactional
    public UserData registerUserData(RegisterUserDataCommand command) {
        UserDataJpaEntity jpaEntity = registerUserDataPort.createUserData(
                new UserData.UserId(command.getUserId()),
                new UserData.UserName(command.getName()),
                new UserData.UserGold(command.getGold()),
                new UserData.UserHighscore(command.getHighscore()),
                new UserData.UserEnergy(command.getEnergy()),
                new UserData.UserScenario(command.getScenario()),
                new UserData.UserHead(command.getHead()),
                new UserData.UserBody(command.getBody()),
                new UserData.UserArm(command.getArm()),
                new UserData.UserHealth(command.getHealth()),
                new UserData.UserAttack(command.getAttack()),
                new UserData.UserCritical(command.getCritical()),
                new UserData.UserDurability(command.getDurability())
        );

        return userDataMapper.mapToDomainEntity(jpaEntity);
    }

    @Override
    @Transactional
    public UserData modifyUserData(ModifyUserDataCommand command) {
        UserDataJpaEntity jpaEntity = modifyUserDataPort.modifyUserData(
                new UserData.UserId(command.getUserId()),
                new UserData.UserGold(command.getGold()),
                new UserData.UserHighscore(command.getHighscore()),
                new UserData.UserEnergy(command.getEnergy()),
                new UserData.UserScenario(command.getScenario()),
                new UserData.UserHead(command.getHead()),
                new UserData.UserBody(command.getBody()),
                new UserData.UserArm(command.getArm()),
                new UserData.UserHealth(command.getHealth()),
                new UserData.UserAttack(command.getAttack()),
                new UserData.UserCritical(command.getCritical()),
                new UserData.UserDurability(command.getDurability())
        );

        return userDataMapper.mapToDomainEntity(jpaEntity);
    }

    @Override
    @Transactional
    public UserData findUserData(FindUserDataCommand command) {
        UserDataJpaEntity entity = findUserDataPort.findUserData(new UserData.UserId(command.getUserId()));
        return userDataMapper.mapToDomainEntity(entity);
    }
}
