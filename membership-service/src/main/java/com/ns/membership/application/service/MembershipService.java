package com.ns.membership.application.service;

import com.ns.common.CountDownLatchManager;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.common.UseCase;
import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.adapter.out.persistance.MembershipMapper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public Membership registerMembership(RegisterMembershipCommand command) {
        if(validateEmailAndAccount(command.getEmail(), command.getAccount()))
            return null;

        MembershipJpaEntity jpaEntity = registerMembershipJpaEntity(command);

        String membershipId = String.valueOf(jpaEntity.getMembershipId());
        List<SubTask> subTaskList = createSubTaskListGetMembershipData(membershipId, jpaEntity.getName());
        Task task = createTaskGetMembershipData(membershipId, subTaskList);

        sendTaskPort.sendTaskPort(task);


        countDownLatchManager.addCountDownLatch(task.getTaskID());

        try {
            countDownLatchManager.getCountDownLatch(task.getTaskID()).await();
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }

        countDownLatchManager.getDataForKey(task.getTaskID());
        return membershipMapper.mapToDomainEntity(jpaEntity);
    }

    private boolean validateEmailAndAccount(String email, String account){
        Optional<MembershipJpaEntity> memberByAccount = findMembershipPort.findMembershipByAccount(new Membership.MembershipAccount(account));
        Optional<MembershipJpaEntity> memberByEmail = findMembershipPort.findMembershipByEmail(new Membership.MembershipEmail(email));

        if (memberByAccount.isPresent() || memberByEmail.isPresent()) {
            return false;
        }

        return true;
    }

    private Task createTaskGetMembershipData(String membershipId, List<SubTask> subTasks){
        return Task.builder()
                .taskID(UUID.randomUUID().toString())
                .taskName("GetUserDatas Task")
                .subTaskList(subTasks)
                .membershipId(membershipId)
                .build();
    }

    private List<SubTask> createSubTaskListGetMembershipData(String membershipId, String name){
        List<SubTask> subTaskList = new ArrayList<>();
        SubTask subtask = createSubTaskGetMembershipData(String.valueOf(membershipId), name);
        subTaskList.add(subtask);

        return subTaskList;
    }

    private SubTask createSubTaskGetMembershipData(String membershipId, String name){
        return SubTask.builder()
                .subTaskName("GetMemberDataTask : " + "MembershipId validation, transfer UserData.")
                .membersrhipId(membershipId)
                .taskType("register")
                .status("ready")
                .data(name)
                .build();
    }

    private MembershipJpaEntity registerMembershipJpaEntity(RegisterMembershipCommand command){
        return registerMembershipPort.createMembership(
                new Membership.MembershipName(command.getName()),
                new Membership.MembershipAccount(command.getAccount()),
                new Membership.MembershipPassword(command.getPassword()),
                new Membership.MembershipAddress(command.getAddress()),
                new Membership.MembershipEmail(command.getEmail()),
                new Membership.MembershipIsValid(command.isValid()),
                new Membership.Friends(null),
                new Membership.WantedFriends(null),
                new Membership.RefreshToken(""),
                new Membership.MembershipRole("ROLE_USER"),
                new Membership.MembershipProvider("default"),
                new Membership.MembershipProviderId(null)
        );
    }

    @Override
    @Transactional
    @CacheEvict(value="userData", key="'userData'+':'+ #command.membershipId+':'+ #command.targetIdList",allEntries = true)
    public Membership modifyMembership(ModifyMembershipCommand command) {
        MembershipJpaEntity entity = findMembershipPort.findMembership(new Membership.MembershipId(command.getMembershipId())).get();
        MembershipJpaEntity jpaEntity = modifyMembershipJpaEntity(command, entity);
        return membershipMapper.mapToDomainEntity(jpaEntity);
    }

    private MembershipJpaEntity modifyMembershipJpaEntity(ModifyMembershipCommand command, MembershipJpaEntity entity){
        return modifyMembershipPort.modifyMembership(
                new Membership.MembershipId(command.getMembershipId()),
                new Membership.MembershipName(command.getName()),
                new Membership.MembershipAccount(command.getAccount()),
                new Membership.MembershipPassword(command.getPassword()),
                new Membership.MembershipAddress(command.getAddress()),
                new Membership.MembershipEmail(command.getEmail()),
                new Membership.MembershipIsValid(command.isValid()),
                new Membership.Friends(command.getFriends()),
                new Membership.WantedFriends(command.getWantedFriends()),
                new Membership.RefreshToken(entity.getRefreshToken()),
                new Membership.MembershipRole(entity.getRole()),
                new Membership.MembershipProvider(entity.getProvider()),
                new Membership.MembershipProviderId(entity.getProviderId())
        );
    }

    @Override
    @Transactional
    public Membership findMembership(FindMembershipCommand command) {
        MembershipJpaEntity entity = findMembershipPort.findMembership(new Membership.MembershipId(command.getMembershipId())).get();
        return membershipMapper.mapToDomainEntity(entity);
    }
}
