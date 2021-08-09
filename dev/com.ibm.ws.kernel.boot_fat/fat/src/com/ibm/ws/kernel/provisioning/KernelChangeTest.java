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
package com.ibm.ws.kernel.provisioning;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class KernelChangeTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.bootstrap.fat");

    @Rule
    public TestName testName = new TestName();

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testKernelChangeRestart() throws Exception {
        // Start the server (wait for server started message)
        server.startServer("part1.console.log");

        // Make sure no error messages in console.log
        List<String> output = server.findStringsInLogs("ERROR.*", server.getConsoleLogFile());
        assertTrue("We should not see error messages in the log. output=" + output, output.isEmpty());

        // Stop the server (WARM)
        server.stopServer(false);

        // Save logs, switch bootstrap.properties files to enable hpel
        server.renameLibertyServerRootFile("logs/messages.log", "logs/part1.messages.log");
        server.renameLibertyServerRootFile("logs/trace.log", "logs/part1.trace.log");
        server.renameLibertyServerRootFile("bootstrap.properties", "bootstrap.properties.orig");
        server.renameLibertyServerRootFile("bootstrap.properties.hpel", "bootstrap.properties");

        // Start the server with HPEL (WARM)
        server.startServer("part2.console.log", false, false);

        // Make sure no error messages in console.log
        output = server.findStringsInLogs("ERROR.*", server.getConsoleLogFile());
        assertTrue("We should not see error messages in the log. output=" + output, output.isEmpty());

        // Stop the server (WARM)
        server.stopServer(false);

        // Save logs, switch back to disable hpel again
        server.renameLibertyServerRootFile("bootstrap.properties", "bootstrap.properties.hpel");
        server.renameLibertyServerRootFile("bootstrap.properties.orig", "bootstrap.properties");

        // Start the server WITHOUT HPEL again 
        server.startServer("part3.console.log", false, false);

        // Make sure no error messages in console.log
        output = server.findStringsInLogs("ERROR.*", server.getConsoleLogFile());
        assertTrue("We should not see error messages in the log. output=" + output, output.isEmpty());
    }

}
