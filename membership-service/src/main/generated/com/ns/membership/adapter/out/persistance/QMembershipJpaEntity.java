package com.ns.membership.adapter.out.persistance;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMembershipJpaEntity is a Querydsl query type for MembershipJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMembershipJpaEntity extends EntityPathBase<MembershipJpaEntity> {

    private static final long serialVersionUID = -1097647294L;

    public static final QMembershipJpaEntity membershipJpaEntity = new QMembershipJpaEntity("membershipJpaEntity");

    public final StringPath account = createString("account");

    public final StringPath address = createString("address");

    public final StringPath email = createString("email");

    public final SetPath<Long, NumberPath<Long>> friends = this.<Long, NumberPath<Long>>createSet("friends", Long.class, NumberPath.class, PathInits.DIRECT2);

    public final BooleanPath isValid = createBoolean("isValid");

    public final NumberPath<Long> membershipId = createNumber("membershipId", Long.class);

    public final StringPath name = createString("name");

    public final StringPath password = createString("password");

    public final StringPath refreshToken = createString("refreshToken");

    public final StringPath role = createString("role");

    public final SetPath<Long, NumberPath<Long>> wantedFriends = this.<Long, NumberPath<Long>>createSet("wantedFriends", Long.class, NumberPath.class, PathInits.DIRECT2);

    public QMembershipJpaEntity(String variable) {
        super(MembershipJpaEntity.class, forVariable(variable));
    }

    public QMembershipJpaEntity(Path<? extends MembershipJpaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMembershipJpaEntity(PathMetadata metadata) {
        super(MembershipJpaEntity.class, metadata);
    }

}

