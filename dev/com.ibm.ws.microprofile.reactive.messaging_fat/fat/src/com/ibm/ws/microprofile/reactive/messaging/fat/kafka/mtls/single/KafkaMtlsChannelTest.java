/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.mtls.single;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.MtlsTests;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
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
public class KafkaMtlsChannelTest {

    private static final String APP_NAME = "kafkaMtlsChannelTest";
    private static final String APP_GROUP_ID = "mtls-channel-test-group";
    private static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = KafkaMtlsTestServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME, ReactiveMessagingActions.MP61_RM30, ReactiveMessagingActions.MP20_RM10, ReactiveMessagingActions.MP50_RM30);

    @BeforeClass
    public static void setup() throws Exception {
        ConnectorProperties outgoingProperties = simpleOutgoingChannel(null, BasicMessagingBean.CHANNEL_OUT)
                .addProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KafkaUtils.KEYSTORE_FILENAME)
                .addProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MtlsTests.kafkaContainer.getKeystorePassword());

        ConnectorProperties incomingProperties = simpleIncomingChannel(null, BasicMessagingBean.CHANNEL_IN, APP_GROUP_ID)
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

        server.startServer();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        try {
            server.stopServer();
        } finally {
            KafkaUtils.deleteKafkaTopics(MtlsTests.getAdminClient());
        }
    }

}
