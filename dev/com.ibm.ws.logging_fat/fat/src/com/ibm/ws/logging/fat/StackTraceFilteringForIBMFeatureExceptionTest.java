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

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServerFactory;

public class StackTraceFilteringForIBMFeatureExceptionTest extends AbstractStackTraceFilteringTest {

    private static final String MAIN_EXCEPTION = "ConfigurationReceivedException";
    private static final String BUNDLE_NAME = "test.configuration.fallalloverthefloor.ibmfeature";
    private static final String FEATURE_NAME = "unconfigurableIbmFeature-1.0";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.badconfig.ibm");

        // install our user feature
        server.installUserBundle(BUNDLE_NAME); // NO HYPHENS! NO ".jar" SUFFIX!
        server.installUserFeature(FEATURE_NAME); // NO UNDERSCORES! NO ".mf" SUFFIX!

        // Just starting the server should be enough to get exceptions
        server.startServer();
        // ... but to be safe, wait until we know the config has been driven
        String successMessage = server.waitForStringInLog("The user feature is about to throw an exception.");
        assertNotNull("The user feature should have produced a message saying it was active and about to fall all over the floor.", successMessage);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(MAIN_EXCEPTION);
        server.uninstallUserBundle(BUNDLE_NAME);
        server.uninstallUserFeature(FEATURE_NAME);
    }

    @Test
    public void testConsoleIsTrimmedForNastyInternalErrorFromIBMFeature() throws Exception {
        assertConsoleLogContains("The console log should at the very least have our exception in it.", MAIN_EXCEPTION);
        assertConsoleLogCountEquals("The console stack should only have one [internal classes] in it.",
                                    INTERNAL_CLASSES_REGEXP, 1);
        // The other methods from the user feature should still be in the stack trace
        assertConsoleLogContains("The console log should have frames from the mock-IBM classes in it.", "reallyThrowAnException");
        assertConsoleLogDoesNotContain("The console log should not have more than one frames from the mock-IBM classes in it.", "thinkAboutThrowingAnException");

        // We should have no SCR stuff, since SCR is calling a 'Liberty' feature
        assertConsoleLogDoesNotContain("The SCR classes should not be in the console log",
                                       "at org.apache.felix.scr.impl");
        // Similarly, the Java frames are just calls to Liberty code from SCR code so should be stripped
        assertConsoleLogDoesNotContain("The console stack should not have any JVM frames in it.",
                                       "at java.");

    }

    @Test
    public void testMessagesIsNotTrimmedForNastyInternalErrorFromIBMFeature() throws Exception {
        assertMessagesLogContains("The messages log should have our exception in it.",
                                  MAIN_EXCEPTION);
        assertMessagesLogContains("The console stack should have the scr packages we think our stack trace has in it",
                                  "at org.apache.felix.scr.impl");
        assertMessagesLogDoesNotContain("The messages log should not have a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
    }
}
