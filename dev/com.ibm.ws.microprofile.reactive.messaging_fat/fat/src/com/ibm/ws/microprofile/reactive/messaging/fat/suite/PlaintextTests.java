/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.ack.auto.KafkaAutoAckTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.delivery.KafkaAcknowledgementTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.flatmap.KafkaFlatMapTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.message.ConsumerRecordTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.message.UseConfiguredTopicTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.message.UseProducerRecordTopicTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions.KafkaPartitionTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.serializer.KafkaCustomKeySerializerTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.serializer.KafkaCustomSerializerTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.sharedLib.KafkaSharedLibTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck.ReactiveStreamsTckTest;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;

/**
 * Tests which run against a plaintext Kafka broker
 */
@RunWith(Suite.class)
@SuiteClasses({
                BadConnectorIDTest.class,
                BasicReactiveMessagingTest.class,
                ConsumerRecordTest.class,
                KafkaMessagingTest.class,
                KafkaAcknowledgementTest.class,
                KafkaAutoAckTest.class,
                KafkaCustomSerializerTest.class,
                KafkaCustomKeySerializerTest.class,
                KafkaFlatMapTest.class,
                KafkaPartitionTest.class,
                KafkaSharedLibTest.class,
                MissingGroupIDTest.class,
                ReactiveStreamsTckTest.class,
                UseConfiguredTopicTest.class,
                UseProducerRecordTopicTest.class,
})
public class PlaintextTests {

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    @ClassRule
    public static ExtendedKafkaContainer kafkaContainer = new ExtendedKafkaContainer()
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .withStartupAttempts(3)
                    .withLogConsumer(new SimpleLogConsumer(PlaintextTests.class, "kafka"));

    public static Map<String, String> connectionProperties() {
        return Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    }
}
