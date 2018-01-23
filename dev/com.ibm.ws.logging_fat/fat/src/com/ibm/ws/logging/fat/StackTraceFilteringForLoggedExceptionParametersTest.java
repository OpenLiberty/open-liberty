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

public class StackTraceFilteringForLoggedExceptionParametersTest extends AbstractStackTraceFilteringTest {

    private static final String SPECIAL_BROKEN_EXCEPTION = "SpecialBrokenException";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.brokenserver");

        server.startServer();
        ShrinkHelper.defaultDropinApp(server, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");

        // Hit the servlet, to drive the error
        hitWebPage("broken-servlet", "BrokenServlet", true);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(SPECIAL_BROKEN_EXCEPTION, "com.ibm.ws.logging.fat.broken.servlet.BrokenServlet.doGet");
        }
    }

    @Test
    public void testConsoleIsTrimmedForLoggedParameter() throws Exception {
        assertConsoleLogContains("The console log did not have our exception in it at all.",
                                 SPECIAL_BROKEN_EXCEPTION);
        assertConsoleLogContains("The console stack was not trimmed.",
                                 INTERNAL_CLASSES_REGEXP);
        // We better have a line for the class that threw the exception
        assertConsoleLogContains("The console stack was trimmed too aggressively and stripped out our servlet.",
                                 "at com.ibm.ws.logging.fat.broken.servlet.BrokenServlet.doGet");
        // We also want at least one line about javax.servlet
        assertConsoleLogContains("The console stack was was trimmed too aggressively and stripped out the API we're using",
                                 "at javax.servlet.http.HttpServlet.service");

        // We don't want to be seeing anything that looks like internal WAS classes in the console
        assertConsoleLogDoesNotContain("The console stack was apparently trimmed, but internal WAS classes got left in it",
                                       "at com.ibm.ws.webcontainer.servlet.ServletWrapper.handleRequest");
    }

    @Test
    public void testMessagesIsNotTrimmedForLoggedParameter() throws Exception {
        assertMessagesLogContains("The messages log did not have our exception in it at all.",
                                  SPECIAL_BROKEN_EXCEPTION);
        assertMessagesLogDoesNotContain("The messages log had a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
        // We don't want to be seeing anything that looks like internal WAS classes in the console
        assertMessagesLogContains("The messages stack was apparently untrimmed, but it didn't have the internal WAS class stacks we expected in it",
                                  "at com.ibm.ws.webcontainer.servlet.ServletWrapper.handleRequest");
    }

    @Test
    public void testTraceIsNotTrimmedForLoggedParameter() throws Exception {
        assertTraceLogContains("The trace log did not have our exception in it at all.",
                               SPECIAL_BROKEN_EXCEPTION);
        assertTraceLogDoesNotContain("The trace log had a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
    }
}
