package com.ns.membership.adapter.out.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


public interface MembershipRepository extends JpaRepository<MembershipJpaEntity,Long>,MembershipCustomRepository {

    @Query(value = "SELECT * FROM resistance.membership WHERE membership_id != ?1 AND membership_id >= (SELECT FLOOR(RAND() * (SELECT MAX(membership_id) FROM resistance.membership))) LIMIT ?2", nativeQuery = true)
    List<MembershipJpaEntity> getRandomAlly(String membershipId, int count);

    Optional<MembershipJpaEntity> findByAccountOrEmail(String account, String email);
    Optional<MembershipJpaEntity> findByAccountAndPassword(String account, String password);
    Optional<MembershipJpaEntity> findByAddress(String address);
    Optional<MembershipJpaEntity> findByEmail(String email);
    Optional<MembershipJpaEntity> findByAccount(String account);
    Optional<MembershipJpaEntity> findByName(String name);

}
