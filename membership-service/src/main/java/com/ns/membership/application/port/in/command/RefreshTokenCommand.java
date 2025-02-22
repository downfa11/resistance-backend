package com.ns.membership.application.port.in.command;

import com.ns.common.SelfValidating;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
public class RefreshTokenCommand extends SelfValidating<RefreshTokenCommand> {

    private final String refreshToken;

    public RefreshTokenCommand(String refreshToken) {
        this.refreshToken = refreshToken;

        this.validateSelf();
    }
}
