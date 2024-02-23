/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.reactive.messaging.fat.kafka.emitter.restful;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.reactive.messaging.fat.apps.emitter.EmitterApplication;
import io.openliberty.microprofile.reactive.messaging.fat.apps.emitter.EmitterRestResource;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;
import io.openliberty.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class KafkaEmitterRestfulTest {

    public static final String APP_NAME = "kafka-message-restful-emitter";
    public static final String SERVER_NAME = "SimpleRxMessagingServer";
    public static KafkaTestClient kafkaTestClient;

    public static String payload_topic_name;
    public static String message_topic_name;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static final RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME, ReactiveMessagingActions.MP61_RM30, ReactiveMessagingActions.MP50_RM30,
                                                                        ReactiveMessagingActions.MP60_RM30);

    @BeforeClass
    public static void setup() throws Exception {

        payload_topic_name = EmitterRestResource.PAYLOAD_CHANNEL_NAME + RepeatTestFilter.getRepeatActionsAsString();
        message_topic_name = EmitterRestResource.MESSAGE_CHANNEL_NAME + RepeatTestFilter.getRepeatActionsAsString();

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, KafkaTests.kafkaContainer.getBootstrapServers())
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           EmitterRestResource.PAYLOAD_CHANNEL_NAME, payload_topic_name))
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           EmitterRestResource.MESSAGE_CHANNEL_NAME, message_topic_name));

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(EmitterApplication.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        kafkaTestClient = new KafkaTestClient(KafkaTests.connectionProperties());
    }

    @Test
    public void testEmittingRestPayload() throws Exception {
        sendRequests("payload");

        try (KafkaReader<String, String> reader = kafkaTestClient.readerFor(payload_topic_name)) {
            List<String> messages = reader.assertReadMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            assertThat(messages, contains("payload1", "payload2", "payload3", "payload4", "payload5"));
        }
    }

    @Test
    public void testEmttingRestMessage() throws Exception {
        sendRequests("message");

        try (KafkaReader<String, String> reader = kafkaTestClient.readerFor(message_topic_name)) {
            List<String> messages = reader.assertReadMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            assertThat(messages, contains("message1", "message2", "message3", "message4", "message5"));
        }
    }

    public void sendRequests(String path) throws IOException {
        int port = server.getHttpDefaultPort();
        URL url = HttpUtils.createURL(server, APP_NAME + "/" + path);
        // Send 5 messages to the server to make sure we do process multiple requests
        for (int count = 1; count < 6; count++) {
            HttpURLConnection conn = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HttpUtils.HTTPRequestMethod.POST);
            try (OutputStream os = conn.getOutputStream()) {
                os.write((path + count).getBytes());
                os.flush();
                assertThat(conn.getResponseCode(), is(204));
            }
            conn.disconnect();
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            kafkaStopServer(server);
        } finally {
            KafkaUtils.deleteKafkaTopics(KafkaTests.getAdminClient());
        }
    }
}
