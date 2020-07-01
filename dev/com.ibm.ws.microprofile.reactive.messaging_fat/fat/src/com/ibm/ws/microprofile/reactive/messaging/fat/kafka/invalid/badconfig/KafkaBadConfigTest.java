/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.invalid.badconfig;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaPermissions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.kafka.KafkaConnectorConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test that when invalid config is given to Kafka, an error is printed in the log.
 * <p>
 * Also test that if creation.retry.seconds set, the creation of the KafkaConsumer is retried. This is specifically to cope with the case where, in a Kubernetes environment, the
 * hostname of the kafka broker does not resolve at startup.
 */
@RunWith(FATRunner.class)
public class KafkaBadConfigTest {

    private static final String APP_NAME = "KafkaBadConfig";
    private static final String APP_GROUP_ID = "bad-config-test-group";

    @Server("SimpleRxMessagingServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        server.stopServer("CWMRX1007E", // Expected error message
                          "CWWKZ000[1-4]", // Generic "Exception starting app" messages
                          "CWMRX1009W", // Connector initialization failed but will be retried message
                          "CWMRX1008E", // Expected outgoing error message
                          "CWMRX1010W" // Outgoing connector initialization failed by will be retried message
        );
    }

    @Test
    @AllowedFFDC
    public void testBadConfig() throws Exception {

        // Invalid config because bootstrap.servers not set
        ConnectorProperties incomingProperties = simpleIncomingChannel("", KafkaBadConfigIncomingBean.CHANNEL_NAME, APP_GROUP_ID);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .include(incomingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(KafkaUtils.kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClass(KafkaBadConfigIncomingBean.class)
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        server.setMarkToEndOfLog();
        // Deploy the application
        ShrinkHelper.exportToServer(server, "dropins", war, SERVER_ONLY);

        // Wait for the app either to start, or to fail to start
        String logLine = server.waitForStringInLogUsingMark("CWWKZ000[1-4].*" + APP_NAME);
        assertNotNull("Application startup didn't complete - app startup line not found", logLine);

        // Check that the bad config error was emitted
        List<String> configErrorLines = server.findStringsInLogsUsingMark("CWMRX1007E:", server.getDefaultLogFile());
        assertThat(configErrorLines, hasSize(1));
        String configErrorLine = configErrorLines.get(0);
        // ...and that it contained the channel name
        assertThat(configErrorLine, containsString(KafkaBadConfigIncomingBean.CHANNEL_NAME));

        // Check that the failure was not retried
        List<String> retryLines = server.findStringsInLogsUsingMark("CWMRX1009W", server.getDefaultLogFile());
        assertThat(retryLines, is(empty()));
    }

    @Test
    @AllowedFFDC
    public void testBadConfigRetry() throws Exception {
        // Invalid config because bootstrap.servers not set, but creation retry enabled
        ConnectorProperties incomingProperties = simpleIncomingChannel("", KafkaBadConfigIncomingBean.CHANNEL_NAME, APP_GROUP_ID)
                        .addProperty(KafkaConnectorConstants.CREATION_RETRY_SECONDS, "5");

        PropertiesAsset appConfig = new PropertiesAsset()
                        .include(incomingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(KafkaUtils.kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClass(KafkaBadConfigIncomingBean.class)
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        server.setMarkToEndOfLog();
        // Deploy the application
        ShrinkHelper.exportToServer(server, "dropins", war, SERVER_ONLY);

        // Wait for the app either to start, or to fail to start
        String logLine = server.waitForStringInLogUsingMark("CWWKZ000[1-4].*" + APP_NAME);
        assertNotNull("Application startup didn't complete - app startup line not found", logLine);

        // Check that the bad config error was emitted
        List<String> configErrorLines = server.findStringsInLogsUsingMark("CWMRX1007E:", server.getDefaultLogFile());
        assertThat(configErrorLines, hasSize(1));
        String configErrorLine = configErrorLines.get(0);
        // ...and that it contained the channel name
        assertThat(configErrorLine, containsString(KafkaBadConfigIncomingBean.CHANNEL_NAME));

        // Check that the failure was retried
        List<String> retryLines = server.findStringsInLogsUsingMark("CWMRX1009W:", server.getDefaultLogFile());
        assertThat(retryLines, not(empty()));
        for (String retryLine : retryLines) {
            // placeholder in message should be replaced by channel name
            assertThat(retryLine, containsString(KafkaBadConfigIncomingBean.CHANNEL_NAME));
        }
    }

    @Test
    @AllowedFFDC
    public void testBadConfigOutgoing() throws Exception {

        // Invalid config because bootstrap.servers not set
        ConnectorProperties outgoingProperties = ConnectorProperties.simpleOutgoingChannel("", KafkaBadConfigOutgoingBean.CHANNEL_NAME);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .include(outgoingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(KafkaUtils.kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClass(KafkaBadConfigOutgoingBean.class)
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        server.setMarkToEndOfLog();
        // Deploy the application
        ShrinkHelper.exportToServer(server, "dropins", war, SERVER_ONLY);

        // Wait for the app either to start, or to fail to start
        String logLine = server.waitForStringInLogUsingMark("CWWKZ000[1-4].*" + APP_NAME);
        assertNotNull("Application startup didn't complete - app startup line not found", logLine);

        // Check that the bad config error was emitted
        List<String> configErrorLines = server.findStringsInLogsUsingMark("CWMRX1008E:", server.getDefaultLogFile());
        assertThat(configErrorLines, hasSize(1));
        String configErrorLine = configErrorLines.get(0);
        // ...and that it contained the channel name
        assertThat(configErrorLine, containsString(KafkaBadConfigOutgoingBean.CHANNEL_NAME));

        // Check that the failure was not retried
        List<String> retryLines = server.findStringsInLogsUsingMark("CWMRX1010W", server.getDefaultLogFile());
        assertThat(retryLines, is(empty()));
    }

    @Test
    @AllowedFFDC
    public void testBadConfigOutgoingRetry() throws Exception {

        // Invalid config because bootstrap.servers not set
        ConnectorProperties outgoingProperties = ConnectorProperties.simpleOutgoingChannel("", KafkaBadConfigOutgoingBean.CHANNEL_NAME)
                        .addProperty(KafkaConnectorConstants.CREATION_RETRY_SECONDS, "5");

        PropertiesAsset appConfig = new PropertiesAsset()
                        .include(outgoingProperties);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(KafkaUtils.kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClass(KafkaBadConfigOutgoingBean.class)
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        server.setMarkToEndOfLog();
        // Deploy the application
        ShrinkHelper.exportToServer(server, "dropins", war, SERVER_ONLY);

        // Wait for the app either to start, or to fail to start
        String logLine = server.waitForStringInLogUsingMark("CWWKZ000[1-4].*" + APP_NAME);
        assertNotNull("Application startup didn't complete - app startup line not found", logLine);

        // Check that the bad config error was emitted
        List<String> configErrorLines = server.findStringsInLogsUsingMark("CWMRX1008E:", server.getDefaultLogFile());
        assertThat(configErrorLines, hasSize(1));
        String configErrorLine = configErrorLines.get(0);
        // ...and that it contained the channel name
        assertThat(configErrorLine, containsString(KafkaBadConfigOutgoingBean.CHANNEL_NAME));

        // Check that the failure was retried
        List<String> retryLines = server.findStringsInLogsUsingMark("CWMRX1010W:", server.getDefaultLogFile());
        assertThat(retryLines, not(empty()));
        for (String retryLine : retryLines) {
            // placeholder in message should be replaced by channel name
            assertThat(retryLine, containsString(KafkaBadConfigOutgoingBean.CHANNEL_NAME));
        }
    }

}
