package com.ns.membership;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.adapter.out.persistance.MembershipRepository;
import java.util.Optional;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MembershipRepositoryImplTest {

    @Autowired
    private MembershipRepository membershipCustomRepository;

    @Test
    void BatchSize로_쿼리문_실행해도_연관관계를_잘_가져오는지_테스트() {
        Long membershipId = 1L;

        Optional<MembershipJpaEntity> membership = membershipCustomRepository.findByMembershipId(membershipId);

        assertTrue(membership.isPresent());
        assertNotNull(membership.get().getFriends());
        assertNotNull(membership.get().getWantedFriends());
    }
}
