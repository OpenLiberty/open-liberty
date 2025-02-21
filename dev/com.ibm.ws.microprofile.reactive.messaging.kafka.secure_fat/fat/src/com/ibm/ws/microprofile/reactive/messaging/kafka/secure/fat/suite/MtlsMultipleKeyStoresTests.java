/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.suite;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.mtls.mutliple.multipleapp.KafkaMtlsMultipleAppsTest;
import componenttest.containers.SimpleLogConsumer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@RunWith(Suite.class)
@Suite.SuiteClasses({
        KafkaMtlsMultipleAppsTest.class
})
public class MtlsMultipleKeyStoresTests {

    @ClassRule
    public static ExtendedKafkaContainer kafkaContainer = new ExtendedKafkaContainer()
            .withMTls()
            .mergeKeyStores()
            .withStartupTimeout(Duration.ofMinutes(2))
            .withStartupAttempts(3)
            .withLogConsumer(new SimpleLogConsumer(MtlsTests.class, "kafka-mtls-multiple"));

    /**
     * Does not contain keystore information to allow for the tests to set appropiately for whatever component needs to set the keystore information
     */
    public static Map<String, Object> testConnectionProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, KafkaUtils.TRUSTSTORE_FILENAME);
        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, MtlsTests.kafkaContainer.getKeystorePassword());
        properties.put("security.protocol", "SSL");
        return properties;
    }

    /**
     * Includes the Keystore information for use with the admin and KafkaTestClient which uses its own properties to connect
     *
     * Only need to use one of the two keystores for the connections
     * @return
     */
    public static Map<String, Object> connectionProperties(){
        Map<String, Object > cProps = testConnectionProperties();
        cProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.TRUSTSTORE_FILENAME);
        cProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsTests.kafkaContainer.getKeystorePassword());
        return cProps;
    }

    public static AdminClient getAdminClient() throws IOException {
        KafkaUtils.copyTrustStoreToTest(kafkaContainer);
        return AdminClient.create(connectionProperties());
    }
}
