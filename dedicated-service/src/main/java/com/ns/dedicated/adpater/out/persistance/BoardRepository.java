package com.ns.dedicated.adpater.out.persistance;


import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<BoardJpaEntity,Long>, BoardCustomRepository {
}
