package com.ns.dedicated.application.port.out;


import com.ns.dedicated.adpater.out.persistance.BoardJpaEntity;
import com.ns.dedicated.domain.Board;

public interface RegisterBoardPort {

    BoardJpaEntity createBoard(
            Board.BoardTitle boardTitle,
            Board.BoardContents boardContents
    );
}
