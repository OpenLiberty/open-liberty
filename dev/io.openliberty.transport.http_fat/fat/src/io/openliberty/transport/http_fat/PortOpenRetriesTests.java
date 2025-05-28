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
 * Test to ensure that the tcpOptions portOpenRetries works.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PortOpenRetriesTests {

    private static final String CLASS_NAME = PortOpenRetriesTests.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    @Server("PortOpenRetries1")
    public static LibertyServer server1;

    @Server("PortOpenRetries2")
    public static LibertyServer server2;

    @BeforeClass
    public static void setup() throws Exception {
        // Start server1 and use the class name so we can find logs easily.
        server1.startServer(PortOpenRetriesTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop server1.
        if (server1 != null && server1.isStarted()) {
            // CWWKG0011W: The configuration validation did not succeed. Value "-1" is out of range.
            // CWWKG0083W: A validation failure occurred while processing the [portOpenRetries] property, value = [-1]. Default value in use: [0].
            server1.stopServer("CWWKG0011W", "CWWKG0083W");
        }

        // Server2 should already have been stopped but let's make sure!
        if (server2 != null && server2.isStarted()) {
            // CWWKO0221E: TCP Channel defaultHttpEndpoint initialization did not succeed.
            // The socket bind did not succeed for host * and port 8010. The port might already be in use.
            // Exception Message: Address already in use: bind
            server2.stopServer("CWWKO0221E");
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
        server1.saveServerConfiguration();

        ServerConfiguration configuration = server1.getServerConfiguration();
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
        server1.setMarkToEndOfLog();
        server1.setTraceMarkToEndOfDefaultTrace();
        server1.restoreServerConfiguration();
        server1.waitForConfigUpdateInLogUsingMark(null);
    }

    /**
     * The test will set portOpenRetries to a value of 60 and validate in the trace file that
     * the correct value is being used.
     *
     * The below configuration will be used to set portOpenRetries to 60:
     * <tcpOptions portOpenRetries="60"/>
     *
     * @throws Exception
     */
    @Test
    public void testPortOpenRetries_nonDefault() throws Exception {
        ServerConfiguration configuration = server1.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setPortOpenRetries(60);

        server1.setMarkToEndOfLog();
        server1.setTraceMarkToEndOfDefaultTrace();
        server1.updateServerConfiguration(configuration);
        server1.waitForConfigUpdateInLogUsingMark(null);

        // Validate that portOpenRetries is set to 60.
        assertNotNull("The configured value of portOpenRetries was not 60!", server1.waitForStringInTraceUsingMark("portOpenRetries: 60"));
    }

    /**
     * The test will set portOpenRetries to a value of -1 and validate that is an incorrect configuration.
     *
     * The below configuration will be used to set portOpenRetries to -1:
     * <tcpOptions portOpenRetries="-1"/>
     *
     * @throws Exception
     */
    @Test
    public void testPortOpenRetries_invalid() throws Exception {
        ServerConfiguration configuration = server1.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setPortOpenRetries(-1);

        server1.setMarkToEndOfLog();
        server1.setTraceMarkToEndOfDefaultTrace();
        server1.updateServerConfiguration(configuration);
        server1.waitForConfigUpdateInLogUsingMark(null);

        // CWWKG0011W: The configuration validation did not succeed. Value "-1" is out of range.
        assertNotNull("The CWWKG0011W was not found in the logs!", server1.waitForStringInLogUsingMark("CWWKG0011W"));

        // CWWKG0083W: A validation failure occurred while processing the [portOpenRetries] property, value = [-1]. Default value in use: [0].
        assertNotNull("The CWWKG0083W was not found in the logs!", server1.waitForStringInLogUsingMark("CWWKG0083W"));
    }

    /**
     * The test uses server2 which defines the following configuration:
     * <tcpOptions portOpenRetries="5"/>
     *
     * The test will start server2 which has not been started yet. Both server1 and server2
     * define the same ports to bind to. The test will then verify that server2 tries to bind to
     * the port that is already in use 5 times and ensure that the server eventually fails to bind
     * to the port and the following message is logged:
     *
     * CWWKO0221E: TCP Channel defaultHttpEndpoint initialization did not succeed.
     * The socket bind did not succeed for host * and port 8010. The port might already be in use.
     * Exception Message: Address already in use: bind
     *
     * @throws Exception
     */
    @Test
    public void testPortOpenRetries() throws Exception {
        // Start server2 and use the class name so we can find logs easily.
        server2.startServer(PortOpenRetriesTests.class.getSimpleName() + ".log");

        // Validate that portOpenRetries is set to 5.
        assertNotNull("The configured value of portOpenRetries was not 5!", server2.waitForStringInTrace("portOpenRetries: 5"));

        // Validate that the port binding was tried 5 times.
        assertNotNull("Attempt 1 to bind did not fail and should have!", server2.waitForStringInTrace("attempt 1 of 6 failed to open the port"));
        assertNotNull("Attempt 2 to bind did not fail and should have!", server2.waitForStringInTrace("attempt 2 of 6 failed to open the port"));
        assertNotNull("Attempt 3 to bind did not fail and should have!", server2.waitForStringInTrace("attempt 3 of 6 failed to open the port"));
        assertNotNull("Attempt 4 to bind did not fail and should have!", server2.waitForStringInTrace("attempt 4 of 6 failed to open the port"));
        assertNotNull("Attempt 5 to bind did not fail and should have!", server2.waitForStringInTrace("attempt 5 of 6 failed to open the port"));

        // Validate that CWWKO0221E was logged.
        assertNotNull("The PortOpenRetries2 server was able to bind successfully but should not have been able to!", server2.waitForStringInLog("CWWKO0221E"));

        if (server2 != null && server2.isStarted()) {
            server2.stopServer("CWWKO0221E");
        }
    }
}
