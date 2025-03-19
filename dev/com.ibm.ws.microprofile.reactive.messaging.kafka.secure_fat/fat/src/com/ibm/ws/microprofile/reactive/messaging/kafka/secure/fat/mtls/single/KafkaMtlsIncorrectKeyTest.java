/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.mtls.single;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.SslConfigs;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.suite.MtlsTests;
import com.ibm.ws.microprofile.reactive.messaging.fat.repeats.ReactiveMessagingActions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This test covers the interactions that occur if at least one channel is using an incorrect certificate
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class KafkaMtlsIncorrectKeyTest {

    private static final String APP_NAME = "kafkaMtlsChannelTest";
    private static final String APP_GROUP_ID = "mtls-channel-test-group";
    private static final String SERVER_NAME = "SimpleRxMessagingServer";

    private static String inTopicName;
    private static String outTopicName;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {

        inTopicName = BasicMessagingBean.CHANNEL_IN + RepeatTestFilter.getRepeatActionsAsString();
        outTopicName = BasicMessagingBean.CHANNEL_OUT + RepeatTestFilter.getRepeatActionsAsString();

        // Given the deliberate breaking in the test, the topics don't end up
        List<NewTopic> newTopics = new ArrayList<>();
        newTopics.add(new NewTopic(inTopicName, 2, (short) 1));
        newTopics.add(new NewTopic(outTopicName, 2, (short) 1));
        MtlsTests.getAdminClient().createTopics(newTopics).all().get(KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        ConnectorProperties outgoingProperties = simpleOutgoingChannel(null, ConnectorProperties.DEFAULT_CONNECTOR_ID, BasicMessagingBean.CHANNEL_OUT, outTopicName)
                        .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE2_FILENAME)
                        .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsTests.kafkaContainer.getKeystorePassword());

        ConnectorProperties incomingProperties = simpleIncomingChannel(null, ConnectorProperties.DEFAULT_CONNECTOR_ID, BasicMessagingBean.CHANNEL_IN, APP_GROUP_ID, inTopicName)
                        .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE_FILENAME)
                        .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsTests.kafkaContainer.getKeystorePassword());

        ConnectorProperties connectorProperties = new ConnectorProperties(ConnectorProperties.Direction.CONNECTOR, "liberty-kafka")
                        .addAll(MtlsTests.testConnectionProperties());

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(MtlsTests.connectionProperties()))
                        .include(incomingProperties)
                        .include(outgoingProperties)
                        .include(connectorProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(KafkaMtlsTestServlet.class.getPackage())
                        .addPackage(BasicMessagingBean.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        KafkaUtils.copyTrustStore(MtlsTests.kafkaContainer, server);
        KafkaUtils.copyKeyStoresToServer(MtlsTests.kafkaContainer, server);
        KafkaUtils.copyTrustStoreToTest(MtlsTests.kafkaContainer);

        server.startServer();
    }

    @Test
    @AllowedFFDC("org.apache.kafka.common.errors.SslAuthenticationException")
    public void testIncorrectMtlsChannel() throws Exception {

        // We write from the test to the kafka server so we can guarantee that we are putting messages on the initial topic
        KafkaTestClient kafkaTestClient = new KafkaTestClient(MtlsTests.connectionProperties());

        try (KafkaWriter<String, String> writer = kafkaTestClient.writerFor(inTopicName)) {
            writer.sendMessage("abc");
            writer.sendMessage("xyz");
        }

        // We should have two messages on the Incoming channel, so show we still put some messages on the topics
        try (KafkaReader<String, String> reader = kafkaTestClient.readerFor(inTopicName)) {
            reader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        }
        try (KafkaReader<String, String> reader = kafkaTestClient.readerFor(outTopicName)) {
            reader.assertReadMessages(0, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        }

        assertNotNull("Did not find SSL Auth issue in logs",
                      server.waitForStringInLog("CWMRX1003E.*SslAuthenticationException"));
    }

    @AfterClass
    @AllowedFFDC("org.apache.kafka.common.errors.SslAuthenticationException")
    public static void teardownTest() throws Exception {
        try {
            //
            server.stopServer("CWMRX1011E", "CWMRX1004E", "CWMRX1003E");
        } finally {
            KafkaUtils.deleteKafkaTopics(MtlsTests.getAdminClient());
        }
    }
}
