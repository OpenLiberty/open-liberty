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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * CDI Test
 *
 * Tests Servlet Filter/Listener Injection
 */
@RunWith(FATRunner.class)
public class CDIServletFilterListenerTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(CDIServletFilterListenerTest.class.getName());

    // Server instance ...
    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_cdiServletFilterListenerServer");
    
    private static final String CDI12_TEST_V2_JAR_NAME = "CDI12TestV2";
    private static final String CDI12_TEST_V2_INJECTION_APP_NAME = "CDI12TestV2Injection";

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
        // Build the war app CDI12TestV2Injection.war and add the dependencies
        WebArchive CDI12TestV2InjectionApp = ShrinkHelper.buildDefaultApp(CDI12_TEST_V2_INJECTION_APP_NAME + ".war",
                                                                          "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2injection.war.cdi.servlets");
        CDI12TestV2InjectionApp = CDI12TestV2InjectionApp.addAsLibrary(CDI12TestV2Jar);
        // Verify if the apps are in the server before trying to deploy them
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
          Set<String> appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(CDI12_TEST_V2_INJECTION_APP_NAME);
          LOG.info("addAppToServer : " + CDI12_TEST_V2_INJECTION_APP_NAME + " already installed : " + !appInstalled.isEmpty());
          if (appInstalled.isEmpty())
            ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), CDI12TestV2InjectionApp);
        }
        SHARED_SERVER.startIfNotStarted();
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + CDI12_TEST_V2_INJECTION_APP_NAME);
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

    public static final String SERVLET_INJECTION_CONTEXT_ROOT = "/CDI12TestV2Injection";
    public static final String SERVLET_INJECTION_URL_FRAGMENT = "/CDIInjection";
    public static final String SERVLET_INJECTION_URL = SERVLET_INJECTION_CONTEXT_ROOT + SERVLET_INJECTION_URL_FRAGMENT;

    // The pattern for expected text is:
    //
    // Emitter (Servlet, Listener, Filter)
    // Injection Case (Constructor, PostConstruct, PreDestroy, Field, Method, Produces)
    // Scope (Request, Session, Application)
    // Bean class
    // Bean value
    //
    // Or:
    //
    // Emitter (Servlet, Listener)
    // Comment (SessionId, Payload, Entry, Exit)

    // @formatter:off
    public static final String[] SERVLET_EXPECTED_TEXT_1 = new String[] {
        ":Listener:Entry:",
        ":Listener:Payload=V1:",
        ":Listener:Constructor:Dependent:ConstructorBean:(Listener:V1):",
        ":Listener:PostConstruct:Start:",
        ":Listener:PreDestroy:Stop:",
        ":Listener:Field:Dependent:ListenerFieldBean:(Listener:V1):",
        ":Listener:Field:Application:ApplicationFieldBean:(Listener:V1):",
        ":Listener:Method:Dependent:MethodBean:(Listener:V1):",
        ":Listener:Produces:Application:ListenerProducesBean:0:",
        ":Listener:Exit:",

        ":Filter:Entry:",
        ":Filter:SessionId=",
        ":Filter:Payload=V1:",
        ":Filter:Constructor:Dependent:ConstructorBean:(Filter:V1):",
        ":Filter:PostConstruct:Start:",
        ":Filter:PreDestroy:Stop:",
        ":Filter:Field:Request:FilterFieldBean:(Filter:V1):",
        ":Filter:Field:Session:SessionFieldBean:(Filter:V1):",
        ":Filter:Field:Application:ApplicationFieldBean:(Listener:V1):(Filter:V1):",
        ":Filter:Method:Dependent:MethodBean:(Filter:V1):",
        // ":Filter:Produces:Session:FilterProducesBean:0:",
        ":Filter:Exit:",

        ":Servlet:Entry:",
        ":Servlet:SessionId=",
        ":Servlet:Payload=V1:",
        ":Servlet:Constructor:Dependent:ConstructorBean:(Servlet:V1):",
        ":Servlet:PostConstruct:Start:",
        ":Servlet:PreDestroy:Stop:",
        ":Servlet:Field:Request:ServletFieldBean:(Servlet:V1):",
        ":Servlet:Field:Session:SessionFieldBean:(Filter:V1):(Servlet:V1):",
        ":Servlet:Field:Application:ApplicationFieldBean:(Listener:V1):(Filter:V1):(Servlet:V1):",
        ":Servlet:Method:Dependent:MethodBean:(Servlet:V1):",
        ":Servlet:Produces:Dependent:ServletProducesBean:0:",
        ":Servlet:Exit:"
    };

    public static final String[] SERVLET_EXPECTED_TEXT_2 = new String[] {
        ":Listener:Entry:",
        ":Listener:Payload=V2:",
        ":Listener:Constructor:Dependent:ConstructorBean:(Listener:V1):(Listener:V2):",
        ":Listener:PostConstruct:Start:",
        ":Listener:PreDestroy:Stop:",
        ":Listener:Field:Dependent:ListenerFieldBean:(Listener:V1):(Listener:V2):",
        ":Listener:Field:Application:ApplicationFieldBean:" +
          "(Listener:V1):(Filter:V1):(Servlet:V1):(Listener:V2):",
        ":Listener:Method:Dependent:MethodBean:(Listener:V1):(Listener:V2):",
        ":Listener:Produces:Application:ListenerProducesBean:0:",
        ":Listener:Exit:",

        ":Filter:Entry:",
        ":Filter:SessionId=",
        ":Filter:Payload=V2:",
        ":Filter:Constructor:Dependent:ConstructorBean:(Filter:V1):(Filter:V2):",
        ":Filter:PostConstruct:Start:",
        ":Filter:PreDestroy:Stop:",
        ":Filter:Field:Request:FilterFieldBean:(Filter:V2):",
        ":Filter:Field:Session:SessionFieldBean:" +
          "(Filter:V1):(Servlet:V1):(Filter:V2):",
        ":Filter:Field:Application:ApplicationFieldBean:" +
          "(Listener:V1):(Filter:V1):(Servlet:V1):" +
          "(Listener:V2):(Filter:V2):",
        ":Filter:Method:Dependent:MethodBean:(Filter:V1):(Filter:V2):",
        // ":Filter:Produces:Session:FilterProducesBean:0:",
        ":Filter:Exit:",

        ":Servlet:Entry:",
        ":Servlet:SessionId=",
        ":Servlet:Payload=V2:",
        ":Servlet:Constructor:Dependent:ConstructorBean:(Servlet:V1):(Servlet:V2):",
        ":Servlet:PostConstruct:Start:",
        ":Servlet:PreDestroy:Stop:",
        ":Servlet:Field:Request:ServletFieldBean:(Servlet:V2):",
        ":Servlet:Field:Session:SessionFieldBean:" +
          "(Filter:V1):(Servlet:V1):(Filter:V2):(Servlet:V2):",
        ":Servlet:Field:Application:ApplicationFieldBean:" +
          "(Listener:V1):(Filter:V1):(Servlet:V1):" +
          "(Listener:V2):(Filter:V2):(Servlet:V2)",
        ":Servlet:Method:Dependent:MethodBean:(Servlet:V1):(Servlet:V2):",
        ":Servlet:Produces:Dependent:ServletProducesBean:0:",
        ":Servlet:Exit:"
    };

    public static final String[] SERVLET_EXPECTED_TEXT_3 = new String[] {
        ":Listener:Entry:",
        ":Listener:Payload=V3:",
        ":Listener:Constructor:Dependent:ConstructorBean:" +
          "(Listener:V1):(Listener:V2):(Listener:V3):",
        ":Listener:PostConstruct:Start:",
        ":Listener:PreDestroy:Stop:",
        ":Listener:Field:Dependent:ListenerFieldBean:" +
          "(Listener:V1):(Listener:V2):(Listener:V3):",
        ":Listener:Field:Application:ApplicationFieldBean:" +
          "(Listener:V1):(Filter:V1):(Servlet:V1):" +
          "(Listener:V2):(Filter:V2):(Servlet:V2):" +
          "(Listener:V3):",
        ":Listener:Method:Dependent:MethodBean:" +
          "(Listener:V1):(Listener:V2):(Listener:V3):",
        ":Listener:Produces:Application:ListenerProducesBean:0:",
        ":Listener:Exit:",

        ":Filter:Entry:",
        ":Filter:SessionId=",
        ":Filter:Payload=V3:",
        ":Filter:Constructor:Dependent:ConstructorBean:" +
          "(Filter:V1):(Filter:V2):(Filter:V3):",
        ":Filter:PostConstruct:Start:",
        ":Filter:PreDestroy:Stop:",
        ":Filter:Field:Request:FilterFieldBean:(Filter:V3):",
        ":Filter:Field:Session:SessionFieldBean:(Filter:V3):",
        ":Filter:Field:Application:ApplicationFieldBean:" +
          "(Listener:V1):(Filter:V1):(Servlet:V1):" +
          "(Listener:V2):(Filter:V2):(Servlet:V2):" +
          "(Listener:V3):(Filter:V3):",
        ":Filter:Method:Dependent:MethodBean:" +
          "(Filter:V1):(Filter:V2):(Filter:V3):",
        // ":Filter:Produces:Session:FilterProducesBean:0:",
        ":Filter:Exit:",

        ":Servlet:Entry:",
        ":Servlet:SessionId=",
        ":Servlet:Payload=V3:",
        ":Servlet:Constructor:Dependent:ConstructorBean:" +
          "(Servlet:V1):(Servlet:V2):(Servlet:V3):",
        ":Servlet:PostConstruct:Start:",
        ":Servlet:PreDestroy:Stop:",
        ":Servlet:Field:Request:ServletFieldBean:(Servlet:V3):",
        ":Servlet:Field:Session:SessionFieldBean:(Filter:V3):(Servlet:V3):",
        ":Servlet:Field:Application:ApplicationFieldBean:" +
          "(Listener:V1):(Filter:V1):(Servlet:V1):" +
          "(Listener:V2):(Filter:V2):(Servlet:V2):" +
          "(Listener:V3):(Filter:V3):(Servlet:V3):",
        ":Servlet:Method:Dependent:MethodBean:" +
          "(Servlet:V1):(Servlet:V2):(Servlet:V3):",
        ":Servlet:Produces:Dependent:ServletProducesBean:0:",
        ":Servlet:Exit:"
    };

    @Test
    @Mode(TestMode.LITE)
    public void testCDIServletFilterListener() throws Exception {
        WebBrowser firstSessionBrowser = createWebBrowserForTestCase();
        verifyResponse(
            firstSessionBrowser,
            SERVLET_INJECTION_URL + "?payload=" + "V1",
            SERVLET_EXPECTED_TEXT_1, FAILED_RESPONSE);

        verifyResponse(
            firstSessionBrowser,
            SERVLET_INJECTION_URL + "?payload=" + "V2",
            SERVLET_EXPECTED_TEXT_2, FAILED_RESPONSE);

        WebBrowser secondSessionBrowser = createWebBrowserForTestCase();
        verifyResponse(
            secondSessionBrowser,
            SERVLET_INJECTION_URL + "?payload=" + "V3",
            SERVLET_EXPECTED_TEXT_3, FAILED_RESPONSE);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
