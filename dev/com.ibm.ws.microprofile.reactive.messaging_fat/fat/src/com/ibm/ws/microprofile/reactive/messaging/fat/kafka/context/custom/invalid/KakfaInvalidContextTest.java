/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context.custom.invalid;

import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.reactive.messaging.fat.AppValidator;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PlaintextTests;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;
import com.ibm.ws.microprofile.reactive.messaging.kafka.KafkaConnectorConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test invalid context service configuration
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class KakfaInvalidContextTest extends FATServletClient {
    public static final String SERVER_NAME = "CustomContextRxMessagingServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30,
                                                                  ReactiveMessagingActions.MP20_RM10);

    @BeforeClass
    public static void setup() throws Exception {
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

    @Test
    @AllowedFFDC
    public void testInvalidContextService() throws Exception {

        ConnectorProperties config = ConnectorProperties.simpleIncomingChannel(PlaintextTests.connectionProperties(),
                                                                               KafkaInvalidContextBean.CHANNEL_NAME,
                                                                               "invalid")
                        .addProperty(KafkaConnectorConstants.CONTEXT_SERVICE, "invalid-context-service");

        AppValidator.validateAppOn(server)
                        .withAppName("testInvalidContextService")
                        .withClass(KafkaInvalidContextBean.class)
                        .withAppConfig(config)
                        .withLibrary(kafkaClientLibs())
                        .withManifestResource(kafkaPermissions(), "permissions.xml")
                        .failsWith("CWMRX1200E: .*" + KafkaInvalidContextBean.CHANNEL_NAME + ".*invalid-context-service")
                        .run();
        // CWMRX1200E: The {0} channel is configured to use the {1} context service, but no such context service was found.
    }

}
