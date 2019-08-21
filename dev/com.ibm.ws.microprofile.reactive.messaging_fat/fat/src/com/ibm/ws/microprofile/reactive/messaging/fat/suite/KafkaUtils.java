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

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.testcontainers.containers.KafkaContainer;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.KafkaSaslPlainContainer;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.KafkaTlsContainer;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class KafkaUtils {

    private static final String TRUSTSTORE_FILENAME = "kafka-truststore.jks";

    public static File[] kafkaClientLibs() {
        File libsDir = new File("lib/LibertyFATTestFiles/libs");
        return libsDir.listFiles();
    }

    public static URL kafkaPermissions() {
        return KafkaUtils.class.getResource("permissions.xml");
    }

    public static Map<String, Object> connectionProperties(KafkaContainer container) {
        HashMap<String, Object> result = new HashMap<>();
        result.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        return result;
    }

    public static Map<String, Object> connectionProperties(KafkaTlsContainer container) {
        HashMap<String, Object> result = new HashMap<>();
        result.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        result.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, TRUSTSTORE_FILENAME);
        result.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, TlsTests.kafkaContainer.getKeystorePassword());
        result.put("security.protocol", "SSL");
        return result;
    }

    public static Map<String, Object> connectionProperties(KafkaSaslPlainContainer container) {
        HashMap<String, Object> result = new HashMap<>();
        result.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        result.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, TRUSTSTORE_FILENAME);
        result.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, TlsTests.kafkaContainer.getKeystorePassword());
        result.put("security.protocol", "SASL_SSL");
        result.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        result.put(SaslConfigs.SASL_JAAS_CONFIG,
                   "org.apache.kafka.common.security.plain.PlainLoginModule required "
                                                 + "username=\"" + container.getTestUser() + "\" "
                                                 + "password=\"" + container.getTestSecret() + "\";");
        return result;
    }

    public static void copyTrustStore(KafkaTlsContainer container, LibertyServer server) throws Exception {
        copyFileToServer(container.getKeystoreFile(), server);
    }

    public static void copyTrustStore(KafkaSaslPlainContainer container, LibertyServer server) throws Exception {
        copyFileToServer(container.getKeystoreFile(), server);
    }

    private static void copyFileToServer(File file, LibertyServer server) throws Exception {
        // Easiest to copy it to the desired filename, then copy it to the server
        Path tmpDest = Paths.get(TRUSTSTORE_FILENAME);
        Files.copy(file.toPath(), tmpDest, StandardCopyOption.REPLACE_EXISTING);
        server.copyFileToLibertyServerRootUsingTmp(server.getServerRoot(), tmpDest.toString());
        Files.delete(tmpDest);
    }

}
