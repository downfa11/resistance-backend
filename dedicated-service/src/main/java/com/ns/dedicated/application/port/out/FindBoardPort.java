package com.ns.dedicated.application.port.out;


import com.ns.dedicated.adpater.out.persistance.BoardJpaEntity;
import com.ns.dedicated.domain.Board;

import java.util.List;

public interface FindBoardPort {

    BoardJpaEntity findBoard(
            Board.BoardId boardId
    );

    List<BoardJpaEntity> findBoardsAll(
            Long offset
    );

    BoardJpaEntity findTopByOrderByCreatedAtDesc();
}
