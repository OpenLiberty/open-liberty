/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions;

import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions.LivePartitionTestBean.PARTITION_COUNT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions.LivePartitionTestBean.AckStatus;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions.LivePartitionTestBean.ReceivedMessage;

/**
 * Tests partition rebalancing in something more akin to a live environment
 * <p>
 * In particular, we want to test rebalancing while the application is actively processing messages to ensure that none are lost and they're handled correctly.
 */
@WebServlet("/LivePartitionTest")
public class LivePartitionTestServlet extends AbstractKafkaTestServlet {

    private static final long serialVersionUID = 1L;

    public static final String APP_GROUPID = "kafka-live-partition-test-group";
    @Resource
    private ManagedExecutorService executor;

    @Inject
    private LivePartitionTestBean bean;

    @Test
    public void testLivePartitionAssignment() throws Exception {

        List<String> sentMessages = new ArrayList<>();

        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Load messages into topic
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerConfig)) {
            for (int partition = 0; partition < PARTITION_COUNT; partition++) {
                for (int message = 0; message < 100; message++) {
                    String value = partition + "-" + message;
                    ProducerRecord<String, String> record = new ProducerRecord<String, String>(LivePartitionTestBean.CHANNEL_IN, partition, null, value);
                    producer.send(record);
                    sentMessages.add(value);
                }

                // Add a sentinal message to the end of each partition
                String value = partition + "-" + LivePartitionTestBean.FINAL_MESSAGE_NUMBER;
                ProducerRecord<String, String> record = new ProducerRecord<String, String>(LivePartitionTestBean.CHANNEL_IN, partition, null, value);
                producer.send(record);
                sentMessages.add(value);
            }
        }

        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, APP_GROUPID);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Sleep
        Thread.sleep(700);

        // Start a second consumer
        LivePartitionTestConsumer consumer1 = new LivePartitionTestConsumer(consumerConfig, LivePartitionTestBean.CHANNEL_IN);
        Future<?> future1 = executor.submit(consumer1);

        // Wait for it to finish and close
        future1.get();

        // Wait
        Thread.sleep(700);

        // Start a third consumer
        LivePartitionTestConsumer consumer2 = new LivePartitionTestConsumer(consumerConfig, LivePartitionTestBean.CHANNEL_IN);
        Future<?> future2 = executor.submit(consumer2);

        // Wait for it to finish and close
        future2.get();

        // Wait for the bean to finish consuming messages
        bean.awaitFinish();

        // Debug: print out the count of messages received by the bean
        Map<Object, Long> messageCountByAckStatus = bean.getMessages().stream().collect(Collectors.groupingBy(m -> m.ackStatus.get(), Collectors.counting()));
        System.out.println("Message counts by acknowledgement status: " + messageCountByAckStatus);

        // Check that successfully acknowledged messages are in ascending order for each partition
        Map<Integer, Integer> highestMessagePerPartition = new HashMap<>();
        IntStream.range(0, PARTITION_COUNT)
                        .forEach(i -> highestMessagePerPartition.put(i, -1));

        for (ReceivedMessage message : bean.getMessages()) {
            assertThat("Received message not yet acked: " + message, message.ackStatus.get(), not(AckStatus.ACK_PENDING));
            if (message.ackStatus.get() == AckStatus.ACK_SUCCESS) {
                assertThat("Out of order message: " + message, message.number, greaterThan(highestMessagePerPartition.get(message.partition)));
                highestMessagePerPartition.put(message.partition, message.number);
            }
        }

        // Check that each sent message was received exactly once
        Stream<String> receivedMessages = bean.getMessages().stream()
                        .filter(m -> m.ackStatus.get() == AckStatus.ACK_SUCCESS)
                        .map(ReceivedMessage::toString);
        receivedMessages = Stream.concat(receivedMessages, consumer1.getMessages().stream());
        receivedMessages = Stream.concat(receivedMessages, consumer2.getMessages().stream());

        List<String> receivedMessageList = new ArrayList<>(receivedMessages.collect(Collectors.toList()));

        Set<String> receivedMessageSet = new HashSet<>();
        receivedMessageList.forEach(m -> assertTrue("Message received twice: " + m, receivedMessageSet.add(m)));

        Collections.sort(sentMessages);
        Collections.sort(receivedMessageList);
        assertEquals("Recived messages not the same as sent messages", sentMessages, receivedMessageList);
    }

}
