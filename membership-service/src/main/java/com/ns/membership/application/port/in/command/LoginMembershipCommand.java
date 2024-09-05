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
    private final String account;
    @NotNull
    private final String password;

    public LoginMembershipCommand(String account, String password) {
        this.account = account;
        this.password = password;

        this.validateSelf();
    }
}
