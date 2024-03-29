package com.ns.membership.application.port.in;

import com.ns.membership.application.port.in.command.LoginMembershipCommand;
import com.ns.membership.application.port.in.command.RefreshTokenCommand;
import com.ns.membership.application.port.in.command.ValidateTokenCommand;
import com.ns.membership.domain.JWtToken;
import com.ns.membership.domain.Membership;

public interface LoginMembershipUseCase {

    JWtToken LoginMembership(LoginMembershipCommand command);
    JWtToken refreshJwtTokenByRefreshToken(RefreshTokenCommand command);
    boolean validateJwtToken(ValidateTokenCommand command);
    Membership getMembershipByJwtToken(ValidateTokenCommand command);

}
