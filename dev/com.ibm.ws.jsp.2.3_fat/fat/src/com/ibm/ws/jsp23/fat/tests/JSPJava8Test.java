/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;

/**
 * JSP 2.3 tests which use Java 1.8 specific features.
 *
 * Tests must only run when Java 1.8 is in use.
 * For this each test should be annotated @MinimumJavaLevel(javaLevel = 8)
 *
 * Tests that just need to drive a simple request using our WebBrowser object can be placed in this class.
 *
 */
// No need to run against cdi-2.0 since these tests don't use CDI at all.
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
public class JSPJava8Test extends LoggingTest {
    private static final String APP_NAME = "TestJSPWithJava8";

    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("jspJava8Server");

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(SHARED_SERVER.getLibertyServer(),
                                      APP_NAME + ".war");

        SHARED_SERVER.getLibertyServer().addInstalledAppForValidation(APP_NAME);
        SHARED_SERVER.startIfNotStarted();
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer();
        }
    }

    /**
     * Simple test for Index.jsp
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testJava8JSP() throws Exception {
        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestJSPWithJava8/Index.jsp", "onetwothreefour");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

}
