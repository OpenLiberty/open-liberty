/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions;

import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions.PartitionTestReceptionBean.CHANNEL_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

@WebServlet("/KafkaPartitionTest")
public class KafkaPartitionTestServlet extends AbstractKafkaTestServlet {

    private static final long serialVersionUID = 1L;

    public static final String APP_GROUPID = "kafka-partition-test-group";

    @Inject
    @ConfigProperty(name = KAFKA_BOOTSTRAP_PROPERTY)
    private String kafkaBootstrap;

    @Inject
    private PartitionTestReceptionBean receptionBean;

    /**
     * Tests Kafka connector having partitions assigned and unassigned
     * <p>
     * In a microservices environment, the connector needs to cope correctly with new readers coming and going, which results in the broker assigning and unassigning partitions
     * from it.
     * <p>
     * Outline of the test:
     * <ul>
     * <li> A topic with two partitions has already been set up.</li>
     * <li>Initially the reactive messaging kafka connector should be subscribed to both, delivering all messages to the receptionBean.</li>
     * <li>We start an additional test reader, reading from the same topic with the same consumer group ID as the kafka connector. This simulates another copy of the application
     * starting and having one of the partitions assigned</li>
     * <li>We assert that one of the partitions remains assigned to the kafka connector, while the other is assigned to our test reader</li>
     * <li>We write messages to each partition, check they are delivered and that the partition offset is advanced correctly.</li>
     * <li>We close the test writer, expecting that the kafka broker will now assign both partitions to the kafka connector.</li>
     * <li>We write messages to each each partition and check that they are all delivered to the receptionBean and that the offsets of both partitions are advanced correctly.</li>
     * </ul>
     */
    @Test
    public void testPartitionRemovedAndAdded() throws Exception {
        // Subscribe a new test reader in the same consumer group which auto-commits offsets
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, APP_GROUPID);
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaReader<String, String> reader = kafkaTestClient.readerFor(consumerConfig, CHANNEL_NAME);

        List<PartitionInfo> allPartitions = reader.getConsumer().partitionsFor(CHANNEL_NAME);

        // Assert that the new reader is assigned one partition
        Duration elapsed = Duration.ZERO;
        long startTime = System.nanoTime();
        Set<TopicPartition> assignedPartitions = Collections.emptySet();
        while (elapsed.compareTo(KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT) < 0 && assignedPartitions.size() != 1) {
            reader.getConsumer().poll(Duration.ZERO); // Kafka doesn't process partition assignment unless we poll
            assignedPartitions = reader.getConsumer().assignment();
            Thread.sleep(100);
            elapsed = Duration.ofNanos(System.nanoTime() - startTime);
        }
        assertThat(assignedPartitions, hasSize(1));

        // Work out which partition is now assigned to the test reader, and which should still be assigned to the reception bean
        int testReaderPartitionId = assignedPartitions.iterator().next().partition();
        int receptionBeanPartitionId = -1;
        for (PartitionInfo p : allPartitions) {
            if (p.partition() != testReaderPartitionId) {
                receptionBeanPartitionId = p.partition();
                break;
            }
        }

        // We want to check that the offset is advanced correctly later, so take a baseline now
        long testReaderPartitionOffset = kafkaTestClient.getTopicOffset(CHANNEL_NAME, testReaderPartitionId, APP_GROUPID);
        long receptionBeanPartitionOffset = kafkaTestClient.getTopicOffset(CHANNEL_NAME, receptionBeanPartitionId, APP_GROUPID);

        // Send a message to the partition assigned to the test reader and check the new reader receives it and advances the partition offset
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(CHANNEL_NAME);
        writer.sendMessage("test1", testReaderPartitionId, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        List<String> testMessagesRead = reader.assertReadMessages(1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        assertThat(testMessagesRead, contains("test1"));

        // Send a message to the partition not assigned to the test reader and check that the bean receives it and advances the partition offset
        writer.sendMessage("test2", receptionBeanPartitionId, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        List<Message<String>> beanMessagesRead = receptionBean.assertReceivedMessages(1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        List<String> beanMessagePayloads = beanMessagesRead.stream().map(Message::getPayload).collect(Collectors.toList());
        beanMessagesRead.forEach(Message::ack);
        assertThat(beanMessagePayloads, contains("test2"));
        kafkaTestClient.assertTopicOffsetAdvancesTo(receptionBeanPartitionOffset + 1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT, CHANNEL_NAME, receptionBeanPartitionId,
                                                    APP_GROUPID);

        // Close the test reader
        reader.close();

        // With AUTO_COMMIT_CONFIG, we don't have control over when Kafka commits the partition offset for us
        // but it should happen once we've closed the reader
        kafkaTestClient.assertTopicOffsetAdvancesTo(testReaderPartitionOffset + 1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT, CHANNEL_NAME, testReaderPartitionId, APP_GROUPID);

        // Send a message to each partition and check that the bean receives both of them and advances the offset of both partitions
        writer.sendMessage("test3", testReaderPartitionId, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        writer.sendMessage("test4", receptionBeanPartitionId, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        // If the reception of these messages interacts with kafka partition reassignment, we may get duplicate messages which makes this check more complex :(
        elapsed = Duration.ZERO;
        startTime = System.nanoTime();
        boolean done = false;
        long testReaderFinalOffset = 0;
        long receptionBeanFinalOffset = 0;
        Set<String> messagePayloads = new HashSet<>();
        // Loop, polling and checking offsets, until we see the topic offsets advance
        while (elapsed.compareTo(KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT) < 0 && !done) {
            List<Message<String>> newMessages = receptionBean.getReceivedMessages();
            for (Message<String> m : newMessages) {
                messagePayloads.add(m.getPayload());
                m.ack();
            }

            testReaderFinalOffset = kafkaTestClient.getTopicOffset(CHANNEL_NAME, testReaderPartitionId, APP_GROUPID);
            receptionBeanFinalOffset = kafkaTestClient.getTopicOffset(CHANNEL_NAME, receptionBeanPartitionId, APP_GROUPID);
            if (testReaderFinalOffset == testReaderPartitionOffset + 2 && receptionBeanFinalOffset == receptionBeanPartitionOffset + 2) {
                done = true;
            }

            elapsed = Duration.ofNanos(System.nanoTime() - startTime);
            Thread.sleep(100);
        }

        // Check we got the right messages (using a set to ignore duplicates)
        assertThat(messagePayloads, containsInAnyOrder("test3", "test4"));
        assertEquals("Test reader partition offset did not advance correctly", testReaderPartitionOffset + 2, testReaderFinalOffset);
        assertEquals("Reception bean partition offset did not advance correctly", receptionBeanPartitionOffset + 2, receptionBeanFinalOffset);
    }

}
