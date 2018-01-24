/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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

public class StackTraceFilteringForPrintedExceptionWithIBMCodeAtTopTest extends AbstractStackTraceFilteringTest {

    private static final String EXPECTED_EXCEPTION = "javax.naming.NamingException";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.brokenserver");
        server.startServer();
        ShrinkHelper.defaultDropinApp(server, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");

        // Hit the servlet, to drive the error
        hitWebPage("broken-servlet", "IBMCodeAtTopExceptionPrintingServlet", false);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testConsoleIsTrimmedForPrintedExceptionThrownByIBMCode() throws Exception {
        assertConsoleLogContains("The console log did not have our exception in it at all.",
                                 EXPECTED_EXCEPTION);
        assertConsoleLogContains("The console stack was not trimmed.",
                                 INTERNAL_CLASSES_REGEXP);
        // We better have a line for the class that threw the exception
        assertConsoleLogContains("The console stack didn't show the originating class.",
                                 "at com.ibm.ws.logging.fat.broken.servlet.IBMCodeAtTopExceptionPrintingServlet.doGet");
        // We also want at least one line about javax.servlet
        assertConsoleLogContains("The console stack was trimmed too aggressively.",
                                 "at javax.servlet.http.HttpServlet.service");
        // We only want one line of internal WAS classes in the console
        int traceCount = server.findStringsInFileInLibertyServerRoot(EXPECTED_EXCEPTION, CONSOLE_LOG).size();
        assertConsoleLogCountEquals("The console stack was apparently trimmed, but internal WAS classes got left in it",
                                    "at com.ibm.ws.webcontainer", traceCount);

    }

    @Test
    public void testMessagesIsNotTrimmedForPrintedExceptionThrownByIBMCode() throws Exception {
        assertMessagesLogContains("The messages log did not have our exception in it at all.",
                                  EXPECTED_EXCEPTION);
        assertMessagesLogDoesNotContain("The messages log had a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
    }

    @Test
    public void testTraceIsNotTrimmedForPrintedExceptionThrownByIBMCode() throws Exception {
        assertTraceLogContains("The trace log did not have our exception in it at all.",
                               EXPECTED_EXCEPTION);
        assertTraceLogDoesNotContain("The trace log had a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
    }
}
