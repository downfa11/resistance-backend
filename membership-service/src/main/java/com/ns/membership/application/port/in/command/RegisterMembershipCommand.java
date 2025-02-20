package com.ns.membership.application.port.in.command;

import com.ns.common.SelfValidating;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode(callSuper = false)

public class RegisterMembershipCommand extends SelfValidating<RegisterMembershipCommand> {
    // CRUD에서 c,u,d를 위한 기능을 위한 Command
    @NotNull
    private final String name;
    @NotNull
    private final String account;
    @NotNull
    private final String password;
    @NotNull
    private final String email;
    @NotNull
    private final String address;

    @AssertTrue
    private final boolean isValid;

    public RegisterMembershipCommand(String name, String account, String password,String email, String address, boolean isValid) {
        this.name = name;
        this.account = account;
        this.password = password;
        this.email = email;
        this.address = address;
        this.isValid = isValid;

        this.validateSelf();
    }
}

