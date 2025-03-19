/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.liberty_login.xor;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;

import java.util.Map;

import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.suite.SaslPlainTests;
import componenttest.annotation.MaximumJavaLevel;
import org.apache.kafka.common.config.SaslConfigs;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.repeats.ReactiveMessagingActions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Test the login module with xor encoded password
 */
@RunWith(FATRunner.class)
@MaximumJavaLevel(javaLevel = 22)
@Mode(TestMode.FULL)
public class LibertyLoginModuleXorTest {

    private static final String APP_NAME = "kafkaLoginModuleXorTest";
    private static final String APP_GROUP_ID = "login-module-xor-test-group";
    private static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = LibertyLoginModuleXorTestServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {

        String testUser = SaslPlainTests.TEST_USER;
        String testSecret = PasswordUtil.encode(SaslPlainTests.TEST_SECRET, "xor");

        // Override SASL_JAAS_CONFIG in the connection props to test the liberty login module
        Map<String, Object> connectionProps = SaslPlainTests.connectionProperties();
        connectionProps.put(SaslConfigs.SASL_JAAS_CONFIG, "com.ibm.ws.kafka.security.LibertyLoginModule required "
                                                          + "username=\"" + testUser + "\" "
                                                          + "password=\"" + testSecret + "\";");

        ConnectorProperties outgoingProperties = simpleOutgoingChannel(connectionProps, XorTestBean.CHANNEL_OUT);

        ConnectorProperties incomingProperties = simpleIncomingChannel(connectionProps, XorTestBean.CHANNEL_IN, APP_GROUP_ID);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(connectionProps))
                        .addProperty(LibertyLoginModuleXorTestServlet.TEST_USER_PROPERTY, testUser)
                        .addProperty(LibertyLoginModuleXorTestServlet.TEST_SECRET_PROPERTY, testSecret)
                        .include(incomingProperties)
                        .include(outgoingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(LibertyLoginModuleXorTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(KafkaTestClientProvider.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        KafkaUtils.copyTrustStore(SaslPlainTests.kafkaContainer, server);
        server.startServer();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        try {
            kafkaStopServer(server);
        } finally {
            KafkaUtils.deleteKafkaTopics(SaslPlainTests.getAdminClient());
        }
    }

}
