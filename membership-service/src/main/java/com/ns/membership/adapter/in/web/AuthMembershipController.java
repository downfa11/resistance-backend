package com.ns.membership.adapter.in.web;


import com.ns.common.WebAdapter;
import com.ns.membership.adapter.in.web.dto.LoginMembershipRequest;
import com.ns.membership.adapter.in.web.dto.RefreshTokenRequest;
import com.ns.membership.adapter.in.web.dto.ValidateTokenRequest;
import com.ns.membership.application.port.in.LoginMembershipUseCase;
import com.ns.membership.application.port.in.command.LoginMembershipCommand;
import com.ns.membership.application.port.in.command.RefreshTokenCommand;
import com.ns.membership.application.port.in.command.ValidateTokenCommand;
import com.ns.membership.domain.JWtToken;
import com.ns.membership.domain.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebAdapter
@RestController
@RequiredArgsConstructor
public class AuthMembershipController {

    private final LoginMembershipUseCase loginMembershipUseCase;

    @PostMapping(path="/membership/login")
    JWtToken loginMembership(@RequestBody LoginMembershipRequest request){

        LoginMembershipCommand command = LoginMembershipCommand.builder()
                .membershipId(request.getMembershipId())
                .build();

        return loginMembershipUseCase.LoginMembership(command);
    }

    @PostMapping(path="/membership/refresh-token")
    JWtToken refreshToken(@RequestBody RefreshTokenRequest request){

        RefreshTokenCommand command = RefreshTokenCommand.builder()
                .refreshToken(request.getRefreshToken())
                .build();

        return loginMembershipUseCase.refreshJwtTokenByRefreshToken(command);
    }

    @PostMapping(path="/membership/token-validate")
    boolean validateToken(@RequestBody ValidateTokenRequest request){

        ValidateTokenCommand command = ValidateTokenCommand.builder()
                .jwtToken(request.getJwtToken())
                .build();

        return loginMembershipUseCase.validateJwtToken(command);
    }

    @PostMapping(path="/membership/token-membership")
    Membership getMembershipByJwtToken(@RequestBody ValidateTokenRequest request){

        ValidateTokenCommand command = ValidateTokenCommand.builder()
                .jwtToken(request.getJwtToken())
                .build();

        return loginMembershipUseCase.getMembershipByJwtToken(command);
    }
}

