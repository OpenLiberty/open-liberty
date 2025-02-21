/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.checkpoint;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.Direction;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.repeats.ReactiveMessagingActions;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.sasl_plain.KafkaSaslTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.suite.SaslPlainTests;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;
import static org.junit.Assert.assertNotNull;

/**
 * Basic test using a kafka broker with TLS enabled
 */
@RunWith(FATRunner.class)
@MaximumJavaLevel(javaLevel = 22)
@CheckpointTest
public class CheckpointKafkaSaslPlainTest {

    private static final String APP_NAME = "kafkaSaslTest";
    private static final String APP_GROUP_ID = "sasl-test-group";
    private static final String SERVER_NAME = "CheckpointSimpleRxMessagingServer";

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeatAboveMP61(SERVER_NAME);

    public static final Map<String, String> ENV_AFTER_CHECKPOINT = new HashMap<String, String>();

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = KafkaSaslTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        ConnectorProperties outgoingProperties = simpleOutgoingChannel(null, BasicMessagingBean.CHANNEL_OUT);

        ConnectorProperties incomingProperties = simpleIncomingChannel(null, BasicMessagingBean.CHANNEL_IN, APP_GROUP_ID);

        ConnectorProperties connectorProperties = new ConnectorProperties(Direction.CONNECTOR, "liberty-kafka")
                        .addAll(SaslPlainTests.connectionProperties());

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(SaslPlainTests.connectionProperties()))
                        .include(incomingProperties)
                        .include(outgoingProperties)
                        .include(connectorProperties);

        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        OutputStream bootstrapStream = bootstrapFile.openForWriting(true);
        Properties props = appConfig.getFinalProperties();

        createTwoConfigs(props, bootstrapStream); //This changes the contents of props

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(KafkaSaslTestServlet.class.getPackage())
                        .addPackage(BasicMessagingBean.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage());
        // This goes in bootstrap properties instead so checkpoint can update it
        //                .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        KafkaUtils.copyTrustStore(SaslPlainTests.kafkaContainer, server);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before restore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME + " started", 0));
                                 try {
                                     configureEnvVariable(server, ENV_AFTER_CHECKPOINT);
                                 } catch (Exception e) {
                                     Log.error(CheckpointKafkaSaslPlainTest.class, e.toString(), e);
                                     throw new RuntimeException(e);
                                 }
                             });

        server.startServer();
    }

    /**
     * For every key/value in appConfig this will create:
     * 1) A bootstrap.properties file in the format key=garbage in <random garbage>
     * 2) an entry in the hashmap ENV_AFTER_CHECKPOINT with the original key/value
     *
     * Before checkpoint the bootstrap.properties file will be used.
     * After checkpoint restore checkpoint will create a server.env file with the original
     * values that will be used instead.
     *
     * @throws Exception
     */
    private static void createTwoConfigs(Properties appConfig, OutputStream bootstrapStream) throws Exception {
        InputStream configStream = null;
        try {

            PrintWriter writer = new PrintWriter(bootstrapStream);
            String garbage = "";

            for (Object k : appConfig.keySet()) {
                String key = (String) k;

                //Put garbage into bootstrap.properties for before the checkpoint restore
                garbage = garbage + "a";//ensure each garbage is unique;
                writer.println(key + "=" + "garbage in " + garbage);

                //reformat the key to fit server.env variable rules
                String value = appConfig.getProperty(key).replaceAll("\\\\", "");//remove escape chars
                String keyServerEnvFormat = key.replace('.', '_').replace('-', '_'); //server.env uses underscores for keys

                ENV_AFTER_CHECKPOINT.put(keyServerEnvFormat, value);
            }

            writer.flush();
            bootstrapStream.flush();
        } catch (Exception e) {
            Log.error(CheckpointKafkaSaslPlainTest.class, e.toString(), e);
            throw e;
        } finally {
            bootstrapStream.close();
            if (configStream != null) {
                configStream.close();
            }
        }
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        try {
            kafkaStopServer(server);
        } finally {
            KafkaUtils.deleteKafkaTopics(SaslPlainTests.getAdminClient());
        }
    }

    static void configureEnvVariable(LibertyServer server, Map<String, String> serverEnvProperties) throws Exception {
        Log.warning(CheckpointKafkaSaslPlainTest.class, "start env config");
        File serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        try (PrintWriter pw = new PrintWriter(serverEnvFile)) {
            for (String s : serverEnvProperties.keySet()) {
                pw.println(s + "=" + serverEnvProperties.get(s));
            }
        }
    }

}
