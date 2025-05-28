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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to verify default values of tcpOptions.
 */
@RunWith(FATRunner.class)
public class TcpOptionsDefaultTests {

    private static final String CLASS_NAME = TcpOptionsDefaultTests.class.getName();
    static final Logger LOG = Logger.getLogger(CLASS_NAME);

    @Server("TcpOptionsDefaults")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        server.startServer(TcpOptionsDefaultTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * The test will check the default value of maxOpenConnections by searching the trace file.
     *
     * The default value is 128000.
     *
     * @throws Exception
     */
    @Test
    public void testMaxOpenConnections_default() throws Exception {
        // Validate that maxOpenConnections default is 128000.
        assertNotNull("The default value of maxOpenConnections was not 128000!", server.waitForStringInTrace("maxOpenConnections: 128000"));
    }

    /**
     * The test will check the default value of portOpenRetries by searching the trace file.
     *
     * The default value is 0.
     */
    @Test
    public void testPortOpenRetries_default() throws Exception {
        // Validate that portOpenRetries default is 0.
        assertNotNull("The default value of portOpenRetries was not: 0!", server.waitForStringInTrace("portOpenRetries: 0"));
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
     * The test will check the default value of addressExlcludeList by searching the trace file.
     *
     * The default value of addressExcludeList is null.
     *
     * @throws Exception
     */
    @Test
    public void testAddressExcludeList_default() throws Exception {
        // Validate that addressExcludeList default is null.
        assertNotNull("The default value of addressExcludeList was not null!", server.waitForStringInTrace("addressExcludeList: null"));
    }

    /**
     * The test will check the default value of addressIncludeList by searching the trace file.
     *
     * The default value of addressIncludeList is null.
     *
     * @throws Exception
     */
    @Test
    public void testAddressIncludeList_default() throws Exception {
        // Validate that addressIncludeList default is null.
        assertNotNull("The default value of addressIncludeList was not null!", server.waitForStringInTrace("addressIncludeList: null"));
    }

    /**
     * The test will check the default value of hostNameExcludeList by searching the trace file.
     *
     * The default value of hostNameExcludeList is null.
     *
     * @throws Exception
     */
    @Test
    public void testHostNameExcludeList_default() throws Exception {
        // Validate that hostNameExcludeList default null.
        assertNotNull("The default value of hostNameExcludeList was not null!", server.waitForStringInTrace("hostNameExcludeList: null"));
    }

    /**
     * The test will check the default value of hostNameIncludeList by searching the trace file.
     *
     * The default value of hostNameIncludeList is null.
     *
     * @throws Exception
     */
    @Test
    public void testHostNameIncludeList_default() throws Exception {
        // Validate that hostNameIncludeList default null.
        assertNotNull("The default value of hostNameIncludeList was not null!", server.waitForStringInTrace("hostNameIncludeList: null"));
    }
}
