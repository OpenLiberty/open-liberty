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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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
     * Save the server configuration before each test, this should be the default server
     * configuration.
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);
    }

    /**
     * Restore the server configuration to the default state after each test.
     *
     * @throws Exception
     */
    @After
    public void afterTest() throws Exception {
        // Restore the server to the default state.
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    /**
     * The test will check the default value of soReuseAddr by searching the trace file.
     *
     * The default value of soReuseAddr is true.
     */
    @Test
    public void testSoReuseAddr_default() throws Exception {
        // Validate that soReuseAddr default is true.
        assertNotNull("The default value of soReuseAddr was not true!", server.waitForStringInTrace("soReuseAddr: true"));
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
    @Mode(TestMode.FULL)
    public void testSoReuseAddr_nonDefault() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setSoReuseAddr(false);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate that soReuseAddr is set to false.
        assertNotNull("The configured value of soReuseAddr was not false!", server.waitForStringInTraceUsingMark("soReuseAddr: false"));
    }
}
