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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Test client for accessing Kafka
 * <p>
 * Includes methods for creating readers, writers and checking committed offsets for a topic.
 * <p>
 * When you're finished using it, call {@link #cleanUp()} to close all opened readers.
 */
public class KafkaTestClient {

    public static final String TEST_GROUPID = "delivery-test-group";

    private final String kafkaBootstrap;
    private final List<AutoCloseable> openClients = new ArrayList<>();

    /**
     * @param kafkaBootstrap the value for the kafka client bootstrap.servers property
     */
    public KafkaTestClient(String kafkaBootstrap) {
        super();
        this.kafkaBootstrap = kafkaBootstrap;
    }

    /**
     * Obtain a SimpleKafkaReader for the given topic name
     * <p>
     * The returned reader expects String messages and uses the {@value #TEST_GROUPID} consumer group
     *
     * @param topicName the topic to read from
     * @return the reader
     */
    public SimpleKafkaReader<String> readerFor(String topicName) {
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_GROUPID);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return readerFor(consumerConfig, topicName);
    }

    /**
     * Obtain a SimpleKafkaReader configured with the given consumer config
     *
     * @param config    the consumer config
     * @param topicName the topic to subscribe to
     * @return the reader
     */
    public SimpleKafkaReader<String> readerFor(Map<String, Object> config, String topicName) {
        // Defensive copy, avoid modifying parameter
        HashMap<String, Object> localConfig = new HashMap<>(config);

        // Default bootstrap servers if not set
        if (!localConfig.containsKey(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            localConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        }

        // Default to String deserializers if not set
        if (!localConfig.containsKey(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG)) {
            localConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        }

        if (!localConfig.containsKey(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG)) {
            localConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        }

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(localConfig);
        SimpleKafkaReader<String> reader = new SimpleKafkaReader<String>(kafkaConsumer, topicName);
        openClients.add(reader);
        return reader;
    }

    /**
     * Obtain a SimpleKafkaWriter for the given topic name
     * <p>
     * The returned writer writes String messages.
     *
     * @param topicName the topic to write to
     * @return the writer
     */
    public SimpleKafkaWriter<String> writerFor(String topicName) {
        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(producerConfig, new StringSerializer(), new StringSerializer());
        SimpleKafkaWriter<String> writer = new SimpleKafkaWriter<String>(kafkaProducer, topicName);
        openClients.add(writer);
        return writer;
    }

    /**
     * Get a KafkaConsumer for checking topic offsets
     * <p>
     * Offsets are committed for each consumer group, so you must specify the consumer group id to use.
     *
     * @param consumerGroupId the consumer group Id to use
     * @return the kafka consumer
     */
    private KafkaConsumer<?, ?> getKafkaTopicOffsetConsumer(String consumerGroupId) {
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<String, String>(consumerConfig, new StringDeserializer(), new StringDeserializer());
    }

    /**
     * Get the current committed offset for the given topic and consumer group
     * <p>
     * The topic must have a single partition
     *
     * @param topicName       the topic name
     * @param consumerGroupId the consumer group id
     * @return the current committed offset
     */
    public long getTopicOffset(String topicName, String consumerGroupId) {
        try (KafkaConsumer<?, ?> kafka = getKafkaTopicOffsetConsumer(consumerGroupId)) {
            TopicPartition topicPartition = getTopicPartition(kafka, topicName);
            return getOffset(kafka, topicPartition);
        }
    }

    /**
     * Get the current committed offset for the given partition and consumer group
     *
     * @param topicName       the topic name
     * @param partitionId     the partition id
     * @param consumerGroupId the consumer group id
     * @return the current committed offset
     */
    public long getTopicOffset(String topicName, int partitionId, String consumerGroupId) {
        try (KafkaConsumer<?, ?> kafka = getKafkaTopicOffsetConsumer(consumerGroupId)) {
            TopicPartition topicPartition = new TopicPartition(topicName, partitionId);
            return getOffset(kafka, topicPartition);
        }
    }

    /**
     * Get the partition for a topic which only has one partition
     * <p>
     * Throws an assertion error if the topic does not have one partition.
     */
    private TopicPartition getTopicPartition(KafkaConsumer<?, ?> kafka, String topicName) {
        List<PartitionInfo> partitions = kafka.partitionsFor(topicName);
        assertThat("Topic " + topicName + " doesn't have one partition", partitions, hasSize(1));
        return new TopicPartition(topicName, partitions.get(0).partition());
    }

    /**
     * Get the committed offset for a given partition
     * <p>
     * Returns the earliest offset if there's no committed offset
     *
     * @param kafka          the KafkaConsumer
     * @param topicPartition the partition to check
     * @return the committed offset if there is one, otherwise the earliest offset
     */
    private long getOffset(KafkaConsumer<?, ?> kafka, TopicPartition topicPartition) {
        OffsetAndMetadata committed = kafka.committed(topicPartition);
        if (committed != null) {
            return committed.offset();
        } else {
            // No committed offset, get the earliest available offset
            return kafka.beginningOffsets(Collections.singleton(topicPartition)).get(topicPartition);
        }
    }

    /**
     * Assert that the committed offset for the given topic advances to the given point with the timeout
     * <p>
     * The topic must have a single partition
     *
     * @param newOffset       the expected new offset
     * @param timeout         the timeout
     * @param topicName       the topic name
     * @param consumerGroupId the consumer group id
     * @throws InterruptedException if a thread interruption occurs
     */
    public void assertTopicOffsetAdvancesTo(long newOffset, Duration timeout, String topicName, String consumerGroupId) throws InterruptedException {
        assertTopicOffsetAdvancesTo(newOffset, timeout, topicName, -1, consumerGroupId);
    }

    /**
     * Assert that the committed offset for the given partition advances to the given point with the timeout
     *
     * @param newOffset       the expected new offset
     * @param timeout         the timeout
     * @param topicName       the topic name
     * @param partitionId     the partition ID, or {@code -1} to indicate that there is only one partition and the ID should be looked up automatically
     * @param consumerGroupId the consumer group id
     * @throws InterruptedException if a thread interruption occurs
     */

    public void assertTopicOffsetAdvancesTo(long newOffset, Duration timeout, String topicName, int partitionId, String consumerGroupId) throws InterruptedException {
        try (KafkaConsumer<?, ?> kafka = getKafkaTopicOffsetConsumer(consumerGroupId)) {
            TopicPartition topicPartition;
            if (partitionId == -1) {
                topicPartition = getTopicPartition(kafka, topicName);
            } else {
                topicPartition = new TopicPartition(topicName, partitionId);
            }
            Duration remaining = timeout;
            long startTime = System.nanoTime();

            long currentOffset = getOffset(kafka, topicPartition);

            while (!remaining.isNegative() && currentOffset < newOffset) {
                Thread.sleep(100);

                currentOffset = getOffset(kafka, topicPartition);

                Duration elapsed = Duration.ofNanos(System.nanoTime() - startTime);
                remaining = timeout.minus(elapsed);
            }

            assertEquals("Committed offset did not advance as expected", newOffset, currentOffset);
        }
    }

    /**
     * Closes all clients which have been opened using {@link #readerFor(String)} and {@link #writerFor(String)}
     */
    public void cleanUp() {
        RuntimeException mainException = null;
        for (AutoCloseable client : openClients) {
            try {
                client.close();
            } catch (Exception e) {
                if (mainException == null) {
                    mainException = new RuntimeException("Exception closing kafka clients", e);
                } else {
                    mainException.addSuppressed(e);
                }
            }
        }

        openClients.clear();

        if (mainException != null) {
            throw mainException;
        }
    }

}
