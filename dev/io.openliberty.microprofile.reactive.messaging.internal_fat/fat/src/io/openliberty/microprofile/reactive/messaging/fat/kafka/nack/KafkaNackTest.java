/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.kafka.nack;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY;
import static java.util.Arrays.asList;

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
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;
import io.openliberty.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

/**
 * Assert what happens when a message from Kafka is nacked
 */
@RunWith(FATRunner.class)
public class KafkaNackTest extends FATServletClient {

    private static final String APP_NAME = "KafkaNackTest";

    public static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = KafkaNackTestServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30,
                                                                  ReactiveMessagingActions.MP60_RM30);

    @BeforeClass
    public static void setup() throws Exception {
        // Configure delivery to a non-existant broker so that we can test with messages which aren't delivered
        Map<String, Object> invalidKafkaConfig = Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "PLAINTEXT://localhost:10000");
        ConnectorProperties invalidOutgoingConfig = ConnectorProperties.simpleOutgoingChannel(invalidKafkaConfig, KafkaNackTestDeliveryBean.NACK_TEST_CHANNEL)
                        .addProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG, "5000");

        // Configure reception from a real kafka broker to test nacking received messages
        ConnectorProperties incomingConfig = ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(),
                                                                                       KafkaNackReceptionBean.CHANNEL_IN,
                                                                                       APP_NAME);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .include(invalidOutgoingConfig)
                        .include(incomingConfig)
                        .addProperty(CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(KafkaTests.connectionProperties()));

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(KafkaNackTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            kafkaStopServer(server, "CWMRX1003E.*nack-test-channel", // CWMRX1003E: An error occurred when sending a message to the Kafka broker. The error is: org.apache.kafka.common.errors.TimeoutException: Topic nack-test-channel not present in metadata after 5000 ms.
                            "CWMRX1011E.*KafkaNackTestException");
        } finally {
            KafkaUtils.deleteKafkaTopics(KafkaTests.getAdminClient());
        }
    }

    @Test
    @ExpectedFFDC("io.openliberty.microprofile.reactive.messaging.fat.kafka.nack.KafkaNackTestException")
    public void testIncomingMessageCanBeNacked() throws Exception {
        server.setMarkToEndOfLog();

        // Test servlet receives messages and nacks two of them
        runTest(server, APP_NAME + "/NackTest", "testIncomingMessageCanBeNacked");

        server.waitForStringsInLogUsingMark(asList("CWMRX1011E.*KafkaNackTestException: Test exception 1",
                                                   "CWMRX1011E.*KafkaNackTestException: Test exception 2")); // throws if not found
    }

}
