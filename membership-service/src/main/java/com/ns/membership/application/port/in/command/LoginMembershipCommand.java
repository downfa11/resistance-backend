package com.ns.membership.application.port.in.command;

import com.ns.common.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)

public class LoginMembershipCommand extends SelfValidating<LoginMembershipCommand> {
    @NotNull
    private final String address;
    @NotNull
    private final String email;

    public LoginMembershipCommand(String address, String email) {
        this.address = address;
        this.email = email;

        this.validateSelf();
    }
}
