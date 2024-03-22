package com.ns.membership.application.port.in.command;

import com.ns.common.SelfValidating;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)

public class RegisterMembershipCommand extends SelfValidating<RegisterMembershipCommand> {
    // CRUD에서 c,u,d를 위한 기능을 위한 Command
    @NotNull
    private final String name;
    @NotNull
    private final String email;
    @NotNull
    @NotBlank
    private final String address;

    @AssertTrue
    private final boolean isValid;

    public RegisterMembershipCommand(String name, String email, String address, boolean isValid) {
        this.name = name;
        this.email = email;
        this.address = address;
        this.isValid = isValid;

        this.validateSelf();
    }
}

