package com.ns.business.application.port.out;


import com.ns.business.adpater.out.persistance.UserDataJpaEntity;
import com.ns.business.domain.UserData;

public interface FindUserDataPort {

    UserDataJpaEntity findUserData(
            UserData.UserId userId
    );
}
