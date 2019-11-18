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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.sasl_plain;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.Direction;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.SaslPlainTests;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Basic test using a kafka broker with TLS enabled
 */
@RunWith(FATRunner.class)
public class KafkaSaslPlainTest {

    private static final String APP_NAME = "kafkaSaslTest";
    private static final String APP_GROUP_ID = "sasl-test-group";

    @Server("SimpleRxMessagingServer")
    @TestServlet(contextRoot = APP_NAME, servlet = KafkaSaslTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        String bootstrapServers = SaslPlainTests.kafkaContainer.getBootstrapServers();
        String keystorePassword = SaslPlainTests.kafkaContainer.getKeystorePassword();
        String testUser = SaslPlainTests.kafkaContainer.getTestUser();
        String testSecret = SaslPlainTests.kafkaContainer.getTestSecret();

        ConnectorProperties outgoingProperties = simpleOutgoingChannel(bootstrapServers, BasicMessagingBean.CHANNEL_OUT);

        ConnectorProperties incomingProperties = simpleIncomingChannel(bootstrapServers, BasicMessagingBean.CHANNEL_IN, APP_GROUP_ID);

        ConnectorProperties connectorProperties = new ConnectorProperties(Direction.CONNECTOR, "liberty-kafka")
                        .addProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, KafkaSaslTestServlet.TRUSTSTORE_FILENAME)
                        .addProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, keystorePassword)
                        .addProperty("security.protocol", "SASL_SSL")
                        .addProperty(SaslConfigs.SASL_MECHANISM, "PLAIN")
                        .addProperty(SaslConfigs.SASL_JAAS_CONFIG,
                                     "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + testUser + "\" password=\""
                                                                   + testSecret + "\";");

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, bootstrapServers)
                        .addProperty(KafkaSaslTestServlet.TRUSTSTORE_PASSWORD_PROPERTY, keystorePassword)
                        .addProperty(KafkaSaslTestServlet.TEST_USER_PROPERTY, testUser)
                        .addProperty(KafkaSaslTestServlet.TEST_SECRET_PROPERTY, testSecret)
                        .include(incomingProperties)
                        .include(outgoingProperties)
                        .include(connectorProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(KafkaSaslTestServlet.class.getPackage())
                        .addPackage(BasicMessagingBean.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // Copy the file so it's where copyFileToLibertyServerRoot wants it to be...
        Files.copy(SaslPlainTests.kafkaContainer.getKeystoreFile().toPath(), Paths.get(server.pathToAutoFVTTestFiles, KafkaSaslTestServlet.TRUSTSTORE_FILENAME));
        server.copyFileToLibertyServerRoot(KafkaSaslTestServlet.TRUSTSTORE_FILENAME);

        server.startServer();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        server.stopServer();
    }

}
