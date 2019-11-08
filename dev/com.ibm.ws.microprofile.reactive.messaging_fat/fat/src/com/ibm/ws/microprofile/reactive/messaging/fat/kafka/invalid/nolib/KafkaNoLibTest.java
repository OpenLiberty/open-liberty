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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.invalid.nolib;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test with the Kafka lib missing
 */
@RunWith(FATRunner.class)
public class KafkaNoLibTest {

    private static final String APP_NAME = "KafkaNoLib";
    private static final String APP_GROUP_ID = "no-lib-test-group";

    @Server("SimpleRxMessagingServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        server.stopServer("CWMRX1006E", // Expected error message
                          "CWWKZ0002E" // Generic "Exception starting app" message
        );
    }

    @Test
    @AllowedFFDC
    public void testDeploymentFailure() throws Exception {
        ConnectorProperties outgoingProperties = simpleOutgoingChannel("example.com", NoLibMessagingBean.CHANNEL_OUT);

        ConnectorProperties incomingProperties = simpleIncomingChannel("example.com", NoLibMessagingBean.CHANNEL_IN, APP_GROUP_ID);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .include(incomingProperties)
                        .include(outgoingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClass(NoLibMessagingBean.class)
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        server.setMarkToEndOfLog();
        // Deploy the application
        ShrinkHelper.exportToServer(server, "dropins", war, SERVER_ONLY);

        // Wait for either the app to start, or for the expected deployment error
        String logLine = server.waitForStringInLogUsingMark("CWWKZ0001I.*" + APP_NAME + "|" + "CWMRX1006E:");

        // Check that we actually got the expected string
        assertThat(logLine, containsString("CWMRX1006E:"));
    }

}
