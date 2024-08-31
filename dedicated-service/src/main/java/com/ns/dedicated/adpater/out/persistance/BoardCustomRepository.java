package com.ns.dedicated.adpater.out.persistance;


import java.util.Optional;

public interface BoardCustomRepository {
    Optional<BoardJpaEntity> findLatestBoard();
}
