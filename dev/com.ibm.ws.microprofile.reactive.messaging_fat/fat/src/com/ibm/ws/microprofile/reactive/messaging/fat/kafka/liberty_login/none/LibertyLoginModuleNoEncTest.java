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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.liberty_login.none;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;

import java.util.Map;

import org.apache.kafka.common.config.SaslConfigs;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

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
 * Test the login module with an unencoded password - should still work
 */
@RunWith(FATRunner.class)
public class LibertyLoginModuleNoEncTest {

    private static final String APP_NAME = "kafkaLoginModuleNoEncTest";
    private static final String APP_GROUP_ID = "login-module-noenc-test-group";

    @Server("SimpleRxMessagingServer")
    @TestServlet(contextRoot = APP_NAME, servlet = LibertyLoginModuleNoEncTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        String testUser = SaslPlainTests.kafkaContainer.getTestUser();
        String testSecret = SaslPlainTests.kafkaContainer.getTestSecret();

        // Override SASL_JAAS_CONFIG in the connection props to test the liberty login module
        Map<String, Object> connectionProps = KafkaUtils.connectionProperties(SaslPlainTests.kafkaContainer);
        connectionProps.put(SaslConfigs.SASL_JAAS_CONFIG, "com.ibm.ws.kafka.security.LibertyLoginModule required "
                                                          + "username=\"" + testUser + "\" "
                                                          + "password=\"" + testSecret + "\";");

        ConnectorProperties outgoingProperties = simpleOutgoingChannel(connectionProps, NoEncTestBean.CHANNEL_OUT);

        ConnectorProperties incomingProperties = simpleIncomingChannel(connectionProps, NoEncTestBean.CHANNEL_IN, APP_GROUP_ID);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(connectionProps))
                        .addProperty(LibertyLoginModuleNoEncTestServlet.TEST_USER_PROPERTY, testUser)
                        .addProperty(LibertyLoginModuleNoEncTestServlet.TEST_SECRET_PROPERTY, testSecret)
                        .include(incomingProperties)
                        .include(outgoingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addPackage(LibertyLoginModuleNoEncTestServlet.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(KafkaTestClientProvider.class.getPackage())
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
