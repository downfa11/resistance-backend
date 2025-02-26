package com.ns.membership.application.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.CountDownLatchManager;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.common.UseCase;
import com.ns.membership.adapter.out.persistance.MembershipRepository;
import com.ns.membership.application.port.in.UserDataRequestUseCase;
import com.ns.membership.application.port.in.command.UserDataRequestCommand;
import com.ns.membership.application.port.out.SendTaskPort;
import com.ns.membership.domain.userData;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class DataService implements UserDataRequestUseCase {
    private static final Integer RANDOM_ALLY_COUNT = 5;
    private final SendTaskPort sendTaskPort;

    private final MembershipRepository membershipRepository;
    private final CountDownLatchManager countDownLatchManager;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    @Async
    @Cacheable(value="userData",key="'userData'+':'+ #command.membershipId+':'+ #command.targetIdList")
    public List<userData> getUserData(UserDataRequestCommand command) {
        try {
            List<SubTask> subTasks = createSubTaskListGetMemberData(command);
            Task task = createTaskGetMemberData(command.getMembershipId(), subTasks);
            sendTaskPort.sendTaskPort(task);

            countDownLatchManager.addCountDownLatch(task.getTaskID());
            countDownLatchManager.getCountDownLatch(task.getTaskID())
                    .await();

            String result = countDownLatchManager.getDataForKey(task.getTaskID());
            return convertUserDataList(result);

        } catch (Exception e) {
            log.error("Error getUserData: ", e);
            return Collections.emptyList();
        }
    }

    private Task createTaskGetMemberData(String membershipId, List<SubTask> subTasks){
        return Task.builder()
                .taskID(UUID.randomUUID().toString())
                .taskName("GetUserDatas Task")
                .membershipId(membershipId)
                .subTaskList(subTasks)
                .build();
    }

    private List<SubTask> createSubTaskListGetMemberData(UserDataRequestCommand command){
        List<SubTask> subTasks = new ArrayList<>();

        for (Long membershipId : command.getTargetIdList()) {
            SubTask subtask = createSubTaskGetMemberData(String.valueOf(membershipId));
            subTasks.add(subtask);
        }
        return subTasks;
    }

    private SubTask createSubTaskGetMemberData(String membershipId){
        return SubTask.builder()
                .subTaskName("GetMemberDataTask : " + "MembershipId validation, transfer UserData.")
                .membersrhipId(membershipId)
                .taskType("membership")
                .status("ready")
                .data(membershipId)
                .build();
    }

    private List<userData> convertUserDataList(String result) throws JsonProcessingException {
        Task taskData = objectMapper.readValue(result, Task.class);

        List<userData> userDataList = new ArrayList<>();
        for (SubTask subTask : taskData.getSubTaskList()) {
            if (subTask.getStatus().equals("success")) {
                try {
                    userData userData = objectMapper.convertValue(subTask.getData(), userData.class);
                    userDataList.add(userData);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }

        return userDataList;
    }


    @Override
    public Set<Long> getAllyRandom(String membershipId) {
        return membershipRepository.getRandomAlly(membershipId, RANDOM_ALLY_COUNT)
                .stream()
                .map(entity -> entity.getMembershipId())
                .collect(Collectors.toSet());
    }
}
