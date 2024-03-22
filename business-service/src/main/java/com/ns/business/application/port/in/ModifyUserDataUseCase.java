package com.ns.business.application.port.in;


import com.ns.business.application.port.in.command.ModifyUserDataCommand;
import com.ns.business.domain.UserData;

public interface ModifyUserDataUseCase {
    UserData modifyUserData(ModifyUserDataCommand command);
}
