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
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;

import componenttest.topology.impl.LibertyServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;

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

    /**
     * Attempt to delete all the topics in the container that the adminclient has been created for
     *
     * Due to the async and seemingly unreliable nature of kafka topic deletion this operation is done on a
     * best effort basis and may leave topics in a `Marked for deletion` state
     *
     * If a test is susceptible to ending up in this state which is typically due to have >1 partition for a topic
     * then using unique topic names per repeat is the recommended workaround
     *
     * @param adminClient
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void deleteKafkaTopics(AdminClient adminClient) throws ExecutionException, InterruptedException {
        ListTopicsResult topics = adminClient.listTopics();
        // `.get()` returns when the Future completes with a result.
        Set<String> topicNames = topics.names().get();
        //`.all()` wraps all the futures from deleteTopics into a signle future that can be
        // used to block progress given the async nature of the base deleteTopics command
        adminClient.deleteTopics(topicNames).all().get();
    }

}
