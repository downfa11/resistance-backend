package com.ns.membership.application.service;


import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.adapter.out.persistance.MembershipMapper;
import com.ns.membership.application.port.in.LoginMembershipUseCase;
import com.ns.membership.application.port.in.command.LoginMembershipCommand;
import com.ns.membership.application.port.in.command.RefreshTokenCommand;
import com.ns.membership.application.port.in.command.ValidateTokenCommand;
import com.ns.membership.application.port.out.AuthMembershipPort;
import com.ns.membership.application.port.out.FindMembershipPort;
import com.ns.membership.application.port.out.ModifyMembershipPort;
import com.ns.membership.domain.JWtToken;
import com.ns.membership.domain.Membership;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class AuthService implements LoginMembershipUseCase {

    private final AuthMembershipPort authMembershipPort;
    private final FindMembershipPort findMembershipPort;
    private final ModifyMembershipPort modifyMembershipPort;

    private final MembershipMapper mapper;

    @Override
    public JWtToken LoginMembership(LoginMembershipCommand command) {
        String account = command.getAccount();

        MembershipJpaEntity membershipJpaEntity = findMembershipPort.findMembershipByAccountAndPassword(
                new Membership.MembershipAccount(account),
                new Membership.MembershipPassword(command.getPassword())).get();


        if(membershipJpaEntity.isValid()) {
            String membershipId = String.valueOf(membershipJpaEntity.getMembershipId());
            Membership.MembershipId memberId = new Membership.MembershipId(membershipId);
            Membership.MembershipRole memberRole = new Membership.MembershipRole(membershipJpaEntity.getRole());

            String jwtToken = authMembershipPort.generateJwtToken(memberId, memberRole);
            String refreshToken = authMembershipPort.generateRefreshToken(memberId);
            updateRefreshTokenMembership(membershipJpaEntity, membershipId, refreshToken);

            return JWtToken.generateJwtToken(
                    new JWtToken.MembershipId(membershipId),
                    new JWtToken.MembershipJwtToken(jwtToken),
                    new JWtToken.MembershipRefreshToken(refreshToken)
            );
        }
        return null;
    }

    private void updateRefreshTokenMembership(MembershipJpaEntity membershipJpaEntity, String membershipId, String refreshToken){
        modifyMembershipPort.modifyMembership(
                new Membership.MembershipId(membershipId),
                new Membership.MembershipName(membershipJpaEntity.getName()),
                new Membership.MembershipAccount(membershipJpaEntity.getAccount()),
                new Membership.MembershipPassword(membershipJpaEntity.getPassword()),
                new Membership.MembershipAddress(membershipJpaEntity.getAddress()),
                new Membership.MembershipEmail(membershipJpaEntity.getEmail()),
                new Membership.MembershipIsValid(membershipJpaEntity.isValid()),
                new Membership.Friends(membershipJpaEntity.getFriends()),
                new Membership.WantedFriends(membershipJpaEntity.getWantedFriends()),
                new Membership.RefreshToken(refreshToken),
                new Membership.MembershipRole(membershipJpaEntity.getRole()),
                new Membership.MembershipProvider(membershipJpaEntity.getProvider()),
                new Membership.MembershipProviderId(membershipJpaEntity.getProviderId())
        );
    }

    @Override
    public JWtToken refreshJwtTokenByRefreshToken(RefreshTokenCommand command) {
        String RequestedRefreshToken = command.getRefreshToken();
        boolean isValid = authMembershipPort.validateJwtToken(RequestedRefreshToken);

        if(isValid){
            Membership.MembershipId membershipId = authMembershipPort.parseMembershipIdFromToken(RequestedRefreshToken);
            String membershipIdString = membershipId.getMembershipId();

            MembershipJpaEntity membershipJpaEntity = findMembershipPort.findMembership(membershipId).get();
            if(!membershipJpaEntity.getRefreshToken().equals(command.getRefreshToken()))
                return null;

            if(membershipJpaEntity.isValid()){
                String newJwtToken = authMembershipPort.generateJwtToken(
                        new Membership.MembershipId(membershipIdString),
                        new Membership.MembershipRole(membershipJpaEntity.getRole())
                );

                return JWtToken.generateJwtToken(
                        new JWtToken.MembershipId(membershipIdString),
                        new JWtToken.MembershipJwtToken(newJwtToken),
                        new JWtToken.MembershipRefreshToken(RequestedRefreshToken)
                );
            }
        }
        log.info("membershipJpaEntity is not valid");
        return null;
    }

    @Override
    public boolean validateJwtToken(ValidateTokenCommand command) {

        String token = command.getJwtToken();
        return authMembershipPort.validateJwtToken(token);
    }

    @Override
    public Membership getMembershipByJwtToken(ValidateTokenCommand command) {
        String jwtToken = command.getJwtToken();
        boolean isValid = authMembershipPort.validateJwtToken(jwtToken);

        if (isValid) {
            Membership.MembershipId membershipId = authMembershipPort.parseMembershipIdFromToken(jwtToken);

            MembershipJpaEntity membershipJpaEntity = findMembershipPort.findMembership(membershipId).get();
            if (!membershipJpaEntity.getRefreshToken().equals(command.getJwtToken())) return null;

            return mapper.mapToDomainEntity(membershipJpaEntity);
        }
        return null;
    }

}

