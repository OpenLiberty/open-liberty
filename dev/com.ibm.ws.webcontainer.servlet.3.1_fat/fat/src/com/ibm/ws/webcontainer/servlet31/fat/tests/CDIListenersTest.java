/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;
import java.util.logging.Logger;

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
 * Verify that injection is performed into several listener types.
 */
@RunWith(FATRunner.class)
public class CDIListenersTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(CDIListenersTest.class.getName());

    // Server instance ...
    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_cdiListenersServer");

    private static final String CDI12_TEST_V2_JAR_NAME = "CDI12TestV2";
    private static final String CDI12_TEST_V2_LISTENERS_APP_NAME = "CDI12TestV2Listeners";

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
        // Build the war app CDI12TestV2Listeners.war and add the dependencies
        WebArchive CDI12TestV2ListenersApp = ShrinkHelper.buildDefaultApp(CDI12_TEST_V2_LISTENERS_APP_NAME + ".war",
                                                                          "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.beans",
                                                                          "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.interceptors",
                                                                          "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.listeners",
                                                                          "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.servlets");
        CDI12TestV2ListenersApp = (WebArchive) ShrinkHelper.addDirectory(CDI12TestV2ListenersApp, "test-applications/CDI12TestV2Listeners.war/resources");
        CDI12TestV2ListenersApp = CDI12TestV2ListenersApp.addAsLibraries(CDI12TestV2Jar);
        // Verify if the apps are in the server before trying to deploy them
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(CDI12_TEST_V2_LISTENERS_APP_NAME);
            LOG.info("addAppToServer : " + CDI12_TEST_V2_LISTENERS_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), CDI12TestV2ListenersApp);
            //SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + CDI12_TEST_V2_LISTENERS_APP_NAME);
        }
        //SHARED_SERVER.startIfNotStarted();
        //SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + CDI12_TEST_V2_LISTENERS_APP_NAME);
        SHARED_SERVER.getLibertyServer().startServer(CDIListenersTest.class.getSimpleName() + ".log");
        SHARED_SERVER.getLibertyServer().waitForStringInLogUsingMark("CWWKO0219I*");
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

    // Operation selection ...

    public static final String OPERATION_PARAMETER_NAME = "operation";
    public static final String OPERATION_DISPLAY_LOG = "displayLog";

    public static final String LISTENERS_SERVLET_CONTEXT_ROOT = "/CDI12TestV2Listeners";
    public static final String LISTENERS_SERVLET_URL_FRAGMENT = "/CDIListeners";
    public static final String LISTENERS_SERVLET_URL = LISTENERS_SERVLET_CONTEXT_ROOT + LISTENERS_SERVLET_URL_FRAGMENT;

    public static final String OPERATION_ADD_ATTRIBUTES = "addAttributes";

    public static final String ATTRIBUTE_NAME_PARAMETER_NAME = "attributeName";
    public static final String ATTRIBUTE_VALUE_PARAMETER_NAME = "parameterValue";

    public static final String COMMENT_PARAMETER_NAME = "comment";

    public String getListenersURL(String operationName, String comment) throws UnsupportedEncodingException {
        comment = URLEncoder.encode(comment, "UTF-8"); // throws UnsupportedEncodingException

        // @formatter:off
        return
            LISTENERS_SERVLET_URL + "?" +
            OPERATION_PARAMETER_NAME + "=" + operationName + "&" +
            COMMENT_PARAMETER_NAME + "=" + comment;
        // @formatter:on
    }

    public String getListenersURL(String operationName,
                                  String attributeName, String attributeValue,
                                  String comment) throws UnsupportedEncodingException {
        comment = URLEncoder.encode(comment, "UTF-8"); // throws UnsupportedEncodingException

        // @formatter:off
        return
            LISTENERS_SERVLET_URL + "?" +
            OPERATION_PARAMETER_NAME + "=" + operationName + "&" +
            ATTRIBUTE_NAME_PARAMETER_NAME + "=" + attributeName + "&" +
            ATTRIBUTE_VALUE_PARAMETER_NAME + "=" + attributeValue + "&" +
            COMMENT_PARAMETER_NAME + "=" + comment;
        // @formatter:on
    }

    /**
     * Verify that injection is performed into several listener types.
     *
     * The test servlet is placed in a WAR with several listeners which are
     * tagged with "@WebListener". Those are:
     *
     * CDIHttpSessionAttributeListener
     * CDIHttpSessionListener
     *
     * CDIHttpSessionIdListener
     *
     * CDIHttpServletContextAttributeListener
     * CDIHttpServletContextListener
     *
     * CDIHttpServletRequestAttributeListener
     * CDIHttpServletRequestListener
     *
     * The test invokes the servlet, which performs operations to trigger
     * methods in each of the listeners, then displays data relayed back
     * through an application scoped log bean.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testCDIListeners() throws Exception {
        WebBrowser firstSessionBrowser = createWebBrowserForTestCase();

        verifyResponse(firstSessionBrowser,
                       getListenersURL(OPERATION_DISPLAY_LOG, "Verify CDI Listeners Servlet"),
                       LISTENERS_SERVLET_EXPECTED_VERIFICATION,
                       FAILED_RESPONSE);

        verifyResponse(firstSessionBrowser,
                       getListenersURL(OPERATION_ADD_ATTRIBUTES,
                                       CDI_ATTRIBUTE_NAME_PREFIX + "_" + "A1", "V1",
                                       "Assignment to A1 of V1 in S1"),
                       LISTENERS_SERVLET_EXPECTED_ASSIGN_A1,
                       FAILED_RESPONSE);

        verifyResponse(firstSessionBrowser,
                       getListenersURL(OPERATION_DISPLAY_LOG,
                                       "Log assignment to A1 of V1 in S1"),
                       LISTENERS_SERVLET_EXPECTED_ASSIGNED_A1,
                       FAILED_RESPONSE);

        verifyResponse(firstSessionBrowser,
                       getListenersURL(OPERATION_ADD_ATTRIBUTES,
                                       CDI_ATTRIBUTE_NAME_PREFIX + "_" + "A2", "V2",
                                       "Assignment to A2 of V2 in S1"),
                       LISTENERS_SERVLET_EXPECTED_ASSIGN_A2,
                       FAILED_RESPONSE);

        verifyResponse(firstSessionBrowser,
                       getListenersURL(OPERATION_DISPLAY_LOG,
                                       "Log assignment to A2 of V2 in S1"),
                       LISTENERS_SERVLET_EXPECTED_ASSIGNED_A2,
                       FAILED_RESPONSE);

        WebBrowser secondSessionBrowser = createWebBrowserForTestCase();

        verifyResponse(secondSessionBrowser,
                       getListenersURL(OPERATION_ADD_ATTRIBUTES,
                                       CDI_ATTRIBUTE_NAME_PREFIX + "_" + "A3", "V3",
                                       "Assignment to A3 of V3 in S2"),
                       LISTENERS_SERVLET_EXPECTED_ASSIGN_A3,
                       FAILED_RESPONSE);

        verifyResponse(secondSessionBrowser,
                       getListenersURL(OPERATION_DISPLAY_LOG,
                                       "Log assignment to A3 of V3 in S2"),
                       LISTENERS_SERVLET_EXPECTED_ASSIGNED_A3,
                       FAILED_RESPONSE);

        verifyResponse(secondSessionBrowser,
                       getListenersURL(OPERATION_ADD_ATTRIBUTES,
                                       CDI_ATTRIBUTE_NAME_PREFIX + "_" + "A4", "V4",
                                       "Assign to A4 of V4 in S2"),
                       LISTENERS_SERVLET_EXPECTED_ASSIGN_A4,
                       FAILED_RESPONSE);

        verifyResponse(secondSessionBrowser,
                       getListenersURL(OPERATION_DISPLAY_LOG,
                                       "Log assignment to A4 of V4 in S2"),
                       LISTENERS_SERVLET_EXPECTED_ASSIGNED_A4,
                       FAILED_RESPONSE);

    }

    // @formatter:off

    private static final String CDI_ATTRIBUTE_NAME_PREFIX = "CDI";

    public static final String[] LISTENERS_SERVLET_EXPECTED_VERIFICATION = new String[] {
        // First call ... making sure the listeners servlet is up and running ...

        "Hello! CDIListenersServlet",

        "Comment [ Verify CDI Listeners Servlet ]",

        "Application Log",
        "Parameter [ operation ] [ displayLog ]: Show application log",

        // Creation of the servlet context.
        "CDIServletContextListener: contextInitialized:",

        ":CDIServletContextListener:Constructor:Dependent:CDIConstructorBean_CL:CL:I:",
        ":CDIServletContextListener:PostConstruct:Start:CL:I:",
        ":CDIServletContextListener:Field:Dependent:CDIListenerFieldBean_CL:CL:I:",
        ":CDIServletContextListener:Method:Dependent:CDIMethodBean_CL:CL:I:",
        ":CDIServletContextListener:Field:Dependent:CDIDependentFieldBean_CL:CL:I:",
        ":CDIServletContextListener:Field:Application:CDIApplicationFieldBean_L:CL:I:", // First

        // Creation of the first session.
        "CDIHttpSessionListener: sessionCreated:",

        ":CDIHttpSessionListener:Constructor:Dependent:CDIConstructorBean_SL:SL:C:I1:",
        ":CDIHttpSessionListener:PostConstruct:Start:SL:C:I1:",
        ":CDIHttpSessionListener:Field:Dependent:CDIListenerFieldBean_SL:SL:C:I1:",
        ":CDIHttpSessionListener:Method:Dependent:CDIMethodBean_SL:SL:C:I1:",
        ":CDIHttpSessionListener:Field:Dependent:CDIDependentFieldBean_SL:SL:C:I1:",
        ":CDIHttpSessionListener:Field:Session:CDISessionFieldBean_SL:SL:C:I1:",
        ":CDIHttpSessionListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:", // Second

        // Creation of the servlet verification log request.
        "CDIServletRequestListener: requestInitialized:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:", // Third
    };

    private static final String[] LISTENERS_SERVLET_EXPECTED_ASSIGN_A1 = {
        "Hello! CDIListenersServlet",

        "Comment [ Assignment to A1 of V1 in S1 ]",

        "Parameter [ operation ] [ addAttributes ]",
        "Parameter [ attributeName ] [ CDI_A1 ]",
        "Parameter [ parameterValue ] [ V1 ]"
    };

    public static final String[] LISTENERS_SERVLET_EXPECTED_ASSIGNED_A1 = new String[] {
        // Assign A1=V1 in the next request, then obtain the log in a request after that.

        "Hello! CDIListenersServlet",

        "Comment [ Log assignment to A1 of V1 in S1 ]",

        "Application Log",
        "Parameter [ operation ] [ addAttributes ]: Add attribute [ CDI_A1 ] [ V1 ]",
        "Parameter [ operation ] [ displayLog ]: Show application log",

        // Destruction of the servlet verification log request.
        "CDIServletRequestListener: requestDestroyed:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:",

        // Creation of the assignment request.
        "CDIServletRequestListener: requestInitialized:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:",

        // The RAL noticing the assignment.
        "CDIServletRequestAttributeListener: attributeAdded: Name [ CDI_A1_R ]",
        "CDIServletRequestAttributeListener: attributeAdded:   Value [ V1 ]",

        ":CDIServletRequestAttributeListener:Constructor:Dependent:CDIConstructorBean_RAL:CDI_A1_R=V1:I1:",
        ":CDIServletRequestAttributeListener:PostConstruct:Start:CDI_A1_R=V1:I1:",
        ":CDIServletRequestAttributeListener:Field:Dependent:CDIListenerFieldBean_RAL:CDI_A1_R=V1:I1:",
        ":CDIServletRequestAttributeListener:Method:Dependent:CDIMethodBean_RAL:CDI_A1_R=V1:I1:",
        ":CDIServletRequestAttributeListener:Field:Dependent:CDIDependentFieldBean_RAL:CDI_A1_R=V1:I1:",
        ":CDIServletRequestAttributeListener:Field:Request:CDIRequestFieldBean_RAL:CDI_A1_R=V1:I1:",
        ":CDIServletRequestAttributeListener:Field:Session:CDISessionFieldBean_RAL:CDI_A1_R=V1:I1:",
        ":CDIServletRequestAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:",
        ":CDIHttpSessionAttributeListener:Constructor:Dependent:CDIConstructorBean_SAL:CDI_A1_S=V1:I1:",

        // The SAL noticing the assignment.
        "CDIHttpSessionAttributeListener: attributeAdded: Name [ CDI_A1_S ]",
        "CDIHttpSessionAttributeListener: attributeAdded:   Value [ V1 ]",

        ":CDIHttpSessionAttributeListener:PostConstruct:Start:CDI_A1_S=V1:I1:",
        ":CDIHttpSessionAttributeListener:Field:Dependent:CDIListenerFieldBean_SAL:CDI_A1_S=V1:I1:",
        ":CDIHttpSessionAttributeListener:Method:Dependent:CDIMethodBean_SAL:CDI_A1_S=V1",
        ":CDIHttpSessionAttributeListener:Field:Dependent:CDIDependentFieldBean_SAL:CDI_A1_S=V1",
        ":CDIHttpSessionAttributeListener:Field:Request:CDIRequestFieldBean_SAL:CDI_A1_S=V1:I1:",
        ":CDIHttpSessionAttributeListener:Field:Session:CDISessionFieldBean_SAL:CDI_A1_S=V1:I1:",
        ":CDIHttpSessionAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:",

        // The CAL noticing the assignment.
        "CDIServletContextAttributeListener: attributeAdded: Name [ CDI_A1_C ]",
        "CDIServletContextAttributeListener: attributeAdded:   Value [ V1 ]",

        ":CDIServletContextAttributeListener:Constructor:Dependent:CDIConstructorBean_CAL:CDI_A1_C=V1:I1:",
        ":CDIServletContextAttributeListener:PostConstruct:Start:CDI_A1_C=V1:I1:",
        ":CDIServletContextAttributeListener:Field:Dependent:CDIListenerFieldBean_CAL:CDI_A1_C=V1:I1:",
        ":CDIServletContextAttributeListener:Method:Dependent:CDIMethodBean_CAL:CDI_A1_C=V1:I1:",
        ":CDIServletContextAttributeListener:Field:Dependent:CDIDependentFieldBean_CAL:CDI_A1_C=V1:I1:",
        ":CDIServletContextAttributeListener:Field:Request:CDIRequestFieldBean_CAL:CDI_A1_C=V1:I1:",
        ":CDIServletContextAttributeListener:Field:Session:CDISessionFieldBean_CAL:CDI_A1_C=V1:I1:",
        ":CDIServletContextAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:",

        // Destruction of the assignment request
        "CDIServletRequestListener: requestDestroyed:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",

        // Creation of the log request.
        "CDIServletRequestListener: requestInitialized:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:"
    };

    private static final String[] LISTENERS_SERVLET_EXPECTED_ASSIGN_A2 = {
        "Hello! CDIListenersServlet",

        "Comment [ Assignment to A2 of V2 in S1 ]",

        "Parameter [ operation ] [ addAttributes ]",
        "Parameter [ attributeName ] [ CDI_A2 ]",
        "Parameter [ parameterValue ] [ V2 ]"
    };

    public static final String[] LISTENERS_SERVLET_EXPECTED_ASSIGNED_A2 = new String[] {
        // We assign A2=V2 in the next request, then obtain the log in a request after that.
        // We are still in the same session as for the A1=V1 assignment.

        "Hello! CDIListenersServlet",

        "Comment [ Log assignment to A2 of V2 in S1 ]",

        "Application Log",
        "Parameter [ operation ] [ addAttributes ]: Add attribute [ CDI_A2 ] [ V2 ]",
        "Parameter [ operation ] [ displayLog ]: Show application log",

        // Destruction of the log request.
        "CDIServletRequestListener: requestDestroyed:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",

        // Creation of the assignment request.
        "CDIServletRequestListener: requestInitialized:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",

        // The RAL noticing the assignment.
        "CDIServletRequestAttributeListener: attributeAdded: Name [ CDI_A2_R ]",
        "CDIServletRequestAttributeListener: attributeAdded:   Value [ V2 ]",

        ":CDIServletRequestAttributeListener:Constructor:Dependent:CDIConstructorBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:",
        ":CDIServletRequestAttributeListener:PostConstruct:Start:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:",
        ":CDIServletRequestAttributeListener:Field:Dependent:CDIListenerFieldBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:",
        ":CDIServletRequestAttributeListener:Method:Dependent:CDIMethodBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:",
        ":CDIServletRequestAttributeListener:Field:Dependent:CDIDependentFieldBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:",
        ":CDIServletRequestAttributeListener:Field:Request:CDIRequestFieldBean_RAL:CDI_A2_R=V2:I2:",
        ":CDIServletRequestAttributeListener:Field:Session:CDISessionFieldBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:",
        ":CDIServletRequestAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:CDI_A2_R=V2:I2:",

        // The SAL noticing the assignment.
        "CDIHttpSessionAttributeListener: attributeAdded: Name [ CDI_A2_S ]",
        "CDIHttpSessionAttributeListener: attributeAdded:   Value [ V2 ]",

        ":CDIHttpSessionAttributeListener:Constructor:Dependent:CDIConstructorBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:",
        ":CDIHttpSessionAttributeListener:PostConstruct:Start:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:",
        ":CDIHttpSessionAttributeListener:Field:Dependent:CDIListenerFieldBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:",
        ":CDIHttpSessionAttributeListener:Method:Dependent:CDIMethodBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:",
        ":CDIHttpSessionAttributeListener:Field:Dependent:CDIDependentFieldBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:",
        ":CDIHttpSessionAttributeListener:Field:Request:CDIRequestFieldBean_SAL:CDI_A2_S=V2:I2:",
        ":CDIHttpSessionAttributeListener:Field:Session:CDISessionFieldBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:",
        ":CDIHttpSessionAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:CDI_A2_R=V2:I2:CDI_A2_S=V2:I2:",

        // The CAL noticing the assignment.
        "CDIServletContextAttributeListener: attributeAdded: Name [ CDI_A2_C ]",
        "CDIServletContextAttributeListener: attributeAdded:   Value [ V2 ]",

        ":CDIServletContextAttributeListener:Constructor:Dependent:CDIConstructorBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:",
        ":CDIServletContextAttributeListener:PostConstruct:Start:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:",
        ":CDIServletContextAttributeListener:Field:Dependent:CDIListenerFieldBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:",
        ":CDIServletContextAttributeListener:Method:Dependent:CDIMethodBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:",
        ":CDIServletContextAttributeListener:Field:Dependent:CDIDependentFieldBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:",
        ":CDIServletContextAttributeListener:Field:Request:CDIRequestFieldBean_CAL:CDI_A2_C=V2:I2:",
        ":CDIServletContextAttributeListener:Field:Session:CDISessionFieldBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:",
        ":CDIServletContextAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:CDI_A2_R=V2:I2:CDI_A2_S=V2:I2:CDI_A2_C=V2:I2:",

        // Destruction of the assignment request.
        "CDIServletRequestListener: requestDestroyed:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",

        // Creation of the log request.
        "CDIServletRequestListener: requestInitialized:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:"
    };


    private static final String[] LISTENERS_SERVLET_EXPECTED_ASSIGN_A3 = {
        "Hello! CDIListenersServlet",

        "Comment [ Assignment to A3 of V3 in S2 ]",

        "Parameter [ operation ] [ addAttributes ]",
        "Parameter [ attributeName ] [ CDI_A3 ]",
        "Parameter [ parameterValue ] [ V3 ]"
    };

    public static final String[] LISTENERS_SERVLET_EXPECTED_ASSIGNED_A3 = new String[] {
        // We assign A3=V3 in the next request, then obtain the log in a request after that.
        // We are in a new, second, session.

        "Hello! CDIListenersServlet",

        "Comment [ Log assignment to A3 of V3 in S2 ]",

        "Application Log",
        "Parameter [ operation ] [ addAttributes ]: Add attribute [ CDI_A3 ] [ V3 ]",
        "Parameter [ operation ] [ displayLog ]: Show application log",

        // Destruction of the log request.
        "CDIServletRequestListener: requestDestroyed:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",

        // Creation of the second session.
        "CDIHttpSessionListener: sessionCreated:",

        ":CDIHttpSessionListener:Constructor:Dependent:CDIConstructorBean_SL:SL:C:I1:SL:C:I1:",
        ":CDIHttpSessionListener:PostConstruct:Start:SL:C:I1:SL:C:I1:",
        ":CDIHttpSessionListener:Field:Dependent:CDIListenerFieldBean_SL:SL:C:I1:SL:C:I1:",
        ":CDIHttpSessionListener:Method:Dependent:CDIMethodBean_SL:SL:C:I1:SL:C:I1:",
        ":CDIHttpSessionListener:Field:Dependent:CDIDependentFieldBean_SL:SL:C:I1:SL:C:I1:",
        ":CDIHttpSessionListener:Field:Session:CDISessionFieldBean_SL:SL:C:I1:",
        ":CDIHttpSessionListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:SL:C:I1:",

        // Creation of the assignment request.
        "CDIServletRequestListener: requestInitialized:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:SL:C:I1:RL:I:Int1:",

        // The RAL noticing the assignment
        "CDIServletRequestAttributeListener: attributeAdded: Name [ CDI_A3_R ]",
        "CDIServletRequestAttributeListener: attributeAdded:   Value [ V3 ]",

        ":CDIServletRequestAttributeListener:Constructor:Dependent:CDIConstructorBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3:",
        ":CDIServletRequestAttributeListener:PostConstruct:Start:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3",
        ":CDIServletRequestAttributeListener:Field:Dependent:CDIListenerFieldBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3:",
        ":CDIServletRequestAttributeListener:Method:Dependent:CDIMethodBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3:",
        ":CDIServletRequestAttributeListener:Field:Dependent:CDIDependentFieldBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3:",
        ":CDIServletRequestAttributeListener:Field:Request:CDIRequestFieldBean_RAL:CDI_A3_R=V3:I3:",
        ":CDIServletRequestAttributeListener:Field:Session:CDISessionFieldBean_RAL:CDI_A3_R=V3:I3:",
        ":CDIServletRequestAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:CDI_A2_R=V2:I2:CDI_A2_S=V2:I2:CDI_A2_C=V2:I2:CDI_A3_R=V3:I3:",

        // The SAL noticing the assignment
        "CDIHttpSessionAttributeListener: attributeAdded: Name [ CDI_A3_S ]",
        "CDIHttpSessionAttributeListener: attributeAdded:   Value [ V3 ]",

        ":CDIHttpSessionAttributeListener:Constructor:Dependent:CDIConstructorBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:",
        ":CDIHttpSessionAttributeListener:PostConstruct:Start:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:",
        ":CDIHttpSessionAttributeListener:Field:Dependent:CDIListenerFieldBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:",
        ":CDIHttpSessionAttributeListener:Method:Dependent:CDIMethodBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:",
        ":CDIHttpSessionAttributeListener:Field:Dependent:CDIDependentFieldBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:",
        ":CDIHttpSessionAttributeListener:Field:Request:CDIRequestFieldBean_SAL:CDI_A3_S=V3:I3:",
        ":CDIHttpSessionAttributeListener:Field:Session:CDISessionFieldBean_SAL:CDI_A3_S=V3:I3:",
        ":CDIHttpSessionAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:CDI_A2_R=V2:I2:CDI_A2_S=V2:I2:CDI_A2_C=V2:I2:CDI_A3_R=V3:I3:CDI_A3_S=V3:I3:",

        // The CAL noticing the assignment
        "CDIServletContextAttributeListener: attributeAdded: Name [ CDI_A3_C ]",
        "CDIServletContextAttributeListener: attributeAdded:   Value [ V3 ]",

        ":CDIServletContextAttributeListener:Constructor:Dependent:CDIConstructorBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:",
        ":CDIServletContextAttributeListener:PostConstruct:Start:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:",
        ":CDIServletContextAttributeListener:Field:Dependent:CDIListenerFieldBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:",
        ":CDIServletContextAttributeListener:Method:Dependent:CDIMethodBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:",
        ":CDIServletContextAttributeListener:Field:Dependent:CDIDependentFieldBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:",
        ":CDIServletContextAttributeListener:Field:Request:CDIRequestFieldBean_CAL:CDI_A3_C=V3:I3:",
        ":CDIServletContextAttributeListener:Field:Session:CDISessionFieldBean_CAL:CDI_A3_C=V3:I3:",
        ":CDIServletContextAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:CDI_A2_R=V2:I2:CDI_A2_S=V2:I2:CDI_A2_C=V2:I2:CDI_A3_R=V3:I3:CDI_A3_S=V3:I3:CDI_A3_C=V3:I3:",

        // Destruction of the assignment request.
        "CDIServletRequestListener: requestDestroyed:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:SL:C:I1:RL:I:Int1:RL:D:Int2:",

        // Creation of the log request.
        "CDIServletRequestListener: requestInitialized:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
    };

    private static final String[] LISTENERS_SERVLET_EXPECTED_ASSIGN_A4 = {
        "Hello! CDIListenersServlet",

        "Parameter [ operation ] [ addAttributes ]",
        "Parameter [ attributeName ] [ CDI_A4 ]",
        "Parameter [ parameterValue ] [ V4 ]"
    };

    public static final String[] LISTENERS_SERVLET_EXPECTED_ASSIGNED_A4 = new String[] {
        // We assign A4=V4 in the next request, then obtain the log in a request after that.
        // We are still in the same session as for the A3=V3 assignment.

        "Hello! CDIListenersServlet",

        "Comment [ Log assignment to A4 of V4 in S2 ]",

        "Application Log",
        "Parameter [ operation ] [ addAttributes ]: Add attribute [ CDI_A4 ] [ V4 ]",
        "Parameter [ operation ] [ displayLog ]: Show application log",

        // Destruction of the log request.
        "CDIServletRequestListener: requestDestroyed:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",

        // Creation of the assignment request.
        "CDIServletRequestListener: requestInitialized:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",

        // The RAL noticing the assignment.
        "CDIServletRequestAttributeListener: attributeAdded: Name [ CDI_A4_R ]",
        "CDIServletRequestAttributeListener: attributeAdded:   Value [ V4 ]",

        ":CDIServletRequestAttributeListener:Constructor:Dependent:CDIConstructorBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3:CDI_A4_R=V4:I4:",
        ":CDIServletRequestAttributeListener:PostConstruct:Start:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3:CDI_A4_R=V4:I4:",
        ":CDIServletRequestAttributeListener:Field:Dependent:CDIListenerFieldBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3:CDI_A4_R=V4:I4:",
        ":CDIServletRequestAttributeListener:Method:Dependent:CDIMethodBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3:CDI_A4_R=V4:I4:",
        ":CDIServletRequestAttributeListener:Field:Dependent:CDIDependentFieldBean_RAL:CDI_A1_R=V1:I1:CDI_A2_R=V2:I2:CDI_A3_R=V3:I3:CDI_A4_R=V4:I4:",
        ":CDIServletRequestAttributeListener:Field:Request:CDIRequestFieldBean_RAL:CDI_A4_R=V4:I4:",
        ":CDIServletRequestAttributeListener:Field:Session:CDISessionFieldBean_RAL:CDI_A3_R=V3:I3:CDI_A4_R=V4:I4:",
        ":CDIServletRequestAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:CDI_A2_R=V2:I2:CDI_A2_S=V2:I2:CDI_A2_C=V2:I2:CDI_A3_R=V3:I3:CDI_A3_S=V3:I3:CDI_A3_C=V3:I3:CDI_A4_R=V4:I4:",

        // The SAL noticing the assignment
        "CDIHttpSessionAttributeListener: attributeAdded: Name [ CDI_A4_S ]",
        "CDIHttpSessionAttributeListener: attributeAdded:   Value [ V4 ]",

        ":CDIHttpSessionAttributeListener:Constructor:Dependent:CDIConstructorBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:CDI_A4_S=V4:I4:",
        ":CDIHttpSessionAttributeListener:PostConstruct:Start:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:CDI_A4_S=V4:I4:",
        ":CDIHttpSessionAttributeListener:Field:Dependent:CDIListenerFieldBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:CDI_A4_S=V4:I4:",
        ":CDIHttpSessionAttributeListener:Method:Dependent:CDIMethodBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:CDI_A4_S=V4:I4:",
        ":CDIHttpSessionAttributeListener:Field:Dependent:CDIDependentFieldBean_SAL:CDI_A1_S=V1:I1:CDI_A2_S=V2:I2:CDI_A3_S=V3:I3:CDI_A4_S=V4:I4:",
        ":CDIHttpSessionAttributeListener:Field:Request:CDIRequestFieldBean_SAL:CDI_A4_S=V4:I4:",
        ":CDIHttpSessionAttributeListener:Field:Session:CDISessionFieldBean_SAL:CDI_A3_S=V3:I3:CDI_A4_S=V4:I4:",
        ":CDIHttpSessionAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:CDI_A2_R=V2:I2:CDI_A2_S=V2:I2:CDI_A2_C=V2:I2:CDI_A3_R=V3:I3:CDI_A3_S=V3:I3:CDI_A3_C=V3:I3:CDI_A4_R=V4:I4:CDI_A4_S=V4:I4:",

        // The CAL noticing the assignment
        "CDIServletContextAttributeListener: attributeAdded: Name [ CDI_A4_C ]",
        "CDIServletContextAttributeListener: attributeAdded:   Value [ V4 ]",

        ":CDIServletContextAttributeListener:Constructor:Dependent:CDIConstructorBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:CDI_A4_C=V4:I4:",
        ":CDIServletContextAttributeListener:PostConstruct:Start:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:CDI_A4_C=V4:I4:",
        ":CDIServletContextAttributeListener:Field:Dependent:CDIListenerFieldBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:CDI_A4_C=V4:I4:",
        ":CDIServletContextAttributeListener:Method:Dependent:CDIMethodBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:CDI_A4_C=V4:I4:",
        ":CDIServletContextAttributeListener:Field:Dependent:CDIDependentFieldBean_CAL:CDI_A1_C=V1:I1:CDI_A2_C=V2:I2:CDI_A3_C=V3:I3:CDI_A4_C=V4:I4:",
        ":CDIServletContextAttributeListener:Field:Request:CDIRequestFieldBean_CAL:CDI_A4_C=V4:I4:",
        ":CDIServletContextAttributeListener:Field:Session:CDISessionFieldBean_CAL:CDI_A3_C=V3:I3:CDI_A4_C=V4:I4:",
        ":CDIServletContextAttributeListener:Field:Application:CDIApplicationFieldBean_AL:CDI_A1_R=V1:I1:CDI_A1_S=V1:I1:CDI_A1_C=V1:I1:CDI_A2_R=V2:I2:CDI_A2_S=V2:I2:CDI_A2_C=V2:I2:CDI_A3_R=V3:I3:CDI_A3_S=V3:I3:CDI_A3_C=V3:I3:CDI_A4_R=V4:I4:CDI_A4_S=V4:I4:CDI_A4_C=V4:I4:",

        // Destruction of the assignment request.
        "CDIServletRequestListener: requestDestroyed:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:",

        // Creation of the log request.
        "CDIServletRequestListener: requestInitialized:",

        ":CDIServletRequestListener:Constructor:Dependent:CDIConstructorBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:PostConstruct:Start:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIListenerFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Method:Dependent:CDIMethodBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Dependent:CDIDependentFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Request:CDIRequestFieldBean_RL:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Session:CDISessionFieldBean_RL:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",
        ":CDIServletRequestListener:Field:Application:CDIApplicationFieldBean_L:CL:I:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:SL:C:I1:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:RL:D:Int2:RL:I:Int1:",

        // We won't see the destruction of the last log request,
        // We won't see the destruction of the servlet context.
        // These all happen after the last log request performed by the test framework.

        // We won't see the destruction of the first or second session.  Those are set to timeout after
        // very long durations.  We could try to log the session destructions, but that would be hard
        // to make happen in a reliable sequence.
    };

    /* (non-Javadoc)
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    // @formatter:on
}
