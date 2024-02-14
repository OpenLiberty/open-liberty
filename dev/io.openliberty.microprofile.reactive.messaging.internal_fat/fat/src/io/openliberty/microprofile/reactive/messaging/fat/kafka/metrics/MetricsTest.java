/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.kafka.metrics;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URL;
import java.util.HashMap;

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
import com.ibm.ws.microprofile.reactive.messaging.fat.metrics.MetricsUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;
import io.openliberty.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

/**
 *
 */
@RunWith(FATRunner.class)
// Allowing InstanceNotFoundException as it is possible for mpmetrics to be queried during server shutdown when
// the MBean is not present, this is an expected FFDC in the metrics FAT so we must allow for it here as these tests interact with metrics.
@AllowedFFDC("javax.management.InstanceNotFoundException")
public class MetricsTest extends FATServletClient {

    public static final String APP_NAME = "MetricsTest";
    public static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = MetricsTestServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30,
                                                                  ReactiveMessagingActions.MP60_RM30);

    @BeforeClass
    public static void setup() throws Exception {
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, KafkaTests.kafkaContainer.getBootstrapServers())
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           MetricsTestServlet.EMITTER_OUTGOING_CHANNEL, MetricsTestServlet.EMITTER_TOPIC))
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           MetricsReceptionBeanForPayloads.CHANNEL_IN, APP_NAME, MetricsTestServlet.EMITTER_TOPIC))
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           MetricsDeliveryBean.METRICS_OUTGOING, MetricsTestServlet.OUTGOING_TOPIC))
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           MetricsReceptionBeanForMessages.CHANNEL_IN, APP_NAME, MetricsTestServlet.OUTGOING_TOPIC));

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClasses(MetricsTestServlet.class, MetricsDeliveryBean.class, MetricsReceptionBeanForPayloads.class, MetricsReceptionBeanForMessages.class)
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testDeliverPayload() throws Exception {
        // Test that metrics are updated when a payload is delivered via an emitter
        runTest(server, APP_NAME + "/MetricsTest", "emitterDeliverPayload");
        URL url = HttpUtils.createURL(server, "/metrics");
        HashMap<String, Integer> metrics = MetricsUtils.getReactiveMessagingMetrics(url);

        assertThat("5 messages not detected on channel", metrics.get("metrics-incoming"), equalTo(5));
        assertThat("Outgoing and incoming channel counts are not the same",
                   metrics.get(MetricsTestServlet.EMITTER_OUTGOING_CHANNEL),
                   equalTo(metrics.get(MetricsReceptionBeanForPayloads.CHANNEL_IN)));
    }

    @Test
    public void testDeliverMessage() throws Exception {
        // Test that metrics are updated when a message is delivered via delivery and reception beans
        runTest(server, APP_NAME + "/MetricsTest", "deliverMessage");
        URL url = HttpUtils.createURL(server, "/metrics");
        HashMap<String, Integer> metrics = MetricsUtils.getReactiveMessagingMetrics(url);

        assertThat("5 messages not detected on channel", metrics.get("metrics-outgoing"), equalTo(5));
        assertThat("Outgoing and incoming channel counts are not the same",
                   metrics.get(MetricsDeliveryBean.METRICS_OUTGOING),
                   equalTo(metrics.get(MetricsReceptionBeanForMessages.CHANNEL_IN)));
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
