/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PlaintextTests;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that thread context is propagated correctly when receiving messages from Kafka without the concurrent feature enabled in server.xml.
 * <p>
 * Note that RM 1.0 depends on concurrent, so concurrent will be active for that repeat
 */
@RunWith(FATRunner.class)
public class KafkaDefaultContextTest extends FATServletClient {

    public static final String APP_NAME = "KafkaDefaultContext";
    public static final String SERVER_NAME = "ContextRxMessagingServer";

    public static final String INPUT_TOPIC = "KafkaDefaultContextTest-in";
    public static final String OUTPUT_TOPIC = "KafkaDefaultContextTest-out";

    @TestServlet(servlet = KafkaDefaultContextTestServlet.class, contextRoot = APP_NAME)
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30,
                                                                  ReactiveMessagingActions.MP20_RM10);

    @BeforeClass
    public static void setup() throws Exception {
        ConnectorProperties inputConfig = simpleIncomingChannel(PlaintextTests.connectionProperties(),
                                                                KafkaDefaultContextTestMessageBean.INPUT_CHANNEL,
                                                                APP_NAME);

        ConnectorProperties outputConfig = simpleOutgoingChannel(PlaintextTests.connectionProperties(),
                                                                 KafkaDefaultContextTestMessageBean.OUTPUT_CHANNEL);

        PropertiesAsset config = new PropertiesAsset()
                        .include(inputConfig)
                        .include(outputConfig);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(KafkaDefaultContextTest.class.getPackage())
                        .addAsResource(config, "META-INF/microprofile-config.properties");

        KafkaUtils.addKafkaTestFramework(war, PlaintextTests.connectionProperties());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            kafkaStopServer(server);
        } finally {
            KafkaUtils.deleteKafkaTopics(PlaintextTests.getAdminClient());
        }
    }

}
