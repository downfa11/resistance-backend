package com.ns.membership.adapter.in.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.CountDownLatchManager;
import com.ns.common.LoggingProducer;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.membership.domain.userData;
import jakarta.validation.constraints.NotNull;
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
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class ResultConsumer {
    private final KafkaConsumer<String, String> consumer;
    private final LoggingProducer loggingProducer;
    @NotNull
    private final CountDownLatchManager countDownLatchManager;

    private final ObjectMapper mapper;

    public ResultConsumer(@Value("${kafka.clusters.bootstrapservers}") String bootstrapServers,
                          @Value("${task.result.topic}")String topic, LoggingProducer loggingProducer, CountDownLatchManager countDownLatchManager) {
        this.loggingProducer = loggingProducer;
        this.countDownLatchManager = countDownLatchManager;
        this.mapper = new ObjectMapper();

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
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                    for (ConsumerRecord<String, String> record : records) {

                        try{
                            processRecord(record);

                        }
                        catch(Exception e){
                            log.error("Error proessing record: "+record.value(),e);
                        }

                    }
                }
            } finally {
                consumer.close();
            }
        });
        consumerThread.start();
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        log.info("Received message: " + record.value());

        Task task;
        try {
            task = mapper.readValue(record.value(), Task.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON for task: " + record.value(), e);
            return;
        }

        boolean taskResult = task.getSubTaskList().stream().allMatch(subTask -> "success".equals(subTask.getStatus()));

        try {
            String json = mapper.writeValueAsString(task);
            countDownLatchManager.setDataForKey(task.getTaskID(), json);
        } catch (Exception e) {
            log.error("Error setting data for task : "+task.getTaskID(),e);
        }

        loggingProducer.sendMessage(task.getTaskID(), taskResult ? "task success" : "task failed");

        CountDownLatch latch = countDownLatchManager.getCountDownLatch(task.getTaskID());
        if (latch != null) {
            latch.countDown();
        } else {
            log.warn("CountDownLatch is null for task ID: {}", task.getTaskID());
        }
    }
}