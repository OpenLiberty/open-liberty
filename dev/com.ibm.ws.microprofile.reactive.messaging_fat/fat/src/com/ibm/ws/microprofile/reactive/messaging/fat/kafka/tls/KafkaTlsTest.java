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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tls;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.kafka.common.config.SslConfigs;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.Direction;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PropertiesAsset;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.TlsTests;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Basic test using a kafka broker with TLS enabled
 */
@RunWith(FATRunner.class)
public class KafkaTlsTest {

    private static final String APP_NAME = "kafkaTlsTest";
    private static final String APP_GROUP_ID = "tls-test-group";
    private static final String TRUSTSTORE_FILENAME = "kafkakey.jks";

    @Server("SimpleRxMessagingServer")
    @TestServlet(contextRoot = APP_NAME, servlet = KafkaTlsTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ConnectorProperties outgoingProperties = simpleOutgoingChannel(TlsTests.kafkaContainer.getBootstrapServers(), BasicMessagingBean.CHANNEL_OUT);

        ConnectorProperties incomingProperties = simpleIncomingChannel(TlsTests.kafkaContainer.getBootstrapServers(), BasicMessagingBean.CHANNEL_IN, APP_GROUP_ID);

        ConnectorProperties connectorProperties = new ConnectorProperties(Direction.CONNECTOR, "io.openliberty.kafka")
                        .addProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, TRUSTSTORE_FILENAME)
                        .addProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, TlsTests.kafkaContainer.getKeystorePassword())
                        .addProperty("security.protocol", "SSL");

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, TlsTests.kafkaContainer.getBootstrapServers())
                        .addProperty(KafkaTlsTestServlet.TRUSTSTORE_PASSWORD_PROPERTY, TlsTests.kafkaContainer.getKeystorePassword())
                        .include(incomingProperties)
                        .include(outgoingProperties)
                        .include(connectorProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(KafkaTlsTestServlet.class.getPackage())
                        .addPackage(BasicMessagingBean.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // Copy the file so it's where copyFileToLibertyServerRoot wants it to be...
        Files.copy(TlsTests.kafkaContainer.getKeystoreFile().toPath(), Paths.get(server.pathToAutoFVTTestFiles, TRUSTSTORE_FILENAME));
        server.copyFileToLibertyServerRoot(TRUSTSTORE_FILENAME);

        server.startServer();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        server.stopServer();
    }

}
