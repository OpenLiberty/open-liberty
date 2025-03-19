/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.liberty_login.invalid;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.apache.kafka.common.config.SaslConfigs;
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
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.repeats.ReactiveMessagingActions;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.suite.SaslPlainTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;


/**
 * Test the login module with an incorrectly encoded password
 */
@RunWith(FATRunner.class)
public class LibertyLoginModuleInvalidTest {

    private static final String APP_NAME = "kafkaLoginModuleInvalidTest";
    private static final String APP_GROUP_ID = "login-module-invalid-test-group";
    private static final String SERVER_NAME = "SimpleRxMessagingServer";

    private static final String APP_START_CODE = "CWWKZ000[13]I";
    private static final String APP_FAIL_CODE = "CWWKZ000[24]E";
    private static final String INVALID_CIPHER_CODE = "CWWKS1857E";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        KafkaUtils.copyTrustStore(SaslPlainTests.kafkaContainer, server);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        kafkaStopServer(server, INVALID_CIPHER_CODE, APP_FAIL_CODE);
    }

    @AllowedFFDC
    @Test
    public void testInvalidEncodedPassword() throws Exception {

        String testUser = SaslPlainTests.TEST_USER;
        String testSecret = "{aes}wibbleWibbleNotValid";

        // Override SASL_JAAS_CONFIG in the connection props to test the liberty login module
        Map<String, Object> connectionProps = SaslPlainTests.connectionProperties();
        connectionProps.put(SaslConfigs.SASL_JAAS_CONFIG, "com.ibm.ws.kafka.security.LibertyLoginModule required "
                                                          + "username=\"" + testUser + "\" "
                                                          + "password=\"" + testSecret + "\";");

        ConnectorProperties outgoingProperties = simpleOutgoingChannel(connectionProps, InvalidTestBean.CHANNEL_OUT);

        ConnectorProperties incomingProperties = simpleIncomingChannel(connectionProps, InvalidTestBean.CHANNEL_IN, APP_GROUP_ID);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(connectionProps))
                        .include(incomingProperties)
                        .include(outgoingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(InvalidTestBean.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(KafkaTestClientProvider.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        // Now deploy the app and check that it fails to start
        server.setMarkToEndOfLog();
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY, DISABLE_VALIDATION);

        try {
            String logLine = server.waitForStringInLogUsingMark(APP_START_CODE + "|" + APP_FAIL_CODE + "|" + INVALID_CIPHER_CODE);
            assertThat(logLine, containsString(INVALID_CIPHER_CODE));
        } finally {
            server.deleteFileFromLibertyServerRoot("dropins/" + war.getName());
        }
    }

}
