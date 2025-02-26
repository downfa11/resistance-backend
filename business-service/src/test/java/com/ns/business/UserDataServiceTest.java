package com.ns.business;

import com.ns.business.adpater.out.persistance.UserDataJpaEntity;
import com.ns.business.adpater.out.persistance.UserDataMapper;
import com.ns.business.application.port.in.command.FindUserDataCommand;
import com.ns.business.application.port.in.command.ModifyUserDataCommand;
import com.ns.business.application.port.in.command.RegisterUserDataCommand;
import com.ns.business.application.port.out.FindUserDataPort;
import com.ns.business.application.port.out.ModifyUserDataPort;
import com.ns.business.application.port.out.RegisterUserDataPort;
import com.ns.business.application.service.UserDataService;
import com.ns.business.domain.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserDataServiceTest {

    @InjectMocks private UserDataService userDataService;
    @Mock private RegisterUserDataPort registerUserDataPort;
    @Mock private ModifyUserDataPort modifyUserDataPort;
    @Mock private FindUserDataPort findUserDataPort;
    @Mock private UserDataMapper userDataMapper;
    @Mock private UserDataJpaEntity userDataJpaEntity;


    private UserData userData;

    @BeforeEach
    void init(){
        UserData.UserId userId = new UserData.UserId(123L);
        UserData.UserName userName = new UserData.UserName("user");
        UserData.UserGold userGold = new UserData.UserGold(1000);
        UserData.UserHighscore userHighscore = new UserData.UserHighscore(2000);
        UserData.UserEnergy userEnergy = new UserData.UserEnergy(50);
        UserData.UserScenario userScenario = new UserData.UserScenario(1);
        UserData.UserHead userHead = new UserData.UserHead("모자");
        UserData.UserBody userBody = new UserData.UserBody("전신 의류");
        UserData.UserArm userArm = new UserData.UserArm("무기류");
        UserData.UserHealth userHealth = new UserData.UserHealth(100);
        UserData.UserAttack userAttack = new UserData.UserAttack(150);
        UserData.UserCritical userCritical = new UserData.UserCritical(25);
        UserData.UserDurability userDurability = new UserData.UserDurability(80);

        userData = UserData.generateUserData(userId, userName, userGold, userHighscore, userEnergy, userScenario, userHead, userBody, userArm, userHealth, userAttack, userCritical, userDurability);
    }

    @Test
    void 사용자의_데이터를_최초_등록하는_메서드() {
        // given
        RegisterUserDataCommand command =  RegisterUserDataCommand.builder()
                .userId(123L)
                .gold(1000)
                .highscore(2000)
                .energy(50)
                .scenario(1)
                .head("모자")
                .body("전신 의류")
                .arm("무기류")
                .health(100)
                .attack(150)
                .critical(25)
                .durability(80)
                .build();

        UserDataJpaEntity jpaEntity = mock(UserDataJpaEntity.class);
        when(registerUserDataPort.createUserData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(jpaEntity);
        when(userDataMapper.mapToDomainEntity(jpaEntity)).thenReturn(userData);

        // when
        UserData result = userDataService.registerUserData(command);

        // then
        assertNotNull(result);
        assertEquals(123L, result.getUserId());
        assertEquals("user", result.getName());
        assertEquals(1000, result.getGold());
        verify(registerUserDataPort).createUserData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(userDataMapper).mapToDomainEntity(jpaEntity);
    }

    @Test
    void 사용자의_데이터를_업데이트하는_메서드() {
        // given
        ModifyUserDataCommand command = ModifyUserDataCommand.builder()
                .userId(123L)
                .gold(1000)
                .highscore(2000)
                .energy(50)
                .scenario(1)
                .head("모자")
                .body("전신 의류")
                .arm("무기류")
                .health(100)
                .attack(150)
                .critical(25)
                .durability(80)
                .build();

        UserDataJpaEntity jpaEntity = mock(UserDataJpaEntity.class);
        when(modifyUserDataPort.modifyUserData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(jpaEntity);
        when(userDataMapper.mapToDomainEntity(jpaEntity)).thenReturn(userData);

        // when
        UserData result = userDataService.modifyUserData(command);

        // then
        assertNotNull(result);
        assertEquals(123L, result.getUserId());
        assertEquals(1000, result.getGold());
        verify(modifyUserDataPort).modifyUserData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(userDataMapper).mapToDomainEntity(jpaEntity);
    }

    @Test
    void 사용자의_데이터를_조회하는_메서드() {
        // given
        FindUserDataCommand command = FindUserDataCommand.builder()
                .userId(1L)
                .build();

        UserDataJpaEntity jpaEntity = mock(UserDataJpaEntity.class);
        when(findUserDataPort.findUserData(any())).thenReturn(jpaEntity);
        when(userDataMapper.mapToDomainEntity(jpaEntity)).thenReturn(userData);

        // when
        UserData result = userDataService.findUserData(command);

        // then
        assertNotNull(result);
        assertEquals(123L, result.getUserId());
        verify(findUserDataPort).findUserData(any());
        verify(userDataMapper).mapToDomainEntity(jpaEntity);
    }
}
