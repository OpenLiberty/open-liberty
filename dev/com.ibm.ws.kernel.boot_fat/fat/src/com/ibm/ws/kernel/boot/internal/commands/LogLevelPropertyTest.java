/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class LogLevelPropertyTest {

    private static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.bootstrap.output.fat");

        Map<String, String> options = server.getJvmOptionsAsMap();
        String maxPermSize = "-XX:MaxPermSize";
        if (options.containsKey(maxPermSize)) {
            options.remove(maxPermSize);
        }

        // disable serial filter agent message for testing.
        options.put("-Dcom.ibm.websphere.kernel.instrument.serialfilter.message", "false");

        server.setJvmOptions(options);

        server.startServer();

    }

    private static final String LAUNCHING_MESSAGE = "Launching ";
    private static final String MESSAGE_ID = "CWWK";
    private static final String AUDIT_MESSAGE = "AUDIT";
    private static final String INFO_MESSAGE = "INFO";

    @Test
    public void testLogLevelPropertyDisabled() throws Exception {
        // This test used to check for a zero length log file, but various java messages can be printed
        // to the console even when console logging is disabled. We have added so many special cases over the
        // years to deal with this that the test was effectively ignored most of the time.

        // Instead, we now check for several markers that would indicate that we are actually logging console
        // output (eg, the launching message and message IDs.)

        // First, check to see if the console log is empty. If it is, everything is fine.
        if (server.getConsoleLogFile().length() == 0)
            return;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(server.getConsoleLogFile().openForReading()))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                boolean containsMessages = line.contains(LAUNCHING_MESSAGE) || line.contains(MESSAGE_ID) ||
                                           line.contains(AUDIT_MESSAGE) || line.contains(INFO_MESSAGE);
                assertFalse("Message content indicates that console logging may be enabled: " + line, containsMessages);
            }
        }

    }

    @AfterClass
    public static void after() throws Exception {
        server.stopServer();
    }
}