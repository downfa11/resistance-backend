package com.ns.business.application.port.in;


import com.ns.business.application.port.in.command.FindUserDataCommand;
import com.ns.business.domain.UserData;

public interface FindUserDataUseCase {
    UserData findUserData(FindUserDataCommand command);
}
