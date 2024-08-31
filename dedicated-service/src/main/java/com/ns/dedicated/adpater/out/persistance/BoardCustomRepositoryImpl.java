package com.ns.dedicated.adpater.out.persistance;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BoardCustomRepositoryImpl implements BoardCustomRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QBoardJpaEntity board;

    public BoardCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
        this.board=QBoardJpaEntity.boardJpaEntity;
    }

    @Override
    public Optional<BoardJpaEntity> findLatestBoard() {
        QBoardJpaEntity board = QBoardJpaEntity.boardJpaEntity;

        BoardJpaEntity result = jpaQueryFactory
                .selectFrom(board)
                .orderBy(board.createdAt.desc())
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
