package com.ns.business.adpater.in.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.business.adpater.out.kafka.TaskResultProducer;
import com.ns.business.adpater.out.persistance.UserDataJpaEntity;
import com.ns.business.adpater.out.persistance.UserDataMapper;
import com.ns.business.application.port.in.command.RegisterUserDataCommand;
import com.ns.business.application.port.out.FindUserDataPort;
import com.ns.business.application.port.out.RegisterUserDataPort;
import com.ns.business.domain.UserData;
import com.ns.business.domain.UserDataMembership;
import com.ns.common.SubTask;
import com.ns.common.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
@Slf4j
@Component
public class TaskConsumer {
    private final KafkaConsumer<String, String> consumer;
    private final TaskResultProducer taskResultProducer;
    private final FindUserDataPort findUserDataPort;
    private final RegisterUserDataPort registerUserDataPort;

    private final UserDataMapper userDataMapper;

    public TaskConsumer(@Value("${kafka.clusters.bootstrapservers}") String bootstrapServers,
                        @Value("${task.topic}")String topic, TaskResultProducer taskResultProducer,FindUserDataPort findUserDataPort,RegisterUserDataPort registerUserDataPort,
                        UserDataMapper userDataMapper) {

        this.taskResultProducer = taskResultProducer;
        this.findUserDataPort=findUserDataPort;
        this.registerUserDataPort=registerUserDataPort;
        this.userDataMapper=userDataMapper;

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "my-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
        Thread consumerThread = new Thread(() -> {
            try {
                while (true) {
                    ObjectMapper mapper = new ObjectMapper();
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                    for (ConsumerRecord<String, String> record : records) {
                        System.out.println("Received message: " + record.value());

                        try {
                            Task tasks = mapper.readValue(record.value(), Task.class);
                            List<SubTask> subTaskList = tasks.getSubTaskList();

                            for (SubTask subTask : subTaskList) {
                               if(subTask.getTaskType().equals("membership"))
                                   subTask = getUserDataByMembershipId(subTask);

                               else if(subTask.getTaskType().equals("register"))
                                   subTask = Register(subTask);
                            }

                            this.taskResultProducer.sendTaskResult(tasks.getTaskID(), tasks);

                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } finally {
                consumer.close();
            }
        });
        consumerThread.start();
    }

    private SubTask getUserDataByMembershipId(SubTask subTask){
        String membershipIdString = subTask.getData().toString();
        Long membershipId = Long.parseLong(membershipIdString);


        try {
            UserDataMembership userData = userDataMapper.mapToDomainJump(membershipId);
            subTask.setData(userData);
            log.info(userData.toString());
            subTask.setStatus("success");
        }

        catch(RuntimeException e) {
            log.error("Error processing subtask: " + e.getMessage());
            subTask.setStatus("fail");
            subTask.setData(null);
        }

        return subTask;
    }

    private SubTask Register(SubTask subTask){
        Long membershipId = Long.parseLong(subTask.getMembersrhipId());
        String membershipName = (String) subTask.getData();
        try {
            UserDataJpaEntity jpaEntity = registerUserDataPort.createUserData(
                    new UserData.UserId(membershipId),
                    new UserData.UserName(membershipName),
                    new UserData.UserGold(0),
                    new UserData.UserHighscore(0),
                    new UserData.UserEnergy(0),
                    new UserData.UserScenario(0),
                    new UserData.UserHead("0"),
                    new UserData.UserBody("0"),
                    new UserData.UserArm("0"),
                    new UserData.UserHealth(100),
                    new UserData.UserAttack(15),
                    new UserData.UserCritical(3),
                    new UserData.UserDurability(10)
            );

            log.info(jpaEntity.getUserId()+"번 사용자를 생성했습니다.");
            if(jpaEntity!=null)
                subTask.setStatus("success");

        }catch(RuntimeException e){
            log.error("Error processing subatsk : "+e.getMessage());
            subTask.setData(null);
            subTask.setStatus("fail");
        }

        return subTask;
    }
}