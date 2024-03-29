package com.ns.membership.application.port.in.command;

import com.ns.common.SelfValidating;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class ValidateTokenCommand extends SelfValidating<ValidateTokenCommand> {

    private final String jwtToken;

    public ValidateTokenCommand(String jwtToken) {
        this.jwtToken = jwtToken;

        this.validateSelf();
    }
}
