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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PlaintextTests;
import com.ibm.ws.microprofile.reactive.messaging.kafka.KafkaInput;
import com.ibm.ws.microprofile.reactive.messaging.kafka.PartitionTrackerFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaConsumer;

/**
 * Reactive Streams TCK test for the Kafka incoming connector
 */
public class KafkaPublisherVerification extends PublisherVerification<Message<String>> {

    private final KafkaTestClient kafkaTestClient = new KafkaTestClient(PlaintextTests.kafkaContainer.getBootstrapServers());
    private final KafkaAdapterFactory kafkaAdapterFactory = new TestKafkaAdapterFactory();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private static final int MESSAGE_LIMIT = 200;
    private static final int TIMEOUT_MILLIS = 5000;
    private int testNo = 0;
    private String testName;
    private final List<KafkaInput<?, ?>> kafkaInputs = new ArrayList<>();

    @Override
    public long maxElementsFromPublisher() {
        return publisherUnableToSignalOnComplete();
    }

    public KafkaPublisherVerification() {
        super(new TestEnvironment(TIMEOUT_MILLIS));
    }

    @BeforeMethod
    public void handleTestMethodName(Method method) {
        testName = method.getName();
    }

    @AfterMethod
    public void cleanup() {
        kafkaTestClient.cleanUp();
        for (KafkaInput<?, ?> input : kafkaInputs) {
            try {
                input.shutdown();
            } catch (Exception e) {
            }
        }
        kafkaInputs.clear();
    }

    /** {@inheritDoc} */
    @Override
    public Publisher<Message<String>> createFailedPublisher() {
        // Pick a kafka topic name
        String topicName = "kafka-publisher-test-" + ++testNo + "-" + testName;

        // Push one message into Kafka
        try (KafkaWriter<String, String> writer = kafkaTestClient.writerFor(topicName)) {
            writer.sendMessage("test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Create the publisher, configured with a value deserializer which always throws an exception
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-publisher-verification-consumer-" + testNo);
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, PlaintextTests.kafkaContainer.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-publisher-verification");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, FailingDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        KafkaConsumer<String, String> kafkaConsumer = kafkaAdapterFactory.newKafkaConsumer(config);
        PartitionTrackerFactory trackerFactory = new PartitionTrackerFactory();
        trackerFactory.setExecutor(executor);
        trackerFactory.setAutoCommitEnabled(false);
        KafkaInput<String, String> kafkaInput = new KafkaInput<>(kafkaAdapterFactory, trackerFactory, kafkaConsumer, executor, topicName, 100);
        kafkaInputs.add(kafkaInput);
        return kafkaInput.getPublisher().buildRs();
    }

    /** {@inheritDoc} */
    @Override
    public Publisher<Message<String>> createPublisher(long elements) {
        // Pick a kafka topic name
        String topicName = "kafka-publisher-test-" + ++testNo + "-" + testName;

        elements = Math.min(elements, MESSAGE_LIMIT); // Cap count of messages we will create

        // Push the required number of elements into Kafka
        try (KafkaWriter<String, String> writer = kafkaTestClient.writerFor(topicName)) {
            for (int i = 0; i < elements; i++) {
                System.out.println("Sending test message to " + topicName);
                writer.sendMessage("Test-message-" + i);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Create the publisher
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-publisher-verification-consumer-" + testNo);
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, PlaintextTests.kafkaContainer.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-publisher-verification");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        KafkaConsumer<String, String> kafkaConsumer = kafkaAdapterFactory.newKafkaConsumer(config);
        PartitionTrackerFactory trackerFactory = new PartitionTrackerFactory();
        trackerFactory.setExecutor(executor);
        trackerFactory.setAutoCommitEnabled(false);
        KafkaInput<String, String> kafkaInput = new KafkaInput<>(kafkaAdapterFactory, trackerFactory, kafkaConsumer, executor, topicName, 100);
        kafkaInputs.add(kafkaInput);
        return kafkaInput.getPublisher().buildRs();
    }

}
