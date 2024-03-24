package com.ns.membership.application.service;


import com.fasterxml.jackson.core.JsonParser;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class DataService implements UserDataRequestUseCase {

    private final SendTaskPort sendTaskPort;

    private final MembershipRepository membershipRepository;
    private final CountDownLatchManager countDownLatchManager;


    @Override
    @Async
    @Cacheable(value="userData",key="'userData'+':'+ #command.membershipId+':'+ #command.targetIdList")
    public List<userData> getUserData(UserDataRequestCommand command) {
        try {

            List<SubTask> subtasklist = new ArrayList<>();

            for (Long membershipId : command.getTargetIdList()) {
                SubTask subtask = SubTask.builder()
                        .subTaskName("GetMemberDataTask : " + "MembershipId validation, transfer UserData.")
                        .membersrhipId(membershipId.toString())
                        .taskType("membership")
                        .status("ready")
                        .data(membershipId)
                        .build();

                subtasklist.add(subtask);
            }

            Task task = Task.builder()
                    .taskID(UUID.randomUUID().toString())
                    .taskName("GetUserDatas Task")
                    .membershipId(command.getMembershipId())
                    .subTaskList(subtasklist)
                    .build();



            sendTaskPort.sendTaskPort(task);
            countDownLatchManager.addCountDownLatch(task.getTaskID());

            countDownLatchManager.getCountDownLatch(task.getTaskID()).await();

            String result = countDownLatchManager.getDataForKey(task.getTaskID());

            ObjectMapper objectMapper = new ObjectMapper();
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

        } catch (Exception e) {
            log.error("Error retrieving user data: ", e);
            return Collections.emptyList();
        }
    }


    @Override
    public Set<Long> getAllyRandom(String membershipId) {
        int count = 5;
        return membershipRepository.getRandomAlly(membershipId, count).stream()
                .map(entity -> entity.getMembershipId())
                .collect(Collectors.toSet());
    }
}
