package com.ns.membership.application.port.in;

import com.ns.membership.application.port.in.command.ModifyMembershipCommand;
import com.ns.membership.domain.Membership;

public interface ModifyMembershipUseCase {
    Membership modifyMembership(ModifyMembershipCommand command);
}
