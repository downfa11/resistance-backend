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
                          @Value("${task.result.topic}")String topic,
                          @Value("${consumer.group}") String groupId,
                          LoggingProducer loggingProducer, CountDownLatchManager countDownLatchManager) {
        this.loggingProducer = loggingProducer;
        this.countDownLatchManager = countDownLatchManager;
        this.mapper = new ObjectMapper();

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        log.info("current membership Pod's consumer-group : "+groupId);

        Thread consumerThread = new Thread(() -> {
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                    for (ConsumerRecord<String, String> record : records) {
                        processRecord(record);
                    }
                }
            } finally {
                consumer.close();
            }
        });
        consumerThread.start();
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        log.info("processRecord: " + record.value());

        Task task = mapRecordToTask(record.value());
        String data = mapTaskToData(task);

        if(data==null)
            return;

        countDownLatchManager.setDataForKey(task.getTaskID(), data);

        CountDownLatch latch = countDownLatchManager.getCountDownLatch(task.getTaskID());
        if (latch != null) {
            latch.countDown();
        } else {
            log.warn("CountDownLatch is null: {}", task.getTaskID());
        }
    }

    private Task mapRecordToTask(String value){
        try {
            return mapper.readValue(value, Task.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON for task: " + value, e);
        }
    }

    private String mapTaskToData(Task task){
        try {
            return mapper.writeValueAsString(task);
        } catch (Exception e) {
            throw new RuntimeException("Error setting data for task : "+task.getTaskID(),e);
        }
    }


}