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

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServerFactory;

public class StackTraceFilteringForUserFeatureExceptionTest extends AbstractStackTraceFilteringTest {

    private static final String MAIN_EXCEPTION = "ConfigurationReceivedException";
    private static final String BUNDLE_NAME = "test.configuration.fallalloverthefloor.userfeature";
    private static final String FEATURE_NAME = "unconfigurableUserFeature-1.0";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.badconfig.user");

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
        server.stopServer();

        server.uninstallUserBundle(BUNDLE_NAME);
        server.uninstallUserFeature(FEATURE_NAME);
    }

    @Test
    public void testConsoleIsTrimmedForNastyInternalErrorFromUserFeature() throws Exception {
        assertConsoleLogContains("The console log should at the very least have our exception in it.", MAIN_EXCEPTION);
        assertConsoleLogCountEquals("The console stack should only have one [internal classes] in it.",
                                    INTERNAL_CLASSES_REGEXP, 1);
        // The other methods from the user feature should still be in the stack trace
        assertConsoleLogContains("The console log should have frames from the user classes in it.", "thinkAboutThrowingAnException");
        assertConsoleLogContains("The console log should have more than one frames from the user classes in it.", "reallyThrowAnException");

        // We should have one line of scr stuff, since it's the last internal line before the java
        // class packages are called, which count as third-party, and in the IBM->third party->user
        // case, the third-party stuff survives
        assertConsoleLogCountEquals("The console stack was apparently trimmed, but the SCR classes got left in it",
                                    "at org.apache.felix.scr.impl", 1);
        // We want a Java line, but only one
        if (JavaInfo.forServer(server).majorVersion() >= 9) {
            assertConsoleLogCountEquals("The console stack should have one Java lines in it.",
                                        "at java.base/java.", 1);
        } else {
            assertConsoleLogCountEquals("The console stack should have one Java lines in it.",
                                        "at java.", 1);
        }

    }

    @Test
    public void testMessagesIsNotTrimmedForNastyInternalErrorFromUserFeature() throws Exception {
        assertMessagesLogContains("The messages log should have our exception in it.",
                                  MAIN_EXCEPTION);
        assertMessagesLogContains("The console stack should have the scr packages we think our stack trace has in it",
                                  "at org.apache.felix.scr.impl");
        assertMessagesLogDoesNotContain("The messages log should not have a trimmed stack trace in it.", INTERNAL_CLASSES_REGEXP);
    }
}
