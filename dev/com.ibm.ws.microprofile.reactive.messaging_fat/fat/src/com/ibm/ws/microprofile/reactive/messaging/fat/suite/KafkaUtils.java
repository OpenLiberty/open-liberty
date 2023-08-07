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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;

import componenttest.topology.impl.LibertyServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;

/**
 *
 */
public class KafkaUtils {

    public static final String TRUSTSTORE_FILENAME = "kafka-truststore.jks";

    public static File[] kafkaClientLibs() {
        File libsDir = new File("lib/LibertyFATTestFiles/libs");
        return libsDir.listFiles();
    }

    public static URL kafkaPermissions() {
        return KafkaUtils.class.getResource("permissions.xml");
    }

    public static void copyTrustStore(ExtendedKafkaContainer container, LibertyServer server) throws Exception {
        copyFileToServer(container.getKeystoreFile(), server);
    }

    private static void copyFileToServer(File file, LibertyServer server) throws Exception {
        // Easiest to copy it to the desired filename, then copy it to the server
        Path tmpDest = Paths.get(TRUSTSTORE_FILENAME);
        Files.copy(file.toPath(), tmpDest, StandardCopyOption.REPLACE_EXISTING);
        server.copyFileToLibertyServerRootUsingTmp(server.getServerRoot(), tmpDest.toString());
        Files.delete(tmpDest);
    }

    public static void copyTrustStoreToTest(ExtendedKafkaContainer container) throws IOException {
        File file = container.getKeystoreFile();
        Path tmpDest = Paths.get(TRUSTSTORE_FILENAME);
        Files.copy(file.toPath(), tmpDest, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void cleanKafka(ExtendedKafkaContainer container) throws IOException, ExecutionException, InterruptedException {
        Map<String, Object> adminClientProps = new HashMap<>();
        adminClientProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());

        //Configure additional settings for SSL and SASL_SSL connections
        if(!container.getListenerScheme().equals("PLAINTEXT")){
            KafkaUtils.copyTrustStoreToTest(container);
            adminClientProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, TRUSTSTORE_FILENAME);
            adminClientProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, container.getKeystorePassword());
            if(container.getListenerScheme().equals("SASL_SSL")){
                adminClientProps.put("security.protocol", "SASL_SSL");
                adminClientProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
                //Admin Credentials are required for use with the Admin Client, otherwise a SSL error is thrown
                adminClientProps.put(SaslConfigs.SASL_JAAS_CONFIG,
                        "org.apache.kafka.common.security.plain.PlainLoginModule required "
                                + "username=\"" + SaslPlainTests.ADMIN_USER + "\" "
                                + "password=\"" + SaslPlainTests.ADMIN_SECRET + "\";");
            } else if(container.getListenerScheme().equals("SSL")) {
                adminClientProps.put("security.protocol", "SSL");
            }
        }

        AdminClient adminClient = AdminClient.create(adminClientProps);

        ListTopicsResult topics = adminClient.listTopics();
        Set<String> topicNames = topics.names().get();
        adminClient.deleteTopics(topicNames);
    }

}
