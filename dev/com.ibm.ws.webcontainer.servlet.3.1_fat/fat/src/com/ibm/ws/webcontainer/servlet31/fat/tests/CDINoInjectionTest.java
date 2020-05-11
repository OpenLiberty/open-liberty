/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.util.logging.Logger;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * CDI Test
 *
 * Verify that we get an expected response from a CDI server with no injection
 */
@RunWith(FATRunner.class)
@SkipForRepeat(EE9_FEATURES)
public class CDINoInjectionTest extends LoggingTest {
    
    private static final Logger LOG = Logger.getLogger(CDINoInjectionTest.class.getName());

    // Server instance ...
    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_cdiNoInjectionServer");

    private static final String CDI12_TEST_V2_JAR_NAME = "CDI12TestV2";
    private static final String CDI12_TEST_V2_NO_INJECTION_APP_NAME = "CDI12TestV2NoInjection";

    /**
     * Perform a request to the the server instance and verify that the
     * response has expected text. Throw an exception if the expected
     * text is not present or if the unexpected text is present.
     *
     * The request path is used to create a request URL via {@link SharedServer.getServerUrl}.
     *
     * Both the expected text and the unexpected text are tested using a contains
     * test. The test does not look for an exact match.
     *
     * @param webBrowser          Simulated web browser instance through which the request is made.
     * @param requestPath         The path which will be requested.
     * @param expectedResponses   Expected response text. All elements are tested.
     * @param unexpectedResponses Unexpected response text. All elements are tested.
     * @return The encapsulated response.
     *
     * @throws Exception Thrown if the expected response text is not present or if the
     *                       unexpected response text is present.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the CDI12TestV2 jar to add to the war app as a lib
        JavaArchive CDI12TestV2Jar = ShrinkHelper.buildJavaArchive(CDI12_TEST_V2_JAR_NAME + ".jar",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2");
        CDI12TestV2Jar = (JavaArchive) ShrinkHelper.addDirectory(CDI12TestV2Jar, "test-applications/CDI12TestV2.jar/resources");
        // Build the war app CDI12TestV2NoInjection.war and add the dependencies
        WebArchive CDI12TestV2NoInjectionApp = ShrinkHelper.buildDefaultApp(CDI12_TEST_V2_NO_INJECTION_APP_NAME + ".war",
                                                                            "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2noinjection.war.cdi.servlets");
        CDI12TestV2NoInjectionApp = CDI12TestV2NoInjectionApp.addAsLibraries(CDI12TestV2Jar);
        // Verify if the apps are in the server before trying to deploy them
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(CDI12_TEST_V2_NO_INJECTION_APP_NAME);
            LOG.info("addAppToServer : " + CDI12_TEST_V2_NO_INJECTION_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
            ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), CDI12TestV2NoInjectionApp);
        }
        SHARED_SERVER.startIfNotStarted();
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + CDI12_TEST_V2_NO_INJECTION_APP_NAME);
    }
    
    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer(null);
        }
    }

    /** Standard failure text. Usually unexpected. */
    public static final String[] FAILED_RESPONSE = new String[] { "FAILED" };

    // Servlet cases ...

    public static final String SERVLET_NO_INJECTION_CONTEXT_ROOT = "/CDI12TestV2NoInjection";
    public static final String SERVLET_NO_INJECTION_URL_FRAGMENT = "/CDINoInjection";
    public static final String SERVLET_NO_INJECTION_URL = SERVLET_NO_INJECTION_CONTEXT_ROOT + SERVLET_NO_INJECTION_URL_FRAGMENT;

    public static final String[] SERVLET_NO_INJECTION_EXPECTED = {
                                                                   "Servlet Hello! No Injection",
                                                                   "Filter Hello! No Injection",
                                                                   "Listener Hello! No Injection"
    };

    @Test
    @Mode(TestMode.LITE)
    public void testCDIEnabledNoInjection() throws Exception {
        WebBrowser webBrowser = createWebBrowserForTestCase();
        verifyResponse(webBrowser,
                       SERVLET_NO_INJECTION_URL,
                       SERVLET_NO_INJECTION_EXPECTED, FAILED_RESPONSE);
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
