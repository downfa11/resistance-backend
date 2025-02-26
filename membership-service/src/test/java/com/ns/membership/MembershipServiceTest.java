package com.ns.membership;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.CountDownLatchManager;
import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.adapter.out.persistance.MembershipMapper;
import com.ns.membership.application.port.in.command.FindMembershipCommand;
import com.ns.membership.application.port.in.command.ModifyMembershipCommand;
import com.ns.membership.application.port.in.command.RegisterMembershipCommand;
import com.ns.membership.application.port.out.FindMembershipPort;
import com.ns.membership.application.port.out.ModifyMembershipPort;
import com.ns.membership.application.port.out.RegisterMembershipPort;
import com.ns.membership.application.port.out.SendTaskPort;
import com.ns.membership.application.service.MembershipService;
import com.ns.membership.domain.Membership;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class MembershipServiceTest {

    @InjectMocks private MembershipService membershipService;
    @Mock private RegisterMembershipPort registerMembershipPort;
    @Mock private ModifyMembershipPort modifyMembershipPort;
    @Mock private FindMembershipPort findMembershipPort;
    @Mock private MembershipMapper membershipMapper;
    @Mock private CountDownLatchManager countDownLatchManager;

    @Mock private MembershipJpaEntity membershipJpaEntity;

    private Membership membership;

    @BeforeEach
    void init(){
        membership = new Membership("1", "name", "account", "password", "email", "address",true,null,null,"token","role","provider","providerId");
    }

    @Test
    void 사용자를_등록하는_메서드() {
        // given
        RegisterMembershipCommand command = RegisterMembershipCommand.builder()
                .name("name")
                .account("account")
                .password("password")
                .address("address")
                .email("email")
                .isValid(true)
                .build();
        MembershipJpaEntity mockJpaEntity = mock(MembershipJpaEntity.class);
        when(registerMembershipPort.createMembership(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockJpaEntity);

        when(countDownLatchManager.getCountDownLatch(anyString())).thenReturn(new java.util.concurrent.CountDownLatch(1));
        when(countDownLatchManager.getDataForKey(anyString())).thenReturn("result");
        when(membershipMapper.mapToDomainEntity(mockJpaEntity)).thenReturn(membership);

        // when
        Membership result = membershipService.registerMembership(command);

        // then
        assertNotNull(result);
        verify(registerMembershipPort, times(1)).createMembership(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 사용자의_정보를_수정하는_메서드() {
        // given
        ModifyMembershipCommand command = ModifyMembershipCommand.builder()
                .membershipId("1")
                .name("newName")
                .account("newAccount")
                .password("newPassword")
                .address("newAddress")
                .email("newEmail")
                .isValid(true)
                .friends(null)
                .wantedFriends(null)
                .build();

        MembershipJpaEntity mockEntity = mock(MembershipJpaEntity.class);
        when(findMembershipPort.findMembership(any())).thenReturn(Optional.of(mockEntity));

        MembershipJpaEntity modifiedEntity = mock(MembershipJpaEntity.class);
        when(modifyMembershipPort.modifyMembership(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(modifiedEntity);

        when(membershipMapper.mapToDomainEntity(modifiedEntity)).thenReturn(membership);

        // when
        Membership result = membershipService.modifyMembership(command);

        // then
        assertNotNull(result);
        verify(modifyMembershipPort, times(1)).modifyMembership(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 사용자를_조회하는_메서드() {
        // given
        FindMembershipCommand command = FindMembershipCommand.builder()
                .membershipId("1")
                .build();
        MembershipJpaEntity mockEntity = mock(MembershipJpaEntity.class);
        when(findMembershipPort.findMembership(any())).thenReturn(Optional.of(mockEntity));

        when(membershipMapper.mapToDomainEntity(mockEntity)).thenReturn(membership);

        // when
        Membership result = membershipService.findMembership(command);

        // then
        assertNotNull(result);
        verify(findMembershipPort, times(1)).findMembership(any());
    }

    @Test
    void 가입시_이메일과_계정이_중복되는_경우를_검증하는_메서드() {
        // given
        String email = "test@example.com";
        String account = "testAccount";
        when(findMembershipPort.findMembershipByAccount(any())).thenReturn(Optional.empty());
        when(findMembershipPort.findMembershipByEmail(any())).thenReturn(Optional.empty());

        // when
        boolean result = membershipService.validateEmailAndAccount(email, account);

        // then
        assertTrue(result);
        when(findMembershipPort.findMembershipByAccount(any())).thenReturn(Optional.of(mock(MembershipJpaEntity.class)));
        assertFalse(membershipService.validateEmailAndAccount(email, account));
    }
}
