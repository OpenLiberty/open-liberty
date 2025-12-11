/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.mtls.mutliple.multipleapp;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static componenttest.topology.utils.FATServletClient.runTest;

import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.mtls.mutliple.apps.MessagingBeanOne;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.mtls.mutliple.apps.MessagingBeanTwo;
import org.apache.kafka.clients.producer.ProducerConfig;
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
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.suite.MtlsMultipleKeyStoresTests;
import com.ibm.ws.microprofile.reactive.messaging.fat.repeats.ReactiveMessagingActions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class KafkaMtlsMultipleAppsTest {

    private static final String APP1_NAME = "kafkaMtlsAppOne";
    private static final String APP2_NAME = "kafkaMtlsAppTwo";
    private static final String APP_GROUP_ID1 = "mtls-channel1-test-group";
    private static final String APP_GROUP_ID2 = "mtls-channel2-test-group";
    private static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {

        //Connector details are common for both applications as we aren't applying the certs at this level
        ConnectorProperties connectorProperties = new ConnectorProperties(ConnectorProperties.Direction.CONNECTOR, "liberty-kafka")
                        .addAll(MtlsMultipleKeyStoresTests.testConnectionProperties());

        // When Kafka is initializing its producers and consumers, it also creates additional ones under the covers, in particular for metrics.
        // As consumers typically have group.id set, the client.id is a derivation of the `<group.id>-<some number>`
        // Producers do not have group.id or have a client.id set. By default, the client.id = `producer-<some number>`. with some number being incremental
        // When multiple RM applications are running with at least one producer each, we start to see issues of MBeans already existing and not existing
        // Not sure why that would be, but all the errors relate to the producer-metrics type, no other type seems to appear in the logs
        // by setting at least one outgoings client.id, the issue is resolved.
        //
        // This does only seem to impact multiple applications on the same server, there is no evidence other tests have issues based on their logs.
        //
        // Setting on the Connectors does not resolve the issue as it then generates slightly different, but still related issues.

        // App One Connection Properties
        ConnectorProperties outgoingProperties = simpleOutgoingChannel(null, MessagingBeanOne.CHANNEL_OUT)
                        .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE_FILENAME)
                        .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsMultipleKeyStoresTests.kafkaContainer.getKeystorePassword())
                        .addProperty(ProducerConfig.CLIENT_ID_CONFIG, "AppOne");

        ConnectorProperties incomingProperties = simpleIncomingChannel(null, MessagingBeanOne.CHANNEL_IN, APP_GROUP_ID1)
                        .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE_FILENAME)
                        .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsMultipleKeyStoresTests.kafkaContainer.getKeystorePassword());

        // App Two Connection properties using different keystore
        ConnectorProperties outgoingProperties2 = simpleOutgoingChannel(null, MessagingBeanTwo.CHANNEL_OUT)
                        // Valid Keystore
                        .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE2_FILENAME)
                        .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsMultipleKeyStoresTests.kafkaContainer.getKeystorePassword())
                        .addProperty(ProducerConfig.CLIENT_ID_CONFIG, "AppTwo");

        ConnectorProperties incomingProperties2 = simpleIncomingChannel(null, MessagingBeanTwo.CHANNEL_IN, APP_GROUP_ID2)
                        // Valid Keystore
                        .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE2_FILENAME)
                        .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsMultipleKeyStoresTests.kafkaContainer.getKeystorePassword());

        // The Connection properties for the KafkaTestClient should include all certificates, so it uses the truststore as both trust and key stores so it has both client certs
        PropertiesAsset appConfig1 = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(MtlsMultipleKeyStoresTests.connectionProperties()))
                        .include(incomingProperties)
                        .include(outgoingProperties)
                        .include(connectorProperties);

        PropertiesAsset appConfig2 = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(MtlsMultipleKeyStoresTests.connectionProperties()))
                        .include(incomingProperties2)
                        .include(outgoingProperties2)
                        .include(connectorProperties);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, APP1_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClasses(KafkaMtlsTestServletOne.class, MessagingBeanOne.class)
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(appConfig1, "META-INF/microprofile-config.properties");

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, APP2_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClasses(KafkaMtlsTestServletTwo.class, MessagingBeanTwo.class)
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(appConfig2, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war1, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, war2, SERVER_ONLY);

        KafkaUtils.copyTrustStore(MtlsMultipleKeyStoresTests.kafkaContainer, server);
        KafkaUtils.copyKeyStoresToServer(MtlsMultipleKeyStoresTests.kafkaContainer, server);

        server.startServer();
    }

    @Test
    public void testMutlipleMtlsApps() throws Exception {
        runTest(server, APP1_NAME + "/KafkaMtlsServletOne", "putMessages");
        runTest(server, APP2_NAME + "/KafkaMtlsServletTwo", "putMessages");

        // the Kafka JMX issues are reported as Errors, not exceptions/ffdcs or have codes
        // so is not picked up on shutdown of the server, meaning that it passes
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        try {
            KafkaUtils.kafkaStopServer(server);
        } finally {
            KafkaUtils.deleteKafkaTopics(MtlsMultipleKeyStoresTests.getAdminClient());
        }
    }
}
