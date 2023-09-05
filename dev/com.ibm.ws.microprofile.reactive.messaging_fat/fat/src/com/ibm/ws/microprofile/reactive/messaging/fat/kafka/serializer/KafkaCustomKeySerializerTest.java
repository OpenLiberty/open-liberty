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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.serializer;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PlaintextTests;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class KafkaCustomKeySerializerTest {

    private static final String APP_NAME = "myDataKeyKafkaTest";

    public static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = KafkaKeySerializerTestServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME, ReactiveMessagingActions.MP61_RM30, ReactiveMessagingActions.MP20_RM10, ReactiveMessagingActions.MP50_RM30, ReactiveMessagingActions.MP60_RM30);

    @BeforeClass
    public static void setup() throws Exception {
        ConnectorProperties outgoingProperties = simpleOutgoingChannel(PlaintextTests.connectionProperties(), MyDataMessagingBean2.OUT_CHANNEL);
        outgoingProperties.addProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, MyDataSerializer.class.getName());
        outgoingProperties.addProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MyDataSerializer.class.getName());

        ConnectorProperties incomingProperties = simpleIncomingChannel(PlaintextTests.connectionProperties(), MyDataMessagingBean2.IN_CHANNEL, MyDataMessagingBean2.GROUP_ID);
        incomingProperties.addProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, MyDataDeserializer.class.getName());
        incomingProperties.addProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MyDataDeserializer.class.getName());

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, PlaintextTests.kafkaContainer.getBootstrapServers())
                        .include(incomingProperties)
                        .include(outgoingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(KafkaKeySerializerTestServlet.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        server.stopServer();
    }

    @AfterClass
    public static void teardownKafka() throws ExecutionException, InterruptedException, IOException {
        KafkaUtils.deleteKafkaTopics(PlaintextTests.getAdminClient());
    }
}
