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

/**
 * A test which makes sure internal classes exported as spec-type API don't get filtered
 * from the top of stack traces, but do get filtered from the middle.
 * We want to see exceptions like
 *
 * java.lang.NullPointerException
 * at javax.servlet.http.Cookie.isToken(Cookie.java:384)
 * at javax.servlet.http.Cookie.<init>(Cookie.java:124)
 * at com.ibm.ws.logging.fat.broken.servlet.SpecUsingServlet.doGet(SpecUsingServlet.java:40)
 * at javax.servlet.http.HttpServlet.service(HttpServlet.java:575)
 * at javax.servlet.http.HttpServlet.service(HttpServlet.java:668)
 * at com.ibm.ws.webcontainer.servlet.ServletWrapper.service(ServletWrapper.java:1240)
 * at [internal classes]
 */
public class StackTraceFilteringForSpecificationClassesExceptionTest extends AbstractStackTraceFilteringTest {

    private static final String MAIN_EXCEPTION = "IllegalArgumentException";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.brokenserver", StackTraceFilteringForSpecificationClassesExceptionTest.class);
        ShrinkHelper.defaultDropinApp(server, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");
        server.startServer();

        hitWebPage("broken-servlet", "SpecUsingServlet", true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(MAIN_EXCEPTION, "SRVE0777E");
        }
    }

    @Test
    public void testConsoleIsTrimmedForUserUseOfSpecificationClass() throws Exception {
        assertConsoleLogContains("The console log should at the very least have our exception in it.", MAIN_EXCEPTION);
        // How many stack traces we get depends a bit on server internals, so to try and be more robust,
        // count how many [ERROR] lines we get and match this
        // We don't want to count errors that don't have stack traces, so try and exclude these by also checking
        // for message id 'SRVE.*E'. This still isn't totally robust since it won't catch printed Errors
        // if the message doesn't include the id 'SRVE.*E' or misspells it, as our current messages do
        int errorCount = server.findStringsInFileInLibertyServerRoot("ERROR.*SRVE.*E", CONSOLE_LOG).size();
        int causedByCount = server.findStringsInFileInLibertyServerRoot("Caused by", CONSOLE_LOG).size();
        // Sanity check - we got an [ERROR], right?
        assertConsoleLogContains("The console log should have [ERROR] prefix in it", "ERROR");

        assertConsoleLogCountEquals("The console stack should only have one [internal classes] in it per stack trace.",
                                    INTERNAL_CLASSES_REGEXP, errorCount);
        // The javax.servlet methods shouldn't be stripped out, because they're spec used by the app
        final int servletFrames = 7;
        assertConsoleLogCountEquals("The console log should have several frames from the specification javax.servlet classes", "javax.servlet", servletFrames);

        assertConsoleLogContains("The console log should have the user class in it", "SpecUsingServlet");

        // We want one line of internal WAS classes in the console
        assertConsoleLogCountEquals("There should be exactly one IBM frame per stack trace",
                                    "at com.ibm.ws.webcontainer", errorCount + causedByCount);

    }

}
