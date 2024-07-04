package com.ns.business.adpater.out.persistance;


import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDataRepository extends JpaRepository<UserDataJpaEntity,Long> {

    UserDataJpaEntity findByUserId(Long userId);
}
