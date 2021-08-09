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
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class StackTraceFilteringForNoClassDefFoundErrorTest extends AbstractStackTraceFilteringTest {

    private static final String MAIN_EXCEPTION = "NoClassDefFoundError";
    private static final String NESTED_EXCEPTION = "ClassNotFoundException";
    private static final String CAUSED_BY = "Caused by:";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.missingfeatureserver", StackTraceFilteringForNoClassDefFoundErrorTest.class);
        ShrinkHelper.defaultDropinApp(server, "missing-feature-servlet", "com.ibm.ws.logging.fat.missing.feature.servlet");
        server.startServer();

        hitWebPage("missing-feature-servlet", "MissingEntityManagerServlet", true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(MAIN_EXCEPTION, "com.ibm.ws.logging.fat.missing.feature.servlet.MissingEntityManagerServlet.doGet");
        }
    }

    @Test
    public void testConsoleIsTrimmedForNoClassDefFoundError() throws Exception {
        assertConsoleLogContains("The console log did not have our exception in it at all.",
                                 MAIN_EXCEPTION);
        assertConsoleLogContains("The console stack was not trimmed.",
                                 INTERNAL_CLASSES_REGEXP);
        // We better have a line for the class that threw the exception, but don't make too many assumptions about what class that was
        assertConsoleLogContains("The console stack didn't show the originating class.",
                                 "at com.ibm.ws.logging");
        // We only want one line of WAS context
        assertConsoleLogCountEquals("The console stack was apparently trimmed, but internal WAS classes got left in it",
                                    "at com.ibm.ws.webcontainer", 1);

    }

    @Test
    public void testRedundantCauseIsStrippedOutForNoClassDefFoundError() throws Exception {
        assertConsoleLogContains("The console log should always have our exception in it.",
                                 MAIN_EXCEPTION);
        assertConsoleLogContains("The console log should have a line saying trimming happened.", INTERNAL_CLASSES_REGEXP);
        assertConsoleLogDoesNotContain("The console log should not have anything about \"Caused by\"",
                                       CAUSED_BY);
        assertConsoleLogDoesNotContain("The console log should not have our nested exception in it.",
                                       NESTED_EXCEPTION);
    }

    @Test
    public void testMessagesIsNotTrimmedForNoClassDefFoundError() throws Exception {
        assertMessagesLogContains("The messages log should have our exception in it.",
                                  MAIN_EXCEPTION);
        assertMessagesLogDoesNotContain("The messages log should not have a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
        assertMessagesLogContains("The messages log should have a 'Caused by' line in it.",
                                  CAUSED_BY);
        assertMessagesLogContains("The message log should have our nested exception in it.",
                                  NESTED_EXCEPTION);
    }

    @Test
    public void testTraceIsNotTrimmedForNoClassDefFoundError() throws Exception {
        assertTraceLogContains("The trace log should not have our exception in it.",
                               MAIN_EXCEPTION);
        assertTraceLogDoesNotContain("The trace log had a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
        assertTraceLogContains("The trace log should have a 'Caused by' line in it.",
                               CAUSED_BY);
        assertTraceLogContains("The trace log should not have our nested exception in it.",
                               NESTED_EXCEPTION);
    }
}
