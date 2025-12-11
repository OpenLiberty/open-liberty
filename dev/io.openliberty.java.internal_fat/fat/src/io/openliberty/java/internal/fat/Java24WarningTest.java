/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.java.internal.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 24)
public class Java24WarningTest {

    @Server("warning-test-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    // The entry in the java9.options file
    // --add-exports java.base/sun.security.action=ALL-UNNAMED
    //
    // which can be passed as a parameter into the Liberty JVM for the com.ibm.crypto.ibmkeycert.jar file, 
    // will generate the warning message in the console.log file:
    //     WARNING: package sun.security.action not in java.base
    // when running on Java 24+ because the sun.security.action.* classes were removed in Java 24.  
    // The JVM is just alerting us to the fact we asked it to make an exception for the sun.security.action.* classes, 
    // which could not be found in java.base.  The server script is being updated to check for the Java version being
    // used and will process a java24.option file instead when running on Java 24 or higher.
    //
    // This test fails if the warning message 
    //     WARNING: package sun.security.action not in java.base
    // appears in the console.log on Java 24 or higher
    //
    public void testJava24WarningTest() throws Exception {
        server.waitForStringInLog("server is ready to run a smarter planet");
        assertFalse("Did not find the message \"server is ready to run a smarter planet\" in the console.log file!",
                    server.findStringsInLogs("server is ready to run a smarter planet", server.getConsoleLogFile()).isEmpty());
        assertTrue("Found the message \"WARNING: package sun.security.action not in java.base\" in the console.log file!",
                   server.findStringsInLogs("WARNING: package sun.security.action not in java.base", server.getConsoleLogFile()).isEmpty());
    }
}
