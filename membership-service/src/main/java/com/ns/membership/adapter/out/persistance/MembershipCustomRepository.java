package com.ns.membership.adapter.out.persistance;

import java.util.List;
import java.util.Optional;

public interface MembershipCustomRepository {
    Optional<MembershipJpaEntity> findByMembershipId(Long membershipId);
}
