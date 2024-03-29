package com.ns.membership.adapter.out.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<MembershipJpaEntity,Long> {

    @Query("SELECT m FROM MembershipJpaEntity m LEFT JOIN FETCH m.friends LEFT JOIN FETCH m.wantedFriends WHERE m.membershipId = :membershipId")
    Optional<MembershipJpaEntity> findById(@Param("membershipId") Long membershipId);

    @Query(value = "SELECT * FROM resistance.membership WHERE membership_id != ?1 ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<MembershipJpaEntity> getRandomAlly(String membershipId, int count);

}
