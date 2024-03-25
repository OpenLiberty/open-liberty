/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.reactive.messaging.fat.kafka.validation;

import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.ws.microprofile.reactive.messaging.fat.AppValidator;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;
import io.openliberty.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

@RunWith(FATRunner.class)
@AllowedFFDC
public class KafkaValidationTests {

    private static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30,
                                                                  ReactiveMessagingActions.MP60_RM30);

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        KafkaUtils.kafkaStopServer(server,"CWMRX1006E");
    }

    @Test
    public void testMissingConnector() throws Exception {
        String testAppName = "MissingConnectorTest";
        // Configure delivery to a non-existant connector
        ConnectorProperties incoming = ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), "missingConnector", ValidConfig.INCOMING_CHANNEL, "Connector",
                                                                                 "Topic");
        PropertiesAsset appConfig = new PropertiesAsset().include(incoming);

        AppValidator.validateAppOn(server)
                        .withAppName(testAppName)
                        .withClass(ValidConfig.class)
                        .withAppConfig(appConfig)
                        .withLibrary(kafkaClientLibs())
                        .withManifestResource(kafkaPermissions(), "permissions.xml")
                        .failsWith("SRMSG00072.*" + ValidConfig.INCOMING_CHANNEL)
                        .run();
//        org.jboss.weld.exceptions.DeploymentException: SRMSG00072: Unknown connector for `ValidIncomingChannel`.
    }

    @Test
    public void testIncomingConnectorWithoutDownstream() throws Exception {
        String testAppName = "IncomingConnectorWithoutDownstreamTest";
        // Test DeploymentException is thrown when an incoming connector has no downstream channels
        String invalidChannel = "InvalidIncomingChannel";
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(KafkaTests.connectionProperties()))
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           invalidChannel, "KafkaApp", "Topic"))
                        // test an invalid incoming connector alongside a valid setup
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           ValidConfig.INCOMING_CHANNEL, "KafkaApp", "ValidTopic"))
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           ValidConfig.OUTGOING_CHANNEL, "ValidTopic"));
        AppValidator.validateAppOn(server)
                        .withAppName(testAppName)
                        .withClass(ValidConfig.class)
                        .withAppConfig(appConfig)
                        .withLibrary(kafkaClientLibs())
                        .withManifestResource(kafkaPermissions(), "permissions.xml")
                        .failsWith("Wiring error\\(s\\) detected in application.")
                        .failsWith("CWMRX1100E:.*" + testAppName)
                        .failsWith("- IncomingConnector\\{channel:'" + invalidChannel + "', attribute:'mp.messaging.incoming." + invalidChannel + "'\\} has no downstream")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.OpenGraphException: Some components are not connected to either downstream consumers or upstream producers:
//        - IncomingConnector{channel:'InvalidIncomingChannel', attribute:'mp.messaging.incoming.InvalidIncomingChannel'} has no downstream
    }

    @Test
    public void testIncomingConnectorWithMultipleDownstreams() throws Exception {
        String testAppName = "IncomingConnectorWithMultipleDownstreamsTest";
        // Test DeploymentException is thrown when an incoming connector has multiple downstream channels
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(KafkaTests.connectionProperties()))
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           IncomingConnectorMultipleDownstreams.INCOMING_CHANNEL, "KafkaApp", "ValidTopic"));
        AppValidator.validateAppOn(server)
                        .withAppName(testAppName)
                        .withClass(IncomingConnectorMultipleDownstreams.class)
                        .withAppConfig(appConfig)
                        .withLibrary(kafkaClientLibs())
                        .withManifestResource(kafkaPermissions(), "permissions.xml")
                        .failsWith("Wiring error\\(s\\) detected in application.")
                        .failsWith("CWMRX1100E:.*" + testAppName)
                        .failsWith("'IncomingConnector\\{channel:'" + IncomingConnectorMultipleDownstreams.INCOMING_CHANNEL + "', attribute:'mp.messaging.incoming."
                                   + IncomingConnectorMultipleDownstreams.INCOMING_CHANNEL + "'\\}' supports a single downstream consumer, but found 2")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.TooManyDownstreamCandidatesException: 'IncomingConnector{channel:'IncomingChannel',
//        attribute:'mp.messaging.incoming.IncomingChannel'}' supports a single downstream consumer, but found 2 ...
    }

    @Test
    public void testOutgoingConnectorWithoutUpstream() throws Exception {
        String testAppName = "OutgoingConnectorWithoutUpstreamTest";
        // Test DeploymentException is thrown when an outgoing connector has no upstream channels
        String invalidChannel = "InvalidOutgoingChannel";
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(KafkaTests.connectionProperties()))
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           invalidChannel, "Topic"))
                        // test an invalid outgoing connector alongside a valid setup
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           ValidConfig.INCOMING_CHANNEL, "KafkaApp", "ValidTopic"))
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           ValidConfig.OUTGOING_CHANNEL, "ValidTopic"));
        AppValidator.validateAppOn(server)
                        .withAppName(testAppName)
                        .withClass(ValidConfig.class)
                        .withAppConfig(appConfig)
                        .withLibrary(kafkaClientLibs())
                        .withManifestResource(kafkaPermissions(), "permissions.xml")
                        .failsWith("Wiring error\\(s\\) detected in application.")
                        .failsWith("CWMRX1100E:.*" + testAppName)
                        .failsWith("- OutgoingConnector\\{channel:'" + invalidChannel + "', attribute:'mp.messaging.outgoing." + invalidChannel + "'\\} has no upstream")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.OpenGraphException: Some components are not connected to either downstream consumers or upstream producers:
//        - OutgoingConnector{channel:'InvalidOutgoingChannel', attribute:'mp.messaging.outgoing.InvalidOutgoingChannel'} has no upstream
    }

    @Test
    public void testOutgoingConnectorWithMultipleUpstreams() throws Exception {
        String testAppName = "OutgoingConnectorWithMultipleUpstreamsTest";
        // Test DeploymentException is thrown when an outgoing connector has multiple upstream channels
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(KafkaTests.connectionProperties()))
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           OutgoingConnectorMultipleUpstreams.OUTGOING_CHANNEL, "ValidTopic"));
        AppValidator.validateAppOn(server)
                        .withAppName(testAppName)
                        .withClass(OutgoingConnectorMultipleUpstreams.class)
                        .withAppConfig(appConfig)
                        .withLibrary(kafkaClientLibs())
                        .withManifestResource(kafkaPermissions(), "permissions.xml")
                        .failsWith("Wiring error\\(s\\) detected in application.")
                        .failsWith("CWMRX1100E:.*" + testAppName)
                        .failsWith("'mp.messaging.outgoing." + OutgoingConnectorMultipleUpstreams.OUTGOING_CHANNEL + ".merge=true' to allow multiple upstreams.")
                        .run();
//      Suppressed: io.smallrye.reactive.messaging.providers.wiring.TooManyUpstreamCandidatesException: 'mp.messaging.outgoing.OutgoingChannel.merge=true' to allow multiple upstreams.
    }
}