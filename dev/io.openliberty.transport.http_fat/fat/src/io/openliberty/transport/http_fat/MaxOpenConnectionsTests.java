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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * A simple test to ensure that the tcpOptions maxOpenConnections works.
 */
@RunWith(FATRunner.class)
public class MaxOpenConnectionsTests {

    static final Logger LOG = Logger.getLogger(MaxOpenConnectionsTests.class.getName());

    @Server("MaxOpenConnections")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        server.startServer(MaxOpenConnectionsTests.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            // CWWKO0222W: TCP Channel defaultHttpEndpoint has exceeded the maximum number of open connections 2.
            server.stopServer("CWWKO0222W");
        }

    }

    /*
     * The server used by this test has the following configuration:
     * <tcpOptions maxOpenConnections=2/>
     *
     * This test will validate that if 3 connections are opened that an exception occurs.
     */
    @Test
    public void testMaxOpenConnections() throws Exception {
        URL url = HttpUtils.createURL(server, "/");

        // Create three Socket connections
        Socket socket1 = null;
        Socket socket2 = null;
        Socket socket3 = null;
        try {
            LOG.info("Creating the first connection.");
            socket1 = new Socket(url.getHost(), url.getPort());

            LOG.info("Creating the second connection.");
            socket2 = new Socket(url.getHost(), url.getPort());

            LOG.info("Creating the third connection.");
            socket3 = new Socket(url.getHost(), url.getPort());
        } catch (Exception e) {
            LOG.info("Exception occurred when creating Sockets: " + e.toString());
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

        // We should get an exception for the third connection since the server has maxOpenConnections set to 2.
        assertNotNull(server.waitForStringInLog("CWWKO0222W"));
    }

}
