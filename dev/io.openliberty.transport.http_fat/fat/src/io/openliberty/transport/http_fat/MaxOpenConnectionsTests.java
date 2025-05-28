/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests to ensure that the tcpOptions maxOpenConnections works.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MaxOpenConnectionsTests {

    private static final String CLASS_NAME = MaxOpenConnectionsTests.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    private static final String NETTY_TCP_CLASS_NAME = "io.openliberty.netty.internal.tcp.TCPUtils";
    private static boolean runningNetty = false;

    @Server("MaxOpenConnections")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        server.startServer(MaxOpenConnectionsTests.class.getSimpleName() + ".log");

        // Go through logs and check if Netty is being used.
        // Wait for the TCP Channel to finish loading and get the TCP Channel started message.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv6) port 8010.
        String tcpChannelMessage = server.waitForStringInLog("CWWKO0219I: TCP Channel defaultHttpEndpoint");
        LOG.info("Endpoint: " + tcpChannelMessage);

        runningNetty = tcpChannelMessage.contains(NETTY_TCP_CLASS_NAME);
        LOG.info("Running Netty? " + runningNetty);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            // CWWKO0222W: TCP Channel defaultHttpEndpoint has exceeded the maximum number of open connections 2.
            // CWWKO0029E: An exception was generated  when initializing chain CHAIN-defaultHttpEndpoint because of exception com.ibm.wsspi.channelfw.exception.ChannelException: A TCP Channel has been constructed with incorrect configuration property value, Channel Name: defaultHttpEndpoint name: maxOpenConnections value: -1 minimum Value: 1 maximum Value: 1280000
            // CWWKO0211E: TCP Channel defaultHttpEndpoint has been constructed with an incorrect configuration property value. Name: maxOpenConnections  Value: -1  Valid Range: Minimum 1, Maximum 1280000
            server.stopServer("CWWKO0222W", "CWWKO0029E", "CWWKO0211E");
        }
    }

    /**
     * Save the server configuration before each test, this should be the default server
     * configuration.
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        server.saveServerConfiguration();
    }

    /**
     * Restore the server configuration to the default state after each test.
     *
     * @throws Exception
     */
    @After
    public void afterTest() throws Exception {
        // Restore the servers to their default state.
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    /**
     * The test will set maxOpenConnections to a value of 2 and validate in the trace file that
     * the correct value is being used.
     *
     * The below configuration will be used to set maxOpenConnections to 2:
     * <tcpOptions maxOpenConnections="2"/>
     *
     * @throws Exception
     */
    @Test
    public void testMaxOpenConnections_not_default() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setMaxOpenConnections(2);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate that maxOpenConnections is set to 2.
        assertNotNull("The configured value of maxOpenConnections was not 2!", server.waitForStringInTraceUsingMark("maxOpenConnections: 2"));
    }

    /**
     * The test will set maxOpenConnections to a value of -1 and validate that is an incorrect configuration.
     *
     * The below configuration will be used to set maxOpenConnections to -1:
     * <tcpOptions maxOpenConnections="-1"/>
     *
     * ExpectedFFDC:
     * Exception = com.ibm.wsspi.channelfw.exception.ChannelException
     * Source = com.ibm.ws.channelfw.internal.ChannelFrameworkImpl.initChainInternal
     * probeid = 2206
     * Stack Dump = com.ibm.wsspi.channelfw.exception.ChannelException: A TCP Channel has been constructed with incorrect configuration property value, Channel Name:
     * defaultHttpEndpoint name: maxOpenConnections value: -1 minimum Value: 1 maximum Value: 1280000
     *
     * ExpectedFFDC:
     * Exception = com.ibm.wsspi.channelfw.exception.ChannelException
     * Source = com.ibm.ws.tcpchannel.internal.TCPChannelConfiguration
     * probeid = 102
     * Stack Dump = com.ibm.wsspi.channelfw.exception.ChannelException: A TCP Channel has been constructed with incorrect configuration property value, Channel Name:
     * defaultHttpEndpoint name: maxOpenConnections value: -1 minimum Value: 1 maximum Value: 1280000
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("com.ibm.wsspi.channelfw.exception.ChannelException")
    @AllowedFFDC("io.openliberty.netty.internal.exception.NettyException")
    public void testMaxOpenConnections_invalid() throws Exception {
        String expectedFFDC;

        if (!runningNetty) {
            expectedFFDC = "com.ibm.wsspi.channelfw.exception.ChannelException";
        } else {
            expectedFFDC = "io.openliberty.netty.internal.exception.NettyException";
        }

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setMaxOpenConnections(-1);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate error messages due to an invalid configuration.
        assertNotNull("CWWKO0211E was not found and should have been!", server.waitForStringInLogUsingMark("CWWKO0211E"));
        assertNotNull("CWWKO0029E was not found and should have been!", server.waitForStringInLogUsingMark("CWWKO0029E"));

        assertTrue("There were not at least two FFDCs created!", server.waitForMultipleStringsInLogUsingMark(2, "FFDC1015I") == 2);

        List<String> ffdcFileNames = server.listFFDCFiles(server.getServerName());

        // There should be 2 FFDCs.
        int numberOfFFDCs = ffdcFileNames.size();
        assertTrue("The number of FFDCs was not 2 but was: " + numberOfFFDCs, numberOfFFDCs == 2);

        // Get the latest two FFDCs.
        RemoteFile ffdcFile1 = server.getFFDCLogFile(ffdcFileNames.get(ffdcFileNames.size() - 1));
        RemoteFile ffdcFile2 = server.getFFDCLogFile(ffdcFileNames.get(ffdcFileNames.size() - 2));

        List<String> lines = server.findStringsInFileInLibertyServerRoot(expectedFFDC, "logs/ffdc/" + ffdcFile1.getName());
        assertTrue("The expected FFDC: " + expectedFFDC + " was not found!", lines.size() > 0);

        lines = server.findStringsInFileInLibertyServerRoot(expectedFFDC, "logs/ffdc/" + ffdcFile2.getName());
        assertTrue("The expected FFDC: " + expectedFFDC + " was not found!", lines.size() > 0);

    }

    /**
     * The test will set maxOpenConnections to a value of 2 and validate in the trace file that
     * the correct value is being used.
     *
     * The below configuration will be used to set maxOpenConnections to 2:
     * <tcpOptions maxOpenConnections="2"/>
     *
     * Three connections are then created. Since the number of connections is 1 greater than maxOpenConnections
     * the following warning should be found in the logs:
     * CWWKO0222W: TCP Channel defaultHttpEndpoint has exceeded the maximum number of open connections 2.
     *
     * @throws Exception
     */
    @Test
    public void testMaxOpenConnections() throws Exception {
        URL url = HttpUtils.createURL(server, "/");

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setMaxOpenConnections(2);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate that maxOpenConnections is set to 2.
        assertNotNull("The configured value of maxOpenConnections was not 2!", server.waitForStringInTraceUsingMark("maxOpenConnections: 2"));

        // Ensure the TCP Channel has started.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv4) port 8010.
        assertNotNull("The TCP Channel was not started!", server.waitForStringInLogUsingMark("CWWKO0219I: TCP Channel defaultHttpEndpoint"));

        // Create three Socket connections.
        Socket socket1 = null;
        Socket socket2 = null;
        Socket socket3 = null;
        try {
            LOG.info("Creating the first connection.");
            socket1 = new Socket(url.getHost(), url.getPort());
            socket1.setKeepAlive(true);

            LOG.info("Creating the second connection.");
            socket2 = new Socket(url.getHost(), url.getPort());
            socket2.setKeepAlive(true);

            LOG.info("Creating the third connection.");
            socket3 = new Socket(url.getHost(), url.getPort());
            socket3.setKeepAlive(true);
        } catch (Exception e) {
            LOG.info("Exception occurred while creating the Sockets: " + e.toString());
        } finally {
            // We should get an exception for the third connection since the server has maxOpenConnections set to 2.
            // Wait for the message in the logs before closing the sockets to ensure they are not closed prematurely.
            try {
                assertNotNull("The CWWKO0222W message was not found in the logs!",
                              server.waitForStringInLogUsingMark("CWWKO0222W: TCP Channel defaultHttpEndpoint has exceeded the maximum number of open connections 2."));
            } finally {
                if (socket1 != null) {
                    LOG.info("Closing the first connection.");
                    socket1.close();
                }
                if (socket2 != null) {
                    LOG.info("Closing the second connection.");
                    socket2.close();
                }
                if (socket3 != null) {
                    LOG.info("Closing the third connection.");
                    socket3.close();
                }
            }
        }
    }

    /**
     * The test will set maxOpenConnections to a value of 2 and validate in the trace file that
     * the correct value is being used.
     *
     * The below configuration will be used to set maxOpenConnections to 2:
     * <tcpOptions maxOpenConnections="2"/>
     *
     * The test will then create a new HttpEndpoint and configure its maxOpenConnections to 1 and verify it is
     * configured correctly.
     *
     * Five connections are then created. Three connections to the defaultHttpEndpoint and two connections to
     * httpEndpoint2. Since the number of connections is 1 greater than maxOpenConnections for each of the httpEndpoints
     * the following warnings should be found in the logs:
     *
     * CWWKO0222W: TCP Channel defaultHttpEndpoint has exceeded the maximum number of open connections 2.
     * CWWKO0222W: TCP Channel httpEndpoint2 has exceeded the maximum number of open connections 1.
     *
     * The main purpose of this test is to ensure that if there are two httpEndpoints that the configuration
     * is unique to each of the httpEndpoints.
     *
     * @throws Exception
     */
    @Test
    public void testMaxOpenConnections_multiple_endpoints() throws Exception {
        URL url = HttpUtils.createURL(server, "/"); // defaultHttpEndpoint
        URL url2 = new URL("http://" + server.getHostname() + ":" + server.getHttpSecondaryPort() + "/"); // httpEndpoint2

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setMaxOpenConnections(2);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate that maxOpenConnections is set to 2.
        assertNotNull("The configured value of maxOpenConnections was not 2!", server.waitForStringInTraceUsingMark("maxOpenConnections: 2"));

        // Ensure the TCP Channel has started.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv4) port 8010.
        assertNotNull("The TCP Channel defaultHttpEndpoint was not started!", server.waitForStringInLogUsingMark("CWWKO0219I: TCP Channel defaultHttpEndpoint"));

        // Add another httpEndpoint and set its maxOpenConnections to 1.
        HttpEndpoint endpoint2 = new HttpEndpoint();
        endpoint2.setId("httpEndpoint2");
        endpoint2.setHttpPort(Integer.toString(server.getHttpSecondaryPort()));
        endpoint2.getTcpOptions().setMaxOpenConnections(1);

        configuration.getHttpEndpoints().add(endpoint2);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate that maxOpenConnections is set to 1.
        assertNotNull("The configured value of maxOpenConnections was not 1!", server.waitForStringInTraceUsingMark("maxOpenConnections: 1"));

        // Ensure the TCP Channel has started.
        assertNotNull("The TCP Channel httpEndpoint2 was not started!", server.waitForStringInLogUsingMark("CWWKO0219I: TCP Channel httpEndpoint2"));

        // Create 3 connections to the defaultHttpEndpoint.
        // Create 2 connections to httpEndpoint2.
        Socket socket1 = null;
        Socket socket2 = null;
        Socket socket3 = null;
        Socket socket4 = null;
        Socket socket5 = null;
        try {
            LOG.info("Creating the first connection to the defaultHttpEndpoint.");
            socket1 = new Socket(url.getHost(), url.getPort());
            socket1.setKeepAlive(true);

            LOG.info("Creating the second connection to the defaultHttpEndpoint.");
            socket2 = new Socket(url.getHost(), url.getPort());
            socket2.setKeepAlive(true);

            LOG.info("Creating the first connection to the httpEndpoint2.");
            socket3 = new Socket(url2.getHost(), url2.getPort());
            socket3.setKeepAlive(true);

            // Exceed the maxOpenConnections for the defaultHttpEndpoint.
            LOG.info("Creating the third connection to the defaultHttpEndpoint.");
            socket4 = new Socket(url.getHost(), url.getPort());

            // Exceed the maxOpenConnections for the httpEndpoint2.
            LOG.info("Creating the second connection to the httpEndpoint2.");
            socket5 = new Socket(url2.getHost(), url2.getPort());
        } catch (Exception e) {
            LOG.info("Exception occurred when creating Sockets: " + e.toString());
        } finally {
            try {
                // Verify maxOpenConnections was exceeded for the defaultHttpEndpoint.
                assertNotNull("The CWWKO0222W message was not found in the logs for the defaultHttpEndpoing!",
                              server.waitForStringInLogUsingMark("CWWKO0222W: TCP Channel defaultHttpEndpoint has exceeded the maximum number of open connections 2."));

                // Verify maxOpenConnections was exceeded for the httpEndpoint2
                assertNotNull("The CWWKO0222W message was not found in the logs for the httpEndpoint2!",
                              server.waitForStringInLogUsingMark("CWWKO0222W: TCP Channel httpEndpoint2 has exceeded the maximum number of open connections 1."));

            } finally {
                // Clean up the Sockets.
                if (socket1 != null) {
                    LOG.info("Closing the first connection to the defaultHttpEndpoint.");
                    socket1.close();
                }
                if (socket2 != null) {
                    LOG.info("Closing the second connection to the defaultHttpEndpoint.");
                    socket2.close();
                }
                if (socket3 != null) {
                    LOG.info("Closing the first connection first connection to the httpEndpoint2.");
                    socket3.close();
                }

                if (socket4 != null) {
                    LOG.info("Closing the third connection to the defaultHttpEndpoint.");
                    socket3.close();
                }

                if (socket5 != null) {
                    LOG.info("Closing the second connection to the httpEndpoint2.");
                    socket3.close();
                }
            }
        }
    }
}
