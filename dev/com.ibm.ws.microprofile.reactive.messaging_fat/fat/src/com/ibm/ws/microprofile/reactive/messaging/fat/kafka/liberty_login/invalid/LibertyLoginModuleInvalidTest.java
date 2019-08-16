/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.liberty_login.invalid;

import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.apache.kafka.common.config.SaslConfigs;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PropertiesAsset;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.SaslPlainTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test the login module with an incorrectly encoded password
 */
@RunWith(FATRunner.class)
public class LibertyLoginModuleInvalidTest {

    private static final String APP_NAME = "kafkaLoginModuleInvalidTest";
    private static final String APP_GROUP_ID = "login-module-invalid-test-group";

    private static final String APP_START_CODE = "CWWKZ000[13]I";
    private static final String APP_FAIL_CODE = "CWWKZ000[24]E";
    private static final String INVALID_CIPHER_CODE = "CWWKS1857E";

    @Server("SimpleRxMessagingServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        KafkaUtils.copyTrustStore(SaslPlainTests.kafkaContainer, server);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(INVALID_CIPHER_CODE, APP_FAIL_CODE);
    }

    @AllowedFFDC
    @Test
    public void testInvalidEncodedPassword() throws Exception {

        String testUser = SaslPlainTests.kafkaContainer.getTestUser();
        String testSecret = "{aes}wibbleWibbleNotValid";

        // Override SASL_JAAS_CONFIG in the connection props to test the liberty login module
        Map<String, Object> connectionProps = KafkaUtils.connectionProperties(SaslPlainTests.kafkaContainer);
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
                        .addPackage(KafkaTestClientProvider.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        // Now deploy the app and check that it fails to start
        server.setMarkToEndOfLog();
        ShrinkHelper.exportArtifact(war, "tmp/apps");
        server.copyFileToLibertyServerRoot("tmp/apps", "dropins", war.getName());

        try {
            String logLine = server.waitForStringInLogUsingMark(APP_START_CODE + "|" + APP_FAIL_CODE + "|" + INVALID_CIPHER_CODE);
            assertThat(logLine, containsString(INVALID_CIPHER_CODE));
        } finally {
            server.deleteFileFromLibertyServerRoot("dropins/" + war.getName());
        }
    }

}
