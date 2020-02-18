/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PlaintextTests;
import com.ibm.ws.microprofile.reactive.messaging.kafka.KafkaConnectorConstants;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class KafkaPartitionTest {

    private static final String APP_NAME = "KafkaPartitionTest";

    @Server("SimpleRxMessagingServer")
    @TestServlets({
                    @TestServlet(contextRoot = APP_NAME, servlet = KafkaPartitionTestServlet.class),
                    @TestServlet(contextRoot = APP_NAME, servlet = LivePartitionTestServlet.class)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Create a topic with two partitions
        Map<String, Object> adminClientProps = new HashMap<>();
        adminClientProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, PlaintextTests.kafkaContainer.getBootstrapServers());
        AdminClient adminClient = AdminClient.create(adminClientProps);

        List<NewTopic> newTopics = new ArrayList<>();
        newTopics.add(new NewTopic(PartitionTestReceptionBean.CHANNEL_NAME, 2, (short) 1));
        newTopics.add(new NewTopic(LivePartitionTestBean.CHANNEL_IN, LivePartitionTestBean.PARTITION_COUNT, (short) 1));
        adminClient.createTopics(newTopics).all().get(KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        // Create and deploy the app
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, PlaintextTests.kafkaContainer.getBootstrapServers())
                        .include(ConnectorProperties.simpleIncomingChannel(PlaintextTests.kafkaContainer, PartitionTestReceptionBean.CHANNEL_NAME,
                                                                           KafkaPartitionTestServlet.APP_GROUPID)
                                        .addProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5"))
                        .include(ConnectorProperties.simpleIncomingChannel(PlaintextTests.kafkaContainer, LivePartitionTestBean.CHANNEL_IN,
                                                                           LivePartitionTestServlet.APP_GROUPID)
                                        .addProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5")
                                        .addProperty(KafkaConnectorConstants.UNACKED_LIMIT, "100")); // Want to simulate having lots of unacked messages

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(KafkaTestClient.class.getPackage())
                        .addPackage(KafkaPartitionTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsLibraries(KafkaUtils.kafkaClientLibs())
                        .addAsManifestResource(KafkaUtils.kafkaPermissions(), "permissions.xml")
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer();
    }

}
