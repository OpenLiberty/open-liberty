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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.net.URL;
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
import com.ibm.ws.microprofile.reactive.messaging.fat.metrics.MetricsUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
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
public class MultiAppMetricsTest extends FATServletClient {

    public static final String APP_ONE_NAME = "MetricsTestOne";
    public static final String APP_TWO_NAME = "MetricsTestTwo";
    public static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30);

    @BeforeClass
    public static void setup() throws Exception {
        PropertiesAsset appOneConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(KafkaTests.connectionProperties()))
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, KafkaTests.kafkaContainer.getBootstrapServers())
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           MessagingBeanOne.CHANNEL_OUT, MultiAppMetricsTestServletOne.OUTGOING_TOPIC_NAME)
                                        .addProperty(ProducerConfig.CLIENT_ID_CONFIG, APP_ONE_NAME))
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           MessagingBeanOne.CHANNEL_IN, APP_ONE_NAME, MultiAppMetricsTestServletOne.INCOMING_TOPIC_NAME));

        PropertiesAsset appTwoConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(KafkaTests.connectionProperties()))
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, KafkaTests.kafkaContainer.getBootstrapServers())
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           MessagingBeanTwo.CHANNEL_OUT, MultiAppMetricsTestServletTwo.OUTGOING_TOPIC_NAME)
                                        .addProperty(ProducerConfig.CLIENT_ID_CONFIG, APP_TWO_NAME))
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           MessagingBeanTwo.CHANNEL_IN, APP_TWO_NAME, MultiAppMetricsTestServletTwo.INCOMING_TOPIC_NAME));

        WebArchive appOneWar = createWar(APP_ONE_NAME, appOneConfig, MultiAppMetricsTestServletOne.class, MessagingBeanOne.class);
        WebArchive appTwoWar = createWar(APP_TWO_NAME, appTwoConfig, MultiAppMetricsTestServletTwo.class, MessagingBeanTwo.class);
        ShrinkHelper.exportDropinAppToServer(server, appOneWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, appTwoWar, SERVER_ONLY);
        server.startServer();
    }

    private static WebArchive createWar(String appName, PropertiesAsset appConfig, Class<?>... classes) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, appName + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClasses(classes)
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");
        return war;
    }

    @Test
    public void testMultipleApps() throws Exception {
        // both applications share the same channels but use different topics
        // we expect the server-wide metrics to include metrics for both apps (4 per channel)
        runTest(server, APP_ONE_NAME + "/MultiAppMetricsTestServletOne", "stringToUpperCase");
        runTest(server, APP_TWO_NAME + "/MultiAppMetricsTestServletTwo", "stringToReverse");

        URL url = HttpUtils.createURL(server, "/metrics");
        Map<String, Integer> overallMetrics = MetricsUtils.getReactiveMessagingMetrics(url);
        assertThat(overallMetrics.get(MessagingBeanOne.CHANNEL_IN), is(4));
        assertThat(overallMetrics.get(MessagingBeanOne.CHANNEL_OUT), is(4));

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
