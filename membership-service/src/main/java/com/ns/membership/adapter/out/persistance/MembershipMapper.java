package com.ns.membership.adapter.out.persistance;


import com.ns.membership.domain.Membership;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MembershipMapper {

    public Membership mapToDomainEntity(MembershipJpaEntity membershipJpaEntity){
        return Membership.generateMember(
                new Membership.MembershipId(membershipJpaEntity.getMembershipId()+""),
                new Membership.MembershipName(membershipJpaEntity.getName()),
                new Membership.MembershipAddress(membershipJpaEntity.getAddress()),
                new Membership.MembershipEmail(membershipJpaEntity.getEmail()),
                new Membership.MembershipIsValid(membershipJpaEntity.isValid()),
                new Membership.Friends(membershipJpaEntity.getFriends()),
                new Membership.WantedFriends(membershipJpaEntity.getWantedFriends()),
                new Membership.RefreshToken(membershipJpaEntity.getRefreshToken())
        );
    }

}

