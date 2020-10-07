/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.ack.auto.KafkaAutoAckTest;
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

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

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

    @ClassRule
    public static Network network = Network.newNetwork();

    @ClassRule
    public static KafkaContainer kafkaContainer = new KafkaContainer().withNetwork(network);

    @BeforeClass
    public static void beforeSuite() throws Exception {
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
    }
}
