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
 * Test to ensure that the tcpOptions soLinger works.
 */
@RunWith(FATRunner.class)
public class SoLingerTests {

    private static final String CLASS_NAME = SoLingerTests.class.getName();
    static final Logger LOG = Logger.getLogger(CLASS_NAME);

    @Server("SoLinger")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        server.startServer(SoLingerTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        // CWWKG0011W: The configuration validation did not succeed. Value "-2" is out of range.
        // CWWKG0011W: The configuration validation did not succeed. Value "65536" is out of range.
        // CWWKG0083W: A validation failure occurred while processing the [soLinger] property, value = [-2]. Default value in use: [-1].
        // CWWKG0083W: A validation failure occurred while processing the [soLinger] property, value = [65536]. Default value in use: [-1].
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKG0011W", "CWWKG0083W");
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
     * The test will check the default value of soLinger by searching the trace file.
     *
     * The default value of soLinger is -1.
     */
    @Test
    public void testSoLinger_default() throws Exception {
        // Validate that soLinger default is -1.
        assertNotNull("The default value of soLinger was not -1!", server.waitForStringInTrace("soLinger: -1"));
    }

    /**
     * The test will set soLinger to a value of 20 and validate in the trace file that
     * the correct value is being used.
     *
     * The below configuration will be used to set soLinger to 20:
     * <tcpOptions soLinger="20"/>
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSoLinger_nonDefault() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setSoLinger(20);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate that soLinger is set to 20.
        assertNotNull("The configured value of soLinger was not 20!", server.waitForStringInTraceUsingMark("soLinger: 20"));
    }

    /**
     * The test will set soLinger to a value of -2 and validate an error occurs since
     * -2 is lower than the minimum allowed value of -1.
     *
     * The below configuration will be used to set soLinger to 20:
     * <tcpOptions soLinger="-1"/>
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSoLinger_tooLow() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setSoLinger(-2);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // CWWKG0011W: The configuration validation did not succeed. Value "-2" is out of range.
        assertNotNull("The CWWKG0011W message was not found in the logs!", server.waitForStringInLogUsingMark("CWWKG0011W"));

        // Validate that soLinger set to -2 causes a validation error.
        assertNotNull("The configured value of soLinger (-2) did not cause a validation error!", server.waitForStringInLogUsingMark("CWWKG0083W"));
    }

    /**
     * The test will set soLinger to a value of 65536 and validate an error occurs since
     * 65536 is higher than the maximum allowed value of 65535.
     *
     * The below configuration will be used to set soLinger to 20:
     * <tcpOptions soLinger="65536"/>
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSoLinger_tooHigh() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setSoLinger(65536);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // CWWKG0011W: The configuration validation did not succeed. Value "65536" is out of range.
        assertNotNull("The CWWKG0011W message was not found in the logs!", server.waitForStringInLogUsingMark("CWWKG0011W"));

        // Validate that soLinger set to 65536 causes a validation error.
        assertNotNull("The configured value of soLinger (65536) did not cause a validation error!", server.waitForStringInLogUsingMark("CWWKG0083W"));
    }
}
