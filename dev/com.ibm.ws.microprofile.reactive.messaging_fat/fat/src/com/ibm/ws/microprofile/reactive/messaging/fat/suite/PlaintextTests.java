/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.ack.auto.KafkaAutoAckTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context.KafkaDefaultContextTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context.custom.KafkaCustomContextTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context.custom.invalid.KakfaInvalidContextTest;
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

import componenttest.containers.SimpleLogConsumer;
import componenttest.containers.TestContainerSuite;

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
                KafkaCustomContextTest.class,
                KafkaCustomSerializerTest.class,
                KafkaCustomKeySerializerTest.class,
                KafkaDefaultContextTest.class,
                KafkaFlatMapTest.class,
                KakfaInvalidContextTest.class,
                KafkaPartitionTest.class,
                KafkaSharedLibTest.class,
                MissingGroupIDTest.class,
                ReactiveStreamsTckTest.class,
                UseConfiguredTopicTest.class,
                UseProducerRecordTopicTest.class
})
public class PlaintextTests extends TestContainerSuite {

    @ClassRule
    public static ExtendedKafkaContainer kafkaContainer = new ExtendedKafkaContainer()
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .withStartupAttempts(3)
                    .withLogConsumer(new SimpleLogConsumer(PlaintextTests.class, "kafka"));

    public static Map<String, Object> connectionProperties() {
        return Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    }

    public static AdminClient getAdminClient() {
        Map<String, Object> adminClientProps = connectionProperties();
        return AdminClient.create(adminClientProps);
    }
}
