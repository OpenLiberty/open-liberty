/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.mtls.mutliple.singleapp;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.mtls.mutliple.apps.MessagingBeanOne;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.mtls.mutliple.apps.MessagingBeanTwo;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.MtlsMultipleKeyStoresTests;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import org.apache.kafka.common.config.SslConfigs;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static componenttest.topology.utils.FATServletClient.runTest;


@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class KafkaMtlsMultipleChannelsTest {

    private static final String APP_NAME = "kafkaMtlsMultipleChannelTest";
    private static final String APP_GROUP_ID1 = "mtls-channel1-test-group";
    private static final String APP_GROUP_ID2 = "mtls-channel2-test-group";
    private static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME, ReactiveMessagingActions.MP61_RM30, ReactiveMessagingActions.MP20_RM10);

    @BeforeClass
    public static void setup() throws Exception {

        ConnectorProperties outgoingProperties = simpleOutgoingChannel(null, MessagingBeanOne.CHANNEL_OUT)
                // Valid Keystore
                .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE_FILENAME)
                .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsMultipleKeyStoresTests.kafkaContainer.getKeystorePassword());

        ConnectorProperties incomingProperties = simpleIncomingChannel(null, MessagingBeanOne.CHANNEL_IN, APP_GROUP_ID1)
                // Valid Keystore
                .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE_FILENAME)
                .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsMultipleKeyStoresTests.kafkaContainer.getKeystorePassword());

        // Channel Properties should take priority over Connector Properties. So provide valid keystore to channels and invalid to Connector.
        // If invalid keystore is used then the SSL Handshake will fail
        ConnectorProperties outgoingProperties2 = simpleOutgoingChannel(null, MessagingBeanTwo.CHANNEL_OUT)
                // Valid Keystore
                .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE2_FILENAME)
                .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsMultipleKeyStoresTests.kafkaContainer.getKeystorePassword());

        ConnectorProperties incomingProperties2 = simpleIncomingChannel(null, MessagingBeanTwo.CHANNEL_IN, APP_GROUP_ID2)
                // Valid Keystore
                .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE2_FILENAME)
                .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsMultipleKeyStoresTests.kafkaContainer.getKeystorePassword());

        ConnectorProperties connectorProperties = new ConnectorProperties(ConnectorProperties.Direction.CONNECTOR, "liberty-kafka")
                .addAll(MtlsMultipleKeyStoresTests.testConnectionProperties());

        // The Connection properties for the KafkaTestClient should include all certificates, so it uses the truststore as both trust and key stores so it has both client certs
        PropertiesAsset appConfig = new PropertiesAsset()
                .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(MtlsMultipleKeyStoresTests.connectionProperties()))
                .include(incomingProperties)
                .include(outgoingProperties)
                .include(incomingProperties2)
                .include(outgoingProperties2)
                .include(connectorProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                .addAsLibraries(kafkaClientLibs())
                .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                .addPackage(KafkaMultipleMtlsTestServlet.class.getPackage())
                .addPackage(MessagingBeanOne.class.getPackage())
                .addPackage(AbstractKafkaTestServlet.class.getPackage())
                .addPackage(KafkaTestConstants.class.getPackage())
                .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        KafkaUtils.copyTrustStore(MtlsMultipleKeyStoresTests.kafkaContainer, server);
        KafkaUtils.copyKeyStoresToServer(MtlsMultipleKeyStoresTests.kafkaContainer, server);

        server.startServer();
    }

    @Test
    public void testMutlipleMtlsChannels() throws Exception {
        runTest(server, APP_NAME + "/KafkaMultipleMtlsTestServlet", "testMtls");
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        try {
            server.stopServer();
        } finally {
            KafkaUtils.deleteKafkaTopics(MtlsMultipleKeyStoresTests.getAdminClient());
        }
    }

}
