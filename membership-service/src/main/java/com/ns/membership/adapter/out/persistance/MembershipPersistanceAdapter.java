package com.ns.membership.adapter.out.persistance;

import com.ns.common.PersistanceAdapter;
import com.ns.membership.application.port.out.FindMembershipPort;
import com.ns.membership.application.port.out.ModifyMembershipPort;
import com.ns.membership.application.port.out.RegisterMembershipPort;
import com.ns.membership.domain.Membership;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@PersistanceAdapter
@Slf4j
public class MembershipPersistanceAdapter implements RegisterMembershipPort, FindMembershipPort, ModifyMembershipPort {

    private final MembershipRepository membershipRepository;
    //private final VaultAdapter vaultAdapter;

    @Override
    public MembershipJpaEntity createMembership(Membership.MembershipName membershipName, Membership.MembershipAccount membershipAccount, Membership.MembershipPassword membershipPassword, Membership.MembershipAddress membershipAddress, Membership.MembershipEmail membershipEmail, Membership.MembershipIsValid membershipIsValid, Membership.Friends friends, Membership.WantedFriends wantedFriends, Membership.RefreshToken refreshToken, Membership.MembershipRole membershipRole, Membership.MembershipProvider membershipProvider, Membership.MembershipProviderId membershipProviderId) {

        //String encryptedPassword = vaultAdapter.encrypt(membershipPassword.getPassword());
        MembershipJpaEntity jpaEntity = new MembershipJpaEntity(
                membershipName.getNameValue(),
                membershipAccount.getAccountValue(),
                membershipPassword.getPasswordValue(),
                membershipAddress.getAddressValue(),
                membershipEmail.getEmailValue(),
                membershipIsValid.isValidValue(),
                friends.getFriends(),wantedFriends.getWantedfriends(),
                "",
                membershipRole.getMembershipRole(),
                membershipProvider.getMembershipProvider(),
                membershipProviderId.getMembershipProviderId()

        );
        membershipRepository.save(jpaEntity);
        log.info("entity encrypted password : "+jpaEntity.getPassword());
        return new MembershipJpaEntity(
                jpaEntity.getMembershipId(),
                membershipName.getNameValue(),
                membershipAccount.getAccountValue(),
                membershipPassword.getPasswordValue(),
                membershipAddress.getAddressValue(),
                membershipEmail.getEmailValue(),
                membershipIsValid.isValidValue(),
                friends.getFriends(),wantedFriends.getWantedfriends(),
                "",
                membershipRole.getMembershipRole(),
                membershipProvider.getMembershipProvider(),
                membershipProviderId.getMembershipProviderId()
        );
    }

    @Override
    public MembershipJpaEntity findMembership(Membership.MembershipId membershipId) {
        Long id = Long.parseLong(membershipId.getMembershipId());
        MembershipJpaEntity membershipJpaEntity = membershipRepository.findByMembershipId(id)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for id: " + id));
        String encryptedEmail = membershipJpaEntity.getEmail();
        //String decrptedEmail = vaultAdapter.decrypt(encryptedEmail);
        membershipJpaEntity.setEmail(encryptedEmail);
        return membershipJpaEntity;
    }

    @Override
    public MembershipJpaEntity findMembershipByAccountOrEmail(Membership.MembershipAccount account, Membership.MembershipEmail email) {

        String accountValue= account.getAccountValue();
        String emailValue= email.getEmailValue();
        MembershipJpaEntity membershipJpaEntity = membershipRepository.findByAccountOrEmail(accountValue,emailValue)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for " + accountValue+", "+emailValue));
        return membershipJpaEntity;
    }

    @Override
    public MembershipJpaEntity findMembershipByAccountAndPassword(Membership.MembershipAccount account, Membership.MembershipPassword password) {

        String accountValue= account.getAccountValue();
        String passwordValue= password.getPasswordValue();
        MembershipJpaEntity membershipJpaEntity = membershipRepository.findByAccountAndPassword(accountValue,passwordValue)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for " + accountValue+", "+passwordValue));
        return membershipJpaEntity;
    }

    @Override
    public MembershipJpaEntity findMembershipByEmail(Membership.MembershipEmail email) {

        String emailValue= email.getEmailValue();
        MembershipJpaEntity membershipJpaEntity = membershipRepository.findByEmail(emailValue)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for "+emailValue));
        return membershipJpaEntity;
    }

    @Override
    public MembershipJpaEntity findMembershipByAccount(Membership.MembershipAccount account) {

        String accountValue= account.getAccountValue();
        MembershipJpaEntity membershipJpaEntity = membershipRepository.findByAccount(accountValue)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for " + accountValue));
        return membershipJpaEntity;
    }

    @Override
    public MembershipJpaEntity findMembershipByName(Membership.MembershipName name) {
        String nameValue= name.getNameValue();
        MembershipJpaEntity membershipJpaEntity = membershipRepository.findByName(nameValue)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for " + nameValue));
        return membershipJpaEntity;
    }


    @Override
    public MembershipJpaEntity modifyMembership(Membership.MembershipId membershipId, Membership.MembershipName membershipName,Membership.MembershipAccount membershipAccount, Membership.MembershipPassword membershipPassword, Membership.MembershipAddress membershipAddress, Membership.MembershipEmail membershipEmail, Membership.MembershipIsValid membershipIsValid, Membership.Friends friends, Membership.WantedFriends wantedFriends, Membership.RefreshToken refreshToken, Membership.MembershipRole membershipRole, Membership.MembershipProvider membershipProvider, Membership.MembershipProviderId membershipProviderId) {
        Long id = Long.parseLong(membershipId.getMembershipId());
        MembershipJpaEntity entity = membershipRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for id: " + id));
        //String encryptedEmail = vaultAdapter.encrypt(membershipEmail.getEmailValue());

        entity.setName(membershipName.getNameValue());
        entity.setAddress(membershipAddress.getAddressValue());
        entity.setEmail(membershipEmail.getEmailValue());
        entity.setValid(membershipIsValid.isValidValue());

        entity.setFriends(friends.getFriends());
        entity.setWantedFriends(wantedFriends.getWantedfriends());
        entity.setRefreshToken(refreshToken.getRefreshToken());
        entity.setRole(membershipRole.getMembershipRole());
        membershipRepository.save(entity);

        return new MembershipJpaEntity(
                membershipName.getNameValue(),
                membershipAccount.getAccountValue(),
                membershipPassword.getPasswordValue(),
                membershipAddress.getAddressValue(),
                membershipEmail.getEmailValue(),
                membershipIsValid.isValidValue(),
                friends.getFriends(),
                wantedFriends.getWantedfriends(),
                refreshToken.getRefreshToken(),
                membershipRole.getMembershipRole(),
                membershipProvider.getMembershipProvider(),
                membershipProviderId.getMembershipProviderId()
        );
    }

}

