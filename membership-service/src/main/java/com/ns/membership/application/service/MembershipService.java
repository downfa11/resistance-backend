package com.ns.membership.application.service;

import com.ns.common.CountDownLatchManager;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.common.UseCase;
import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.adapter.out.persistance.MembershipMapper;
import com.ns.membership.adapter.out.vault.VaultAdapter;
import com.ns.membership.application.port.in.FindMembershipUseCase;
import com.ns.membership.application.port.in.ModifyMembershipUseCase;
import com.ns.membership.application.port.in.RegisterMembershipUseCase;
import com.ns.membership.application.port.in.command.FindMembershipCommand;
import com.ns.membership.application.port.in.command.ModifyMembershipCommand;
import com.ns.membership.application.port.in.command.RegisterMembershipCommand;
import com.ns.membership.application.port.out.FindMembershipPort;
import com.ns.membership.application.port.out.ModifyMembershipPort;
import com.ns.membership.application.port.out.RegisterMembershipPort;
import com.ns.membership.application.port.out.SendTaskPort;
import com.ns.membership.domain.Membership;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class MembershipService implements RegisterMembershipUseCase, ModifyMembershipUseCase, FindMembershipUseCase {

    private final RegisterMembershipPort registerMembershipPort;
    private final ModifyMembershipPort modifyMembershipPort;
    private final FindMembershipPort findMembershipPort;
    private final MembershipMapper membershipMapper;

    private final SendTaskPort sendTaskPort;
    private final CountDownLatchManager countDownLatchManager;
    private final VaultAdapter vaultAdapter;

    @Override
    @Transactional
    public Membership registerMembership(RegisterMembershipCommand command) {


        String encryptedPassword = vaultAdapter.encrypt(command.getPassword());

        // db는 외부 시스템이라 이용하기 위해선 port, adapter를 통해서 나갈 수 있다.
        MembershipJpaEntity memberByAccount = null;
        MembershipJpaEntity memberByEmail = null;

        try {
            memberByAccount = findMembershipPort.findMembershipByAccount(
                    new Membership.MembershipAccount(command.getAccount())
            );

            memberByEmail = findMembershipPort.findMembershipByEmail(
                    new Membership.MembershipEmail(command.getEmail())
            );
        } catch (EntityNotFoundException e) {
            log.info("EntityNotFoundException register membership.");
        }

        if(memberByAccount!=null || memberByEmail!=null){
            return null;
        }

        MembershipJpaEntity jpaEntity = registerMembershipPort.createMembership(
                new Membership.MembershipName(command.getName()),
                new Membership.MembershipAccount(command.getAccount()),
                new Membership.MembershipPassword(encryptedPassword),
                new Membership.MembershipAddress(command.getAddress()),
                new Membership.MembershipEmail(command.getEmail()),
                new Membership.MembershipIsValid(command.isValid()),
                new Membership.Friends(null),
                new Membership.WantedFriends(null),
                new Membership.RefreshToken(""),
                new Membership.MembershipRole("ROLE_USER")
        );


            List<SubTask> subTaskList = new ArrayList<>();
            SubTask subtask = SubTask.builder()
                        .subTaskName("GetMemberDataTask : " + "MembershipId validation, transfer UserData.")
                        .membersrhipId(jpaEntity.getMembershipId().toString())
                        .taskType("register")
                        .status("ready")
                        .data(jpaEntity.getName())
                        .build();
            subTaskList.add(subtask);

            Task task = Task.builder()
                    .taskID(UUID.randomUUID().toString())
                    .taskName("GetUserDatas Task")
                    .subTaskList(subTaskList)
                    .membershipId(jpaEntity.getMembershipId().toString())
                    .build();

            sendTaskPort.sendTaskPort(task);
            countDownLatchManager.addCountDownLatch(task.getTaskID());
            log.info("countDownLatchManager await "+task.getTaskID()+" id:"+task.getMembershipId());

            try {
                countDownLatchManager.getCountDownLatch(task.getTaskID()).await();
            }catch (InterruptedException e){
                throw new RuntimeException(e);
            }


            String result = countDownLatchManager.getDataForKey(task.getTaskID());
            log.info("test kafka IPC : "+result);

        // entity -> Membership 도메인으로 변환해야한다.
        return membershipMapper.mapToDomainEntity(jpaEntity);
    }

    @Override
    @Transactional
    @CacheEvict(value="userData", key="'userData'+':'+ #command.membershipId+':'+ #command.targetIdList",allEntries = true)
    public Membership modifyMembership(ModifyMembershipCommand command) {

        String encryptedPassword = vaultAdapter.encrypt(command.getPassword());

        // db는 외부 시스템이라 이용하기 위해선 port, adapter를 통해서 나갈 수 있다.
        MembershipJpaEntity jpaEntity = modifyMembershipPort.modifyMembership(
                new Membership.MembershipId(command.getMembershipId()),
                new Membership.MembershipName(command.getName()),
                new Membership.MembershipAccount(command.getAccount()),
                new Membership.MembershipPassword(encryptedPassword),
                new Membership.MembershipAddress(command.getAddress()),
                new Membership.MembershipEmail(command.getEmail()),
                new Membership.MembershipIsValid(command.isValid()),

                new Membership.Friends(command.getFriends()),
                new Membership.WantedFriends(command.getWantedFriends()),
                new Membership.RefreshToken(command.getRefreshToken()),
                new Membership.MembershipRole("ROLE_USER")
        );

        // entity -> Membership 도메인으로 변환해야한다.
        return membershipMapper.mapToDomainEntity(jpaEntity);
    }

    @Override
    @Transactional
    public Membership findMembership(FindMembershipCommand command) {
        MembershipJpaEntity entity = findMembershipPort.findMembership(new Membership.MembershipId(command.getMembershipId()));
        return membershipMapper.mapToDomainEntity(entity);
    }



}
