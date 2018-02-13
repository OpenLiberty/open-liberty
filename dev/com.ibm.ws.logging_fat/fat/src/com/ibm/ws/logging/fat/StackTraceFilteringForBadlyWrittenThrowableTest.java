/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServerFactory;

public class StackTraceFilteringForBadlyWrittenThrowableTest extends AbstractStackTraceFilteringTest {

    private static final String EXPECTED_EXCEPTION = "BadlyWrittenException";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.brokenserver");
        server.startServer();
        ShrinkHelper.defaultDropinApp(server, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");

        // Hit the servlet, to drive the error
        hitWebPage("broken-servlet", "BrokenWithABadlyWrittenThrowableServlet", true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            // The actual exception does not identify the class, SRVE0777E: Exception thrown by application class 'unknown.unknown:unknown'
            server.stopServer(EXPECTED_EXCEPTION, "SRVE0777E: Exception thrown by application class");
        }
    }

    @Test
    public void testConsoleDoesNotNPEForPrintedException() throws Exception {
        assertConsoleLogContains("The console log did not have our exception in it at all.",
                                 EXPECTED_EXCEPTION);
        assertMessagesLogDoesNotContain("The console log had an NPE in it.", "NullPointerException");

    }

    @Test
    public void testMessagesIsNotTrimmedForPrintedException() throws Exception {
        assertMessagesLogContains("The messages log did not have our exception in it at all.",
                                  EXPECTED_EXCEPTION);
        assertMessagesLogDoesNotContain("The messages log had a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
        // We better have a line for the class that threw the exception
        assertMessagesLogContains("The console stack didn't show the originating class.",
                                  "at com.ibm.ws.logging.fat.broken.servlet.BrokenWithABadlyWrittenThrowableServlet.doGet");
        // We also want at least one line about javax.servlet
        assertMessagesLogContains("The console stack was trimmed too aggressively.",
                                  "at javax.servlet.http.HttpServlet.service");
        assertMessagesLogContains("The console stack was apparently trimmed, but internal WAS classes got left in it",
                                  "at com.ibm.ws.webcontainer");
    }

    @Test
    public void testTraceIsNotTrimmedForPrintedException() throws Exception {
        assertTraceLogContains("The trace log did not have our exception in it at all.",
                               EXPECTED_EXCEPTION);
        assertTraceLogDoesNotContain("The trace log had a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
    }
}
