package com.ns.membership.adapter.out.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MembershipRepository extends JpaRepository<MembershipJpaEntity,Long> {


    @Query(value = "SELECT * FROM resistance.membership WHERE membership_id != ?1 ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<MembershipJpaEntity> getRandomAlly(String membershipId, int count);

}
