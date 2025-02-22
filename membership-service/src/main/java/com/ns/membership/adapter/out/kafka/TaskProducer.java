package com.ns.membership.adapter.out.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.Task;
import com.ns.membership.application.port.out.SendTaskPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
public class TaskProducer implements SendTaskPort {
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper mapper;
    private final String topic;

    public TaskProducer(@Value("${kafka.clusters.bootstrapservers}") String bootstrapServers,
                        @Value("${task.topic}")String topic) {

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        this.producer = new KafkaProducer<>(props);
        this.mapper = new ObjectMapper();
        this.topic = topic;
    }

    public void sendTask(String key, Task task) {
        String json = mapTaskToData(task);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, json);

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                log.info("Message sent successfully. Offset: " + metadata.offset());
            } else {
                exception.printStackTrace();
            }
        });
    }

    private String mapTaskToData(Task task){
        try {
            return mapper.writeValueAsString(task);
        } catch(JsonProcessingException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendTaskPort(Task task) {
        this.sendTask(task.getTaskID(),task);
        log.info("Task ID : " + task.getTaskID());
    }
}