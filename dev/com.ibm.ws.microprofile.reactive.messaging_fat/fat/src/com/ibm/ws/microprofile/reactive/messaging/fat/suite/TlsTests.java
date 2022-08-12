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
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tls.KafkaTlsTest;

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

    public static Map<String, String> connectionProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, KafkaUtils.TRUSTSTORE_FILENAME);
        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, TlsTests.kafkaContainer.getKeystorePassword());
        properties.put("security.protocol", "SSL");
        return properties;
    }

}
