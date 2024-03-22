package com.ns.membership.application.port.in;

import com.ns.membership.application.port.in.command.UserDataRequestCommand;
import com.ns.membership.domain.userData;

import java.util.List;

public interface UserDataRequestUseCase {
    List<userData> getUserData(UserDataRequestCommand command);

    List<String> getAllyRandom(String membershipId);
}
