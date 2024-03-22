package com.ns.membership.application.port.in;

import com.ns.membership.application.port.in.command.FindMembershipCommand;
import com.ns.membership.domain.Membership;

public interface FindMembershipUseCase {
    Membership findMembership(FindMembershipCommand command);
}
