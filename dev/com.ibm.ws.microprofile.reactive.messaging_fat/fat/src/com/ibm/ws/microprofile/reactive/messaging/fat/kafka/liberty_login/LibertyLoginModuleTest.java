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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.liberty_login;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;

import java.util.Collections;
import java.util.Map;

import org.apache.kafka.common.config.SaslConfigs;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.utility.Base58;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.SaslPlainTests;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test the login module with an aes encoded password and configured encryption key
 */
@RunWith(FATRunner.class)
public class LibertyLoginModuleTest {

    private static final String APP_NAME = "kafkaLoginModuleTest";
    private static final String APP_GROUP_ID = "login-module-test-group";

    @Server("SimpleRxMessagingServer")
    @TestServlet(contextRoot = APP_NAME, servlet = LibertyLoginModuleTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        String passwordEncKey = Base58.randomString(32);

        String testUser = SaslPlainTests.kafkaContainer.getTestUser();
        String testSecret = PasswordUtil.encode(SaslPlainTests.kafkaContainer.getTestSecret(), "aes", passwordEncKey);

        // Override SASL_JAAS_CONFIG in the connection props to test the liberty login module
        Map<String, Object> connectionProps = KafkaUtils.connectionProperties(SaslPlainTests.kafkaContainer);
        connectionProps.put(SaslConfigs.SASL_JAAS_CONFIG, "com.ibm.ws.kafka.security.LibertyLoginModule required "
                                                          + "username=\"" + testUser + "\" "
                                                          + "password=\"" + testSecret + "\";");

        ConnectorProperties outgoingProperties = simpleOutgoingChannel(connectionProps, AesTestBean.CHANNEL_OUT);

        ConnectorProperties incomingProperties = simpleIncomingChannel(connectionProps, AesTestBean.CHANNEL_IN, APP_GROUP_ID);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(connectionProps))
                        .addProperty(LibertyLoginModuleTestServlet.TEST_USER_PROPERTY, testUser)
                        .addProperty(LibertyLoginModuleTestServlet.TEST_SECRET_PROPERTY, testSecret)
                        .include(incomingProperties)
                        .include(outgoingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(LibertyLoginModuleTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(KafkaTestClientProvider.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        KafkaUtils.copyTrustStore(SaslPlainTests.kafkaContainer, server);
        server.setJvmOptions(Collections.singletonMap("-Dwlp.password.encryption.key", passwordEncKey));
        server.startServer();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        server.stopServer();
    }

}
