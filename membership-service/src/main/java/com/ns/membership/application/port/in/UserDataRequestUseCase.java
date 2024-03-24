package com.ns.membership.application.port.in;

import com.ns.membership.application.port.in.command.UserDataRequestCommand;
import com.ns.membership.domain.userData;

import java.util.List;
import java.util.Set;

public interface UserDataRequestUseCase {
    List<userData> getUserData(UserDataRequestCommand command);

    Set<Long> getAllyRandom(String membershipId);
}
