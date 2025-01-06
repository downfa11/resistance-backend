package com.ns.membership.adapter.in.web;


import com.ns.common.WebAdapter;
import com.ns.membership.adapter.in.web.dto.LoginMembershipRequest;
import com.ns.membership.adapter.in.web.dto.PasswordResetRequest;
import com.ns.membership.adapter.in.web.dto.RefreshTokenRequest;
import com.ns.membership.adapter.in.web.dto.ValidateTokenRequest;
import com.ns.membership.adapter.out.JwtTokenProvider;
import com.ns.membership.application.port.in.FindMembershipUseCase;
import com.ns.membership.application.port.in.LoginMembershipUseCase;
import com.ns.membership.application.port.in.MailSendUseCase;
import com.ns.membership.application.port.in.ModifyMembershipUseCase;
import com.ns.membership.application.port.in.command.*;
import com.ns.membership.domain.JWtToken;
import com.ns.membership.domain.Membership;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.AuthenticationException;

@WebAdapter
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthMembershipController {

    private final LoginMembershipUseCase loginMembershipUseCase;
    private final MailSendUseCase mailSendUseCase;
    private final ModifyMembershipUseCase modifyMembershipUseCase;
    private final FindMembershipUseCase findMembershipUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @PostMapping(path="/membership/login")
    JWtToken loginMembership(@RequestBody LoginMembershipRequest request){

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getAccount(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            LoginMembershipCommand command = LoginMembershipCommand.builder()
                    .account(request.getAccount())
                    .password(request.getPassword())
                    .build();

            return loginMembershipUseCase.LoginMembership(command);
        } catch (InternalAuthenticationServiceException e) {
            log.error("Authentication exception: {}", e.getMessage(), e);
            throw e;
        }
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

    @PostMapping("/membership/register/email-send")
    public ResponseEntity<String> sendRegistrationVerificationEmail(HttpSession httpSession, @RequestParam String email) {
        try {
            String code = mailSendUseCase.sendRegisterMessage(email);
            httpSession.setAttribute("registerCode", code);
            return ResponseEntity.ok("회원가입을 위한 이메일 인증 메일이 전송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("이메일 전송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/membership/password-reset/email-send")
    public ResponseEntity<String> sendPasswordResetVerificationEmail(HttpSession httpSession) {
        String membershipId = String.valueOf(jwtTokenProvider.getMembershipIdbyToken());

        FindMembershipCommand findCmmand = FindMembershipCommand.builder()
                .membershipId(membershipId)
                .build();

        Membership membership = findMembershipUseCase.findMembership(findCmmand);

        if(membership==null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당하는 membershipId를 찾을 수 없습니다.");

        String email = membership.getEmail();

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 인증 실패.");
        }

        try {
            String code = mailSendUseCase.sendPasswordResetMessage(email);
            httpSession.setAttribute("passwordResetCode", code);
            return ResponseEntity.ok("비밀번호 변경을 위한 이메일 인증 메일이 전송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("이메일 전송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/membership/password-reset")
    public ResponseEntity<String> resetPassword(HttpSession httpSession,
                                                @RequestBody PasswordResetRequest request) {
        String sessionCode = (String) httpSession.getAttribute("passwordResetCode");

        String membershipId = String.valueOf(jwtTokenProvider.getMembershipIdbyToken());

        if (sessionCode == null || !sessionCode.equals(request.getVerify()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 코드가 유효하지 않거나 일치하지 않습니다.");


        if(membershipId != request.getMembershipId())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("membershipId가 유효하지 않습니다.");

        FindMembershipCommand findCmmand = FindMembershipCommand.builder()
                .membershipId(membershipId)
                .build();

        Membership membership = findMembershipUseCase.findMembership(findCmmand);

        if(membership==null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당하는 membershipId를 찾을 수 없습니다.");


        ModifyMembershipCommand modifyCommand = ModifyMembershipCommand.builder()
                .membershipId(membership.getMembershipId())
                .name(membership.getName())
                .account(membership.getAccount())
                .address(membership.getAddress())
                .password(request.getNewPassword())
                .email(membership.getEmail())
                .isValid(membership.isValid())
                .build();

        modifyMembershipUseCase.modifyMembership(modifyCommand);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

}

