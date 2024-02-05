/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.reactive.messaging.fat.kafka.emitter.nack;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.reactive.messaging.fat.apps.emitter.EmitterApplication;
import io.openliberty.microprofile.reactive.messaging.fat.apps.emitter.EmitterRestResource;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;
import io.openliberty.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class KafkaEmitterNackRestfulTest {

    public static final String APP_NAME = "kafka-message-restful-emitter";
    public static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static final RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME, ReactiveMessagingActions.MP61_RM30, ReactiveMessagingActions.MP50_RM30,
                                                                        ReactiveMessagingActions.MP60_RM30);

    @BeforeClass
    public static void setup() throws Exception {

        Map<String, Object> invalidKafkaConfig = Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "PLAINTEXT://localhost:10000");

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, KafkaTests.kafkaContainer.getBootstrapServers())
                        .include(ConnectorProperties.simpleOutgoingChannel(invalidKafkaConfig, EmitterRestResource.PAYLOAD_CHANNEL_NAME))
                        .include(ConnectorProperties.simpleOutgoingChannel(invalidKafkaConfig, EmitterRestResource.MESSAGE_CHANNEL_NAME));

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(EmitterApplication.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @Test
    public void testEmittingNackPayload() throws Exception {
        server.setMarkToEndOfLog();
        URL url = HttpUtils.createURL(server, APP_NAME + "/payload");
        HttpURLConnection conn = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HttpUtils.HTTPRequestMethod.POST);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(("test").getBytes());
            os.flush();
            assertThat(conn.getResponseCode(), is(500));
        }
        conn.disconnect();
        // check that we get error about the payload emitter channel
        assertNotNull("Channel fails to send the payload.", server.waitForStringInLogUsingMark("CWMRX1003E.*restful-emitter-payload"));
    }

    @Test
    public void testEmittingNackMessage() throws Exception {
        server.setMarkToEndOfLog();
        URL url = HttpUtils.createURL(server, APP_NAME + "/message");
        HttpURLConnection conn = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HttpUtils.HTTPRequestMethod.POST);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(("test").getBytes());
            os.flush();
            assertThat(conn.getResponseCode(), is(500));
        }
        conn.disconnect();
        // check that we get error about the message emitter channel
        assertNotNull("Channel fails to send the message.", server.waitForStringInLogUsingMark("CWMRX1003E.*restful-emitter-message"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            //We expect a kafka error and an error from RESTEASY relating to the failure when we respond to the request
            kafkaStopServer(server, "CWMRX1003E.*restful-emitter", "RESTEASY002020");
        } finally {
            KafkaUtils.deleteKafkaTopics(KafkaTests.getAdminClient());
        }
    }
}
