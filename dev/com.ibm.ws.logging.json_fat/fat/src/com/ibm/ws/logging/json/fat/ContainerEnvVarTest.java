/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.json.fat;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Check CONTAINER_HOST and CONTAINER_NAME env vars
 */
@RunWith(FATRunner.class)
public class ContainerEnvVarTest {

    private static final String MESSAGE_LOG = "logs/messages.log";

    public static final String SERVER_CONTAINER_NAME = "com.ibm.ws.logging.json.ContainerNameEnvServer";
    public static final String SERVER_CONTAINER_HOST = "com.ibm.ws.logging.json.ContainerHostEnvServer";

    @Server(SERVER_CONTAINER_NAME)
    public static LibertyServer server_container_name;

    @Server(SERVER_CONTAINER_HOST)
    public static LibertyServer server_container_host;

    private static LibertyServer serverInUse;

    public void setUp(LibertyServer server) throws Exception {
        serverInUse = server;
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (serverInUse != null && serverInUse.isStarted()) {
            serverInUse.stopServer("com.ibm.ws.logging.fat.ffdc.servlet.FFDCServlet.doGet", "ArithmeticException",
                                   "CWWKG0081E", "CWWKG0083W");
        }
    }

    /*
     * This test sets the CONTAINER_NAME variable in the server.env and checks if it has been successfully set in the JSON logs
     */
    @Test
    public void testContainerNameEnvVar() throws Exception {
        setUp(server_container_name);
        List<String> lines = server_container_name.findStringsInFileInLibertyServerRoot("TEST_SERVER_NAME", MESSAGE_LOG);
        assertTrue("The JSON field ibm_serverName was not set with the value of env var CONTAINER_NAME.", lines.size() > 0);
    }

    /*
     * This test sets the CONTAINER_HOST variable in the server.env and checks if it has been successfully set in the JSON logs
     */
    @Test
    public void testContainerHostEnvVar() throws Exception {
        setUp(server_container_host);
        List<String> lines = server_container_host.findStringsInFileInLibertyServerRoot("TEST_HOST_NAME", MESSAGE_LOG);
        assertTrue("The JSON field host was not set with the value of env var CONTAINER_HOST.", lines.size() > 0);
    }

}
