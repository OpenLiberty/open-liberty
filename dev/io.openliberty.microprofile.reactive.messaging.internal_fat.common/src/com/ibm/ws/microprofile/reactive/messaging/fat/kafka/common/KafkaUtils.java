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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class KafkaUtils {

    public static final String TRUSTSTORE_FILENAME = "kafka-truststore.jks";
    public static final String KEYSTORE_FILENAME = "kafka-keystore.jks";
    public static final String KEYSTORE2_FILENAME = "kafka-keystore2.jks";

    private final static String KAFKA_REGEX = "E Error.*kafka";

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

    public static void copyKeyStoresToServer(ExtendedKafkaContainer container, LibertyServer server) throws Exception {
        // Copy First Keystore
        Path tmpDest = Paths.get(KEYSTORE_FILENAME);
        Files.copy(container.getKeystoreFile().toPath(), tmpDest, StandardCopyOption.REPLACE_EXISTING);
        server.copyFileToLibertyServerRootUsingTmp(server.getServerRoot(), tmpDest.toString());
        Path tmpDest2 = Paths.get(KEYSTORE2_FILENAME);
        Files.copy(container.getKeystoreFile2().toPath(), tmpDest2, StandardCopyOption.REPLACE_EXISTING);
        server.copyFileToLibertyServerRootUsingTmp(server.getServerRoot(), tmpDest2.toString());
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

    /**
     * Add the Kafka Library and supporting test framework classes to a test application archive.
     * <p>
     * This includes everything necessary to use the Kafka connector, and to inject a {@link KafkaTestClient} into a test servlet.
     *
     * @param war                  the test application archive
     * @param connectionProperties the connection properties which test clients should use to connect to the kafka broker
     */
    public static void addKafkaTestFramework(WebArchive war, Map<String, ?> connectionProperties) {
        // Create a jar with the framework classes and config
        JavaArchive frameworkJar = ShrinkWrap.create(JavaArchive.class, "KafkaTestFramework.jar")
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(KafkaTestClientProvider.class.getPackage())
                        .addAsResource(new PropertiesAsset().addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY,
                                                                         KafkaTestClientProvider.encodeProperties(connectionProperties)),
                                       "META-INF/microprofile-config.properties");

        // Add the framework jar, the kafka client and the permissions.xml to the app
        war.addAsLibrary(frameworkJar)
                        .addAsLibraries(KafkaUtils.kafkaClientLibs())
                        .addAsManifestResource(KafkaUtils.kafkaPermissions(), "permissions.xml");
    }

    /**
     * This method is syntatic sugar for calling server.stopServer() with an argument that makes it check the logs
     * for Kafka errors on shutdown and ensure the test is recorded as a failure if any such errors are found.
     *
     * This method considers anything in the logs matching the regex "E Error.*kafka" to be a Kafka error.
     *
     * @param ignoredFailuresRegex A list of reg expressions corresponding to warnings or errors that should be ignored.
     */
    public static void kafkaStopServer(LibertyServer server, String... ignoredFailuresRegex) throws Exception {
        List<String> failuresRegExps = Arrays.asList(LibertyServer.LIBERTY_ERROR_REGEX, KAFKA_REGEX);
        //booleans are default values you get when calling LibertyServer.stopServer() with no args
        server.stopServer(true, false, true, failuresRegExps, ignoredFailuresRegex);
    }

}
