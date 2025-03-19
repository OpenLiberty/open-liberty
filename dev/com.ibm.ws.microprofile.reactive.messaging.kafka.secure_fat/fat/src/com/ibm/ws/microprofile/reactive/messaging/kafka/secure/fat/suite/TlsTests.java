/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.suite;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.tls.KafkaTlsTest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;

import componenttest.containers.SimpleLogConsumer;
import componenttest.containers.TestContainerSuite;

/**
 * Suite for tests which run against a TLS enabled kafka broker
 */
@RunWith(Suite.class)
@SuiteClasses({ KafkaTlsTest.class,
})
public class TlsTests extends TestContainerSuite {

    @ClassRule
    public static ExtendedKafkaContainer kafkaContainer = new ExtendedKafkaContainer()
                    .withTls()
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .withStartupAttempts(3)
                    .withLogConsumer(new SimpleLogConsumer(TlsTests.class, "kafka-tls"));

    public static Map<String, Object> connectionProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, KafkaUtils.TRUSTSTORE_FILENAME);
        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, TlsTests.kafkaContainer.getKeystorePassword());
        properties.put("security.protocol", "SSL");
        return properties;
    }

    public static AdminClient getAdminClient() throws IOException {
        KafkaUtils.copyTrustStoreToTest(kafkaContainer);
        return AdminClient.create(connectionProperties());
    }

}
