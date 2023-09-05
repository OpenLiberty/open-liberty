/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.sasl_plain;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.Direction;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.SaslPlainTests;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Basic test using a kafka broker with TLS enabled
 */
@RunWith(FATRunner.class)
public class KafkaSaslPlainTest {

    private static final String APP_NAME = "kafkaSaslTest";
    private static final String APP_GROUP_ID = "sasl-test-group";

    @Server("SimpleRxMessagingServer")
    @TestServlet(contextRoot = APP_NAME, servlet = KafkaSaslTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        ConnectorProperties outgoingProperties = simpleOutgoingChannel(null, BasicMessagingBean.CHANNEL_OUT);

        ConnectorProperties incomingProperties = simpleIncomingChannel(null, BasicMessagingBean.CHANNEL_IN, APP_GROUP_ID);

        ConnectorProperties connectorProperties = new ConnectorProperties(Direction.CONNECTOR, "liberty-kafka")
                        .addAll(SaslPlainTests.connectionProperties());

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(SaslPlainTests.connectionProperties()))
                        .include(incomingProperties)
                        .include(outgoingProperties)
                        .include(connectorProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(KafkaSaslTestServlet.class.getPackage())
                        .addPackage(BasicMessagingBean.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        KafkaUtils.copyTrustStore(SaslPlainTests.kafkaContainer, server);

        server.startServer();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        server.stopServer();
    }

}
