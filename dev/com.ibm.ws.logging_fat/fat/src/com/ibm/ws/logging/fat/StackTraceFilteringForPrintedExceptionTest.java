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

import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServerFactory;

public class StackTraceFilteringForPrintedExceptionTest extends AbstractStackTraceFilteringTest {

    private static final String SPECIAL_PRINTED_EXCEPTION = "SpecialPrintingException";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.brokenserver");

        server.startServer();

        //Make sure the application has come up before proceeding
        server.addInstalledAppForValidation("broken-servlet");

        // Hit the servlet, to drive the error
        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);

    }

    @Test
    public void testConsoleIsTrimmedForPrintedException() throws Exception {
        assertConsoleLogContains("The console log did not have our exception in it at all.",
                                 SPECIAL_PRINTED_EXCEPTION);
        assertConsoleLogContains("The console stack was not trimmed.",
                                 INTERNAL_CLASSES_REGEXP);
        // We better have a line for the class that threw the exception
        assertConsoleLogContains("The console stack didn't show the originating class.",
                                 "at com.ibm.ws.logging.fat.servlet.ExceptionPrintingServlet.doGet");
        assertConsoleLogContains("The console stack didn't show the inner originating class.",
                                 "ExceptionGeneratingObject.hashCode");
        // We also want at least one line about javax.servlet
        assertConsoleLogContains("The console stack was trimmed too aggressively.",
                                 "at javax.servlet.http.HttpServlet.service");
        // We only want one line of internal WAS classes in the console
        int traceCount = server.findStringsInFileInLibertyServerRoot(SPECIAL_PRINTED_EXCEPTION, CONSOLE_LOG).size();
        assertConsoleLogCountEquals("The console stack was apparently trimmed, but internal WAS classes got left in it",
                                    "at com.ibm.ws.webcontainer", traceCount);

        // The java.* classes used by the user code should not be trimmed
        if (JavaInfo.forServer(server).majorVersion() >= 9) {
            assertConsoleLogContains("The console stack was trimmed too aggressively of java classes.",
                                     "at java.base/java.util.HashMap.put");
            assertConsoleLogContains("The console stack was trimmed too aggressively of java classes.",
                                     "at java.base/java.util.HashSet.add");
        } else {
            assertConsoleLogContains("The console stack was trimmed too aggressively of java classes.",
                                     "at java.util.HashMap.put");
            assertConsoleLogContains("The console stack was trimmed too aggressively of java classes.",
                                     "at java.util.HashSet.add");
        }

    }

    @Test
    public void testMessagesIsNotTrimmedForPrintedException() throws Exception {
        assertMessagesLogContains("The messages log did not have our exception in it at all.",
                                  SPECIAL_PRINTED_EXCEPTION);
        assertMessagesLogDoesNotContain("The messages log had a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
    }

    @Test
    public void testTraceIsNotTrimmedForPrintedException() throws Exception {
        assertTraceLogContains("The trace log did not have our exception in it at all.",
                               SPECIAL_PRINTED_EXCEPTION);
        assertTraceLogDoesNotContain("The trace log had a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
    }
}
