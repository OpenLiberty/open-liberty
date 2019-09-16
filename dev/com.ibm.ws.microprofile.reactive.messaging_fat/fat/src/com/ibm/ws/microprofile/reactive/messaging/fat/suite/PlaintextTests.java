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
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.KafkaContainer;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.ack.auto.KafkaAutoAckTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.delivery.KafkaAcknowledgementTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions.KafkaPartitionTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.serializer.KafkaCustomSerializerTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.sharedLib.KafkaSharedLibTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck.ReactiveStreamsTckTest;

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

/**
 * Tests which run against a plaintext Kafka broker
 */
@RunWith(Suite.class)
@SuiteClasses({
                BasicReactiveMessagingTest.class,
                KafkaMessagingTest.class,
                KafkaAcknowledgementTest.class,
                KafkaAutoAckTest.class,
                KafkaCustomSerializerTest.class,
                KafkaPartitionTest.class,
                KafkaSharedLibTest.class,
                ReactiveStreamsTckTest.class,
                BadConnectorIDTest.class,
                MissingGroupIDTest.class,
})
public class PlaintextTests {

    @ClassRule
    public static KafkaContainer kafkaContainer = new KafkaContainer();

    @BeforeClass
    public static void beforeSuite() throws Exception {
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
    }
}
