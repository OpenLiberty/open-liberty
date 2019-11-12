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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.ack.auto;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PlaintextTests;
import com.ibm.ws.microprofile.reactive.messaging.kafka.KafkaConnectorConstants;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class KafkaAutoAckTest {

    private static final String APP_NAME = "KafkaAutoAckTest";

    @Server("SimpleRxMessagingServer")
    @TestServlet(contextRoot = APP_NAME, servlet = KafkaAutoAckTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        Map<String, Object> connectionProps = KafkaUtils.connectionProperties(PlaintextTests.kafkaContainer);

        ConnectorProperties incomingConnection = ConnectorProperties.simpleIncomingChannel(connectionProps, KafkaAutoAckReceptionBean.CHANNEL_IN,
                                                                                           KafkaAutoAckTestServlet.APP_GROUPID);
        incomingConnection.addProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        incomingConnection.addProperty(KafkaConnectorConstants.UNACKED_LIMIT, "5"); // Set a small unacked limit
        incomingConnection.addProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "500"); // Set a small auto-commit interval so the test runs quickly

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(connectionProps))
                        .include(incomingConnection);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(KafkaAutoAckTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(KafkaTestClientProvider.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

}
