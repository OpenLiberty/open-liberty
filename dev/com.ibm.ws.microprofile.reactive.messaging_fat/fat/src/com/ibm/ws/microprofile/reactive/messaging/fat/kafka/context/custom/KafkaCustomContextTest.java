/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context.custom;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;

import java.util.Arrays;

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
import com.ibm.ws.microprofile.reactive.messaging.kafka.KafkaConnectorConstants;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test context propagation with concurrent enabled and custom context services defined
 */
@RunWith(FATRunner.class)
public class KafkaCustomContextTest extends FATServletClient {
    public static final String APP_NAME = "KafkaDefaultContext";
    public static final String SERVER_NAME = "CustomContextRxMessagingServer";

    @TestServlet(servlet = KafkaCustomContextTestServlet.class, contextRoot = APP_NAME)
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30,
                                                                  ReactiveMessagingActions.MP20_RM10);

    @BeforeClass
    public static void setup() throws Exception {
        ConnectorProperties configuredDefaultInput = simpleIncomingChannel(PlaintextTests.connectionProperties(),
                                                                           KakfaCustomContextTestBean.DEFAULT_IN,
                                                                           APP_NAME + KakfaCustomContextTestBean.DEFAULT_IN);

        ConnectorProperties configuredDefaultOutput = simpleOutgoingChannel(PlaintextTests.connectionProperties(),
                                                                            KakfaCustomContextTestBean.DEFAULT_OUT);

        ConnectorProperties propagateAllInput = simpleIncomingChannel(PlaintextTests.connectionProperties(),
                                                                      KakfaCustomContextTestBean.PROPAGATE_ALL_IN,
                                                                      APP_NAME + KakfaCustomContextTestBean.PROPAGATE_ALL_IN)
                        .addProperty(KafkaConnectorConstants.CONTEXT_SERVICE, "propagateAll");

        ConnectorProperties propagateAllOutput = simpleOutgoingChannel(PlaintextTests.connectionProperties(),
                                                                       KakfaCustomContextTestBean.PROPAGATE_ALL_OUT);

        ConnectorProperties propagateNoneInput = simpleIncomingChannel(PlaintextTests.connectionProperties(),
                                                                       KakfaCustomContextTestBean.PROPAGATE_NONE_IN,
                                                                       APP_NAME + KakfaCustomContextTestBean.PROPAGATE_NONE_IN)
                        .addProperty(KafkaConnectorConstants.CONTEXT_SERVICE, "propagateNone");

        ConnectorProperties propagateNoneOutput = simpleOutgoingChannel(PlaintextTests.connectionProperties(),
                                                                        KakfaCustomContextTestBean.PROPAGATE_NONE_OUT);

        ConnectorProperties propagateAppInput = simpleIncomingChannel(PlaintextTests.connectionProperties(),
                                                                      KakfaCustomContextTestBean.PROPAGATE_APP_IN,
                                                                      APP_NAME + KakfaCustomContextTestBean.PROPAGATE_APP_IN);

        ConnectorProperties propagateAppOutput = simpleOutgoingChannel(PlaintextTests.connectionProperties(),
                                                                       KakfaCustomContextTestBean.PROPAGATE_APP_OUT);

        PropertiesAsset config = new PropertiesAsset()
                        .include(configuredDefaultInput)
                        .include(configuredDefaultOutput)
                        .include(propagateAllInput)
                        .include(propagateAllOutput)
                        .include(propagateNoneInput)
                        .include(propagateNoneOutput)
                        .include(propagateAppInput)
                        .include(propagateAppOutput);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(KafkaCustomContextTestServlet.class.getPackage())
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
