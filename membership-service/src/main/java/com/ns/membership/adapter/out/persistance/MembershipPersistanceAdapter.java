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
    public MembershipJpaEntity createMembership(Membership.MembershipName membershipName, Membership.MembershipAddress membershipAddress, Membership.MembershipEmail membershipEmail, Membership.MembershipIsValid membershipIsValid, Membership.Friends friends, Membership.WantedFriends wantedFriends, Membership.RefreshToken refreshToken) {

        //String encryptedEmail = vaultAdapter.encrypt(membershipEmail.getEmailValue());
        MembershipJpaEntity jpaEntity = new MembershipJpaEntity(
                membershipName.getNameValue(),
                membershipAddress.getAddressValue(),
                membershipEmail.getEmailValue(),
                membershipIsValid.isValidValue(),
                friends.getFriends(),wantedFriends.getWantedfriends(),""

        );
        membershipRepository.save(jpaEntity);
        log.info("entity encrypted Email : "+jpaEntity.getEmail());
        return new MembershipJpaEntity(
                jpaEntity.getMembershipId(),
                membershipName.getNameValue(),
                membershipAddress.getAddressValue(),
                membershipEmail.getEmailValue(),
                membershipIsValid.isValidValue(),
                friends.getFriends(),wantedFriends.getWantedfriends(),""
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
    public MembershipJpaEntity findMembershipByEmailAndAddress(Membership.MembershipAddress address, Membership.MembershipEmail email) {

        String addressValue= address.getAddressValue();
        String emailValue= email.getEmailValue();
        MembershipJpaEntity membershipJpaEntity = membershipRepository.findByAddressAndEmail(addressValue,emailValue)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for " + addressValue+", "+emailValue));
        String encryptedEmail = membershipJpaEntity.getEmail();
        //String decrptedEmail = vaultAdapter.decrypt(encryptedEmail);
        //membershipJpaEntity.setEmail(encryptedEmail);
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
    public MembershipJpaEntity findMembershipByAddress(Membership.MembershipAddress address) {

        String addressValue= address.getAddressValue();
        MembershipJpaEntity membershipJpaEntity = membershipRepository.findByAddress(addressValue)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for " + addressValue));
        return membershipJpaEntity;
    }


    @Override
    public MembershipJpaEntity modifyMembership(Membership.MembershipId membershipId, Membership.MembershipName membershipName, Membership.MembershipAddress membershipAddress, Membership.MembershipEmail membershipEmail, Membership.MembershipIsValid membershipIsValid, Membership.Friends friends, Membership.WantedFriends wantedFriends, Membership.RefreshToken refreshToken) {
        Long id = Long.parseLong(membershipId.getMembershipId());
        MembershipJpaEntity entity = membershipRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found for id: " + id));
        //String encryptedEmail = vaultAdapter.encrypt(membershipEmail.getEmailValue());

        entity.setName(membershipName.getNameValue());
        entity.setAddress(membershipAddress.getAddressValue());
        entity.setEmail(membershipEmail.getEmailValue());
        entity.setValid(membershipIsValid.isValidValue());

        //Todo. 여기서 IPC 통신으로 BUSINESS에서 받아온 데이터들을 토대로 userData를 만들어와야한다.

        entity.setFriends(friends.getFriends());
        entity.setWantedFriends(wantedFriends.getWantedfriends());
        entity.setRefreshToken(refreshToken.getRefreshToken());
        membershipRepository.save(entity);

        return new MembershipJpaEntity(
                membershipName.getNameValue(),
                membershipAddress.getAddressValue(),
                membershipEmail.getEmailValue(),
                membershipIsValid.isValidValue(),
                friends.getFriends(),
                wantedFriends.getWantedfriends(),
                refreshToken.getRefreshToken()
        );
    }

}

