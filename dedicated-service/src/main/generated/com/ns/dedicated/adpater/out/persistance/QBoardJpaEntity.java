package com.ns.dedicated.adpater.out.persistance;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBoardJpaEntity is a Querydsl query type for BoardJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoardJpaEntity extends EntityPathBase<BoardJpaEntity> {

    private static final long serialVersionUID = 313720831L;

    public static final QBoardJpaEntity boardJpaEntity = new QBoardJpaEntity("boardJpaEntity");

    public final NumberPath<Long> boardId = createNumber("boardId", Long.class);

    public final StringPath contents = createString("contents");

    public final DateTimePath<java.sql.Timestamp> createdAt = createDateTime("createdAt", java.sql.Timestamp.class);

    public final NumberPath<Long> hits = createNumber("hits", Long.class);

    public final StringPath title = createString("title");

    public final DateTimePath<java.sql.Timestamp> updatedAt = createDateTime("updatedAt", java.sql.Timestamp.class);

    public QBoardJpaEntity(String variable) {
        super(BoardJpaEntity.class, forVariable(variable));
    }

    public QBoardJpaEntity(Path<? extends BoardJpaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBoardJpaEntity(PathMetadata metadata) {
        super(BoardJpaEntity.class, metadata);
    }

}

