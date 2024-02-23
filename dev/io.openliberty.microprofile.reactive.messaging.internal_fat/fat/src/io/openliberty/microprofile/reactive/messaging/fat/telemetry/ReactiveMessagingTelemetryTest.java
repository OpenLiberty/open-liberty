/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.telemetry;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.reactive.messaging.fat.apps.telemetry.ReactiveMessagingTelemetryTestServlet;
import io.openliberty.microprofile.reactive.messaging.fat.apps.telemetry.RmTelemetryProcessingBean;
import io.openliberty.microprofile.reactive.messaging.fat.apps.telemetry.RmTelemetryReceptionBean;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;
import io.openliberty.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

/**
 * Assert what happens when a message from Kafka is nacked
 */
@RunWith(FATRunner.class)
public class ReactiveMessagingTelemetryTest extends FATServletClient {

    private static final String APP_NAME = "ReactiveMessagingTelemetryApp";
    public static final String SERVER_NAME = "RxMessagingServerWithTelemetry";
    private static final String TOPIC_NAME = "RMTelemetryTopic";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = ReactiveMessagingTelemetryTestServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30);

    @BeforeClass
    public static void setup() throws Exception {

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, KafkaTests.kafkaContainer.getBootstrapServers())
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           RmTelemetryProcessingBean.CHANNEL_IN, APP_NAME, TOPIC_NAME))
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           ReactiveMessagingTelemetryTestServlet.EMITTER_CHANNEL, TOPIC_NAME))
                        .addProperty(CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(KafkaTests.connectionProperties()));

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClasses(ReactiveMessagingTelemetryTestServlet.class, RmTelemetryProcessingBean.class, RmTelemetryReceptionBean.class)
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false"), "META-INF/microprofile-config.properties")
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            kafkaStopServer(server);
        } finally {
            KafkaUtils.deleteKafkaTopics(KafkaTests.getAdminClient());
        }

    }

    @Test
    public void testEmitter() throws Exception {
        runTest(server, APP_NAME + "/RmTelemetryTest", "testEmitter");
    }

}
