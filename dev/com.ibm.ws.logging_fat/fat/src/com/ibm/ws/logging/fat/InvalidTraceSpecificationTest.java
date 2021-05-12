/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class InvalidTraceSpecificationTest {

    protected static final String MESSAGE_LOG = "logs/messages.log";
    protected static final String INVALIDTRACEMSG = "";
    protected String invalidTraceSpec1 = "com.ibm.someInvalid.*=all";
    protected String invalidTraceSpec2 = "com.ibm.someMoreInvalid.*=all";
    private final String validTraceSpec1 = "com.ibm.ws.config.*=all";
    protected static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.tracespec");
        System.out.println("Starting server)");
        server.startServer();
        System.out.println("Stared server)");
    }

    //Check if we have one Warning message with "com.ibm.someInvalid.*=all"
    protected void checkOnlyOneInvalidTraceSpecEntryExists() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0040I", MESSAGE_LOG);
        assertEquals("Message TRAS0040I not appeared or appeared more than once ", 1, lines.size());
        assertMessagesLogContains(lines.get(0), invalidTraceSpec1);
    }

    protected void checkNoInvalidTraceSpecEntryExists() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0040I", MESSAGE_LOG);
        assertEquals("Message TRAS0040I not appeared or appeared more than once ", 0, lines.size());
    }

    @Test
    public void testInvalidTraceSpec() throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        loggingObj.setTraceSpecification(invalidTraceSpec1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");

        //Test 1: Check if TRAS0040I Message appears for invalid trace spec
        checkOnlyOneInvalidTraceSpecEntryExists();
    }

    /*
     * This test sets valid trace string now.
     */
    @Test
    public void testValidTraceSpec() throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        loggingObj.setTraceSpecification(validTraceSpec1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
        checkNoInvalidTraceSpecEntryExists();

    }

    /*
     * Test 3.
     * - Stop Server
     * - Update Config with invalid trace spec
     * - start server
     * - TRAS0040I shouldn't appear during startup.
     */
    @Test
    public void testInvalidTraceSpecOffline() throws Exception {
        Logging loggingObj;
        server.stopServer();
        //Offline test
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        loggingObj.setTraceSpecification(invalidTraceSpec1);
        server.updateServerConfiguration(serverConfig);
        //Offline test.

        server.startServer();
        checkNoInvalidTraceSpecEntryExists();

        String existingTraceString = loggingObj.getTraceSpecification();
        loggingObj.setTraceSpecification(existingTraceString + ":" + validTraceSpec1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
        checkOnlyOneInvalidTraceSpecEntryExists();

    }

    /*
     * Test 4
     * - Update trace specification with invalidTraceSpec1 and invalidTraceSpec2
     * - Make sure the message TRAS0040I appears for both the invalid strings
     */
    @Test
    public void testMultipleInvalidTraceSpec() throws Exception {
        Logging loggingObj;

        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        loggingObj.setTraceSpecification(invalidTraceSpec1 + ":" + invalidTraceSpec2);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForConfigUpdateInLogUsingMark(null);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0040I", MESSAGE_LOG);
        assertEquals("Expecting multiple invalid trace spec message, but not found ", 1, lines.size());
        assertMessagesLogContains(lines.get(0), invalidTraceSpec1);
        assertMessagesLogContains(lines.get(0), invalidTraceSpec2);
    }

    protected void assertMessagesLogContains(String message, String stringToCheckFor) throws Exception {
        assertFalse(message,
                    server.findStringsInLogs(stringToCheckFor).isEmpty());

    }

    @Before
    public void setup() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
