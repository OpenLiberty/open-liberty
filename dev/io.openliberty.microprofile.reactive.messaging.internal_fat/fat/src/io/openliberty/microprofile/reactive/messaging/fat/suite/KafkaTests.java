/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.suite;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;

import componenttest.containers.SimpleLogConsumer;
import io.openliberty.microprofile.reactive.messaging.fat.kafka.emitter.basic.KafkaEmitterTest;
import io.openliberty.microprofile.reactive.messaging.fat.kafka.emitter.nack.KafkaEmitterNackRestfulTest;
import io.openliberty.microprofile.reactive.messaging.fat.kafka.emitter.restful.KafkaEmitterRestfulTest;
import io.openliberty.microprofile.reactive.messaging.fat.kafka.metrics.MetricsTest;
import io.openliberty.microprofile.reactive.messaging.fat.kafka.metrics.MultiAppMetricsTest;
import io.openliberty.microprofile.reactive.messaging.fat.kafka.nack.KafkaNackTest;
import io.openliberty.microprofile.reactive.messaging.fat.kafka.validation.KafkaValidationTests;
import io.openliberty.microprofile.reactive.messaging.fat.telemetry.ReactiveMessagingTelemetryTest;
import io.openliberty.microprofile.reactive.messaging.fat.telemetry.ReactiveMessagingTelemetryTestWithJAXRS;

@RunWith(Suite.class)
@SuiteClasses({
        KafkaNackTest.class,
        KafkaValidationTests.class,
        MetricsTest.class,
        MultiAppMetricsTest.class,
        ReactiveMessagingTelemetryTest.class,
        ReactiveMessagingTelemetryTestWithJAXRS.class,
        KafkaEmitterTest.class,
        KafkaEmitterRestfulTest.class,
        KafkaEmitterNackRestfulTest.class
})
public class KafkaTests {

    @ClassRule
    public static ExtendedKafkaContainer kafkaContainer = new ExtendedKafkaContainer()
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .withStartupAttempts(3)
                    .withLogConsumer(new SimpleLogConsumer(KafkaTests.class, "kafka"));

    public static Map<String, Object> connectionProperties() {
        return Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    }

    public static AdminClient getAdminClient() {
        Map<String, Object> adminClientProps = connectionProperties();
        return AdminClient.create(adminClientProps);
    }

}
