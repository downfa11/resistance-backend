package com.ns.membership.application.port.in;

import com.ns.membership.application.port.in.command.RegisterMembershipCommand;
import com.ns.membership.domain.Membership;

public interface RegisterMembershipUseCase {
    Membership registerMembership(RegisterMembershipCommand command);
}
