package com.ns.membership.application.port.in.command;

import com.ns.common.SelfValidating;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
public class ValidateTokenCommand extends SelfValidating<ValidateTokenCommand> {

    private final String jwtToken;

    public ValidateTokenCommand(String jwtToken) {
        this.jwtToken = jwtToken;

        this.validateSelf();
    }
}
