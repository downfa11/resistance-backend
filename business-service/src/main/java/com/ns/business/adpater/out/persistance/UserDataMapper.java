package com.ns.business.adpater.out.persistance;

import com.ns.business.domain.UserData;
import com.ns.business.domain.UserDataMembership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDataMapper {

    private final UserDataRepository userDataRepository;

    public UserData mapToDomainEntity(UserDataJpaEntity userDataJpaEntity) {
        return UserData.generateUserData(
                new UserData.UserId(userDataJpaEntity.getUserId()),
                new UserData.UserName(userDataJpaEntity.getName()),
                new UserData.UserGold(userDataJpaEntity.getGold()),
                new UserData.UserHighscore(userDataJpaEntity.getHighscore()),
                new UserData.UserEnergy(userDataJpaEntity.getEnergy()),
                new UserData.UserScenario(userDataJpaEntity.getScenario()),
                new UserData.UserHead(userDataJpaEntity.getHead()),
                new UserData.UserBody(userDataJpaEntity.getBody()),
                new UserData.UserArm(userDataJpaEntity.getArm()),
                new UserData.UserHealth(userDataJpaEntity.getHealth()),
                new UserData.UserAttack(userDataJpaEntity.getAttack()),
                new UserData.UserCritical(userDataJpaEntity.getCritical()),
                new UserData.UserDurability(userDataJpaEntity.getDurability())
        );
    }

    public UserDataMembership mapToDomain(UserData userData) {
        return UserDataMembership.generateUserData(
                new UserDataMembership.UserMemberId(userData.getUserId()),
                new UserDataMembership.UserMemberName(userData.getName()),
                new UserDataMembership.UserMemberHighscore(userData.getHighscore()),
                new UserDataMembership.UserMemberHead(userData.getHead()),
                new UserDataMembership.UserMemberBody(userData.getBody()),
                new UserDataMembership.UserMemberArm(userData.getArm()),
                new UserDataMembership.UserMemberHealth(userData.getHealth()),
                new UserDataMembership.UserMemberAttack(userData.getAttack()),
                new UserDataMembership.UserMemberCritical(userData.getCritical()),
                new UserDataMembership.UserMemberDurability(userData.getDurability())
        );
    }

    public UserDataMembership mapToDomainJump(Long userId){
        UserDataJpaEntity userDataJpaEntity = userDataRepository.findByUserId(userId);

        if (userDataJpaEntity == null) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        UserData userData = mapToDomainEntity(userDataJpaEntity);
        return mapToDomain(userData);
    }

}
