package com.ns.dedicated.adpater.out.persistance;

import com.ns.dedicated.adpater.out.persistance.BoardJpaEntity;
import com.ns.dedicated.domain.Board;
import org.springframework.stereotype.Component;

@Component
public class BoardMapper {
    public Board mapToDomainEntity(BoardJpaEntity boardJpaEntity) {
        return Board.generateBoard(
                new Board.BoardId(boardJpaEntity.getBoardId()),
                new Board.BoardTitle(boardJpaEntity.getTitle()),
                new Board.BoardContents(boardJpaEntity.getContents()),
                new Board.BoardHits(boardJpaEntity.getHits()),
                new Board.BoardCreatedAt(boardJpaEntity.getCreatedAt()),
                new Board.BoardUpdatedAt(boardJpaEntity.getUpdatedAt())
        );
    }
}
