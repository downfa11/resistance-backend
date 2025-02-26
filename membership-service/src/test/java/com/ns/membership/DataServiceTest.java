package com.ns.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.CountDownLatchManager;
import com.ns.membership.adapter.out.persistance.MembershipRepository;
import com.ns.membership.application.port.out.SendTaskPort;
import com.ns.membership.application.service.DataService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class DataServiceTest {

    @InjectMocks private DataService dataService;
    @Mock private SendTaskPort sendTaskPort;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CountDownLatchManager countDownLatchManager;
    @Mock private ObjectMapper objectMapper;

    @Test
    void 친구가_아닌_경우의_무작위_동료의_목록을_조회하는_메서드() {
        // given
        String membershipId = "membershipId";
        Set<Long> randomAllySet = new HashSet<>(Arrays.asList(1L, 2L, 3L));

        when(membershipRepository.getRandomAlly(membershipId, 5)).thenReturn(Collections.emptyList());

        // when
        Set<Long> result = dataService.getAllyRandom(membershipId);

        // then
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
