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

import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.checkpoint.CheckpointKafkaSaslPlainTest;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.liberty_login.LibertyLoginModuleTest;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.liberty_login.invalid.LibertyLoginModuleInvalidTest;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.liberty_login.none.LibertyLoginModuleNoEncTest;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.liberty_login.special_chars.LibertyLoginModuleSpecialCharsTest;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.liberty_login.xor.LibertyLoginModuleXorTest;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.sasl_plain.KafkaSaslPlainTest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.utility.Base58;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;

import componenttest.containers.SimpleLogConsumer;
import componenttest.containers.TestContainerSuite;

/**
 * Suite for tests which run against a TLS enabled kafka broker
 */
@RunWith(Suite.class)
@SuiteClasses({
                KafkaSaslPlainTest.class,
                CheckpointKafkaSaslPlainTest.class,
                LibertyLoginModuleTest.class,
                LibertyLoginModuleXorTest.class,
                LibertyLoginModuleNoEncTest.class,
                LibertyLoginModuleInvalidTest.class,
                LibertyLoginModuleSpecialCharsTest.class,
})
public class SaslPlainTests extends TestContainerSuite {

    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_SECRET = generateSecret(ADMIN_USER);
    public static final String TEST_USER = "test";
    public static final String TEST_SECRET = generateSecret(TEST_USER);
    public static final String SPECIAL_USER = "specialchars";
    public static final String SPECIAL_SECRET = generateSecret("{test}^&*+=-$(");

    @ClassRule
    public static ExtendedKafkaContainer kafkaContainer = new ExtendedKafkaContainer()
                    .withTls()
                    .withListenerScheme("SASL_SSL")
                    .withKafkaJaasConfig(getKafkaJaasConf())
                    .withZookeeperJaasConfig(getZookeeperJaasConf())
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .withStartupAttempts(3)
                    .withLogConsumer(new SimpleLogConsumer(SaslPlainTests.class, "kafka-sasl"));

    public static String generateSecret(String prefix) {
        return prefix + "-" + Base58.randomString(6);
    }

    public static AdminClient getAdminClient() throws IOException {
        KafkaUtils.copyTrustStoreToTest(kafkaContainer);
        Map<String, Object> adminClientProps = connectionProperties();
        adminClientProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        adminClientProps.replace(SaslConfigs.SASL_JAAS_CONFIG,
                                 "org.apache.kafka.common.security.plain.PlainLoginModule required "
                                                               + "username=\"" + ADMIN_USER + "\" "
                                                               + "password=\"" + ADMIN_SECRET + "\";");
        return AdminClient.create(adminClientProps);
    }

    public static Map<String, Object> connectionProperties() {
        HashMap<String, Object> result = new HashMap<>();
        result.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        result.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, KafkaUtils.TRUSTSTORE_FILENAME);
        result.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaContainer.getKeystorePassword());
        result.put("security.protocol", "SASL_SSL");
        result.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        result.put(SaslConfigs.SASL_JAAS_CONFIG,
                   "org.apache.kafka.common.security.plain.PlainLoginModule required "
                                                 + "username=\"" + TEST_USER + "\" "
                                                 + "password=\"" + TEST_SECRET + "\";");
        return result;
    }

    private static String getKafkaJaasConf() {
        StringBuilder builder = new StringBuilder();
        builder.append("KafkaServer {\n");
        builder.append("   org.apache.kafka.common.security.plain.PlainLoginModule required\n");
        builder.append("   serviceName=\"kafka\"\n");
        builder.append("   username=\"");
        builder.append(ADMIN_USER);
        builder.append("\"\n");
        builder.append("   password=\"");
        builder.append(ADMIN_SECRET);
        builder.append("\"\n");

        builder.append("   user_");
        builder.append(ADMIN_USER);
        builder.append("=\"");
        builder.append(ADMIN_SECRET);
        builder.append("\"\n");

        builder.append("   user_");
        builder.append(TEST_USER);
        builder.append("=\"");
        builder.append(TEST_SECRET);
        builder.append("\"\n");

        builder.append("   user_");
        builder.append(SPECIAL_USER);
        builder.append("=\"");
        builder.append(SPECIAL_SECRET);
        builder.append("\";\n");

        builder.append("};\n\n");

        builder.append("Client {\n");
        builder.append("   org.apache.kafka.common.security.plain.PlainLoginModule required\n");
        builder.append("   username=\"");
        builder.append(ADMIN_USER);
        builder.append("\"\n");
        builder.append("   password=\"");
        builder.append(ADMIN_SECRET);
        builder.append("\";\n");

        builder.append("};");
        String conf = builder.toString();

        return conf;
    }

    private static String getZookeeperJaasConf() {
        StringBuilder builder = new StringBuilder();
        builder.append("Server {\n");
        builder.append("   org.apache.zookeeper.server.auth.DigestLoginModule required\n");
        builder.append("   user_");
        builder.append(ADMIN_USER);
        builder.append("=\"");
        builder.append(ADMIN_SECRET);
        builder.append("\";\n");

        builder.append("};\n");
        String conf = builder.toString();

        return conf;
    }

}
