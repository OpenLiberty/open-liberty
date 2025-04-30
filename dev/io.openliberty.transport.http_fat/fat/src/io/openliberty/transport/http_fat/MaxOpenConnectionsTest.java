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

import java.net.Socket;
import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test to ensure that the tcpOptions maxOpenConnections works.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MaxOpenConnectionsTest {

    static final Logger LOG = Logger.getLogger(MaxOpenConnectionsTest.class.getName());

    @Server("MaxOpenConnections")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        server.startServer(MaxOpenConnectionsTest.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            // CWWKO0222W: TCP Channel defaultHttpEndpoint has exceeded the maximum number of open connections 2.
            server.stopServer("CWWKO0222W");
        }

    }

    /**
     * The test will set maxOpenConnections to 2 and validate the value by searching the trace file.
     *
     * Three connections are then created. Since the number of connections is 1 greater than maxOpenConnections
     * the following exception should be found in the logs:
     * CWWKO0222W: TCP Channel defaultHttpEndpoint has exceeded the maximum number of open connections 2.
     *
     * The below configuration will be used to set maxOpenConnections to 2:
     * <tcpOptions maxOpenConnections="2"/>
     *
     */
    @Test
    public void testMaxOpenConnections() throws Exception {
        URL url = HttpUtils.createURL(server, "/");

        // Set maxOpenConnections to 2.
        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setMaxOpenConnections(new Integer(2));

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate that maxOpenConnections is set to 2.
        assertNotNull("The configured value of maxOpenConnections was not 2!", server.waitForStringInTraceUsingMark("maxOpenConnections: 2"));

        // Ensure the TCP Channel has started.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv4) port 8010.
        assertNotNull("The TCP Channel was not started!", server.waitForStringInLogUsingMark("CWWKO0219I"));

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
            LOG.info("Exception occurred when creating Sockets: " + e.toString());
        } finally {
            // We should get an exception for the third connection since the server has maxOpenConnections set to 2.
            // Wait for the message in the logs before closing the sockets to ensure they are not closed prematurely.
            assertNotNull("The CWWKO0222W message was not found in the logs!", server.waitForStringInLogUsingMark("CWWKO0222W"));

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

            // Restore the server to the default state.
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(null);
        }
    }

}
