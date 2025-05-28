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

/**
 * Test to ensure that the tcpOptions soReuseAddr works.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SoReuseAddrTests {

    private static final String CLASS_NAME = SoReuseAddrTests.class.getName();
    static final Logger LOG = Logger.getLogger(CLASS_NAME);

    @Server("SoReuseAddr")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        server.startServer(SoReuseAddrTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * The test will set soReuseAddr to a value of false and validate in the trace file that
     * the correct value is being used.
     *
     * The below configuration will be used to set soReuseAddr to false:
     * <tcpOptions soReuseAddr="false"/>
     *
     * @throws Exception
     */
    @Test
    public void testSoReuseAddr_nonDefault() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setSoReuseAddr(false);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv6) port 8010.
        // We should wait for the TCP Channel to start before checking the trace. If we don't it is possible we try to start shutting down
        // the server before we even have finished starting up causing quiesce issues.
        server.waitForStringInLogUsingMark("CWWKO0219I");

        // Validate that soReuseAddr is set to false.
        assertNotNull("The configured value of soReuseAddr was not false!", server.waitForStringInTraceUsingMark("soReuseAddr: false"));
    }
}
