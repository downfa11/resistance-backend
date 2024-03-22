package com.ns.business.application.port.in;


import com.ns.business.application.port.in.command.RegisterUserDataCommand;
import com.ns.business.domain.UserData;

public interface RegisterUserDataUseCase {
    UserData registerUserData(RegisterUserDataCommand command);
}
