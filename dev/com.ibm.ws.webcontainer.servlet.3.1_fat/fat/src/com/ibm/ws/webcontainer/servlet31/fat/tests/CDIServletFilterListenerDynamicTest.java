/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * CDI Tests
 *
 * Tests dynamic servlet filter / listener injection
 */
@RunWith(FATRunner.class)
public class CDIServletFilterListenerDynamicTest {

    private static final Logger LOG = Logger.getLogger(CDIServletFilterListenerDynamicTest.class.getName());

    // Server instance ...
    @Server("servlet31_cdiServletFilterListenerDynamicServer")
    public static LibertyServer LS;

    private static final String CDI12_TEST_V2_JAR_NAME = "CDI12TestV2";
    private static final String CDI12_TEST_V2_DYNAMIC_APP_NAME = "CDI12TestV2Dynamic";

    /**
     * Perform a request to the the server instance and verify that the
     * response has expected text. Throw an exception if the expected
     * text is not present or if the unexpected text is present.
     *
     * Both the expected text and the unexpected text are tested using a contains
     * test. The test does not look for an exact match.
     *
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
        // Build the war app CDI12TestV2Dynamic.war and add the dependencies
        WebArchive CDI12TestV2DynamicApp = ShrinkHelper.buildDefaultApp(CDI12_TEST_V2_DYNAMIC_APP_NAME + ".war",
                                                                        "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2dynamic.war.cdi.dynamic",
                                                                        "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2dynamic.war.cdi.servlets");
        CDI12TestV2DynamicApp = (WebArchive) ShrinkHelper.addDirectory(CDI12TestV2DynamicApp, "test-applications/CDI12TestV2Dynamic.war/resources");
        CDI12TestV2DynamicApp = CDI12TestV2DynamicApp.addAsLibrary(CDI12TestV2Jar);
        
        // Export the application.
        ShrinkHelper.exportDropinAppToServer(LS, CDI12TestV2DynamicApp);

        // Star tthe server and use the class name so we can find logs easily.
        LS.startServer(CDIServletFilterListenerDynamicTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (LS != null && LS.isStarted()) {
          LS.stopServer();
        }
    }

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

    public static final String SERVLET_DYNAMIC_CONTEXT_ROOT = "/CDI12TestV2Dynamic";
    public static final String SERVLET_VERIFIER_URL_FRAGMENT = "/CDIVerifier";

    public static final String SERVLET_DYNAMIC_URL_FRAGMENT = "/CDIDynamicServlet";

    @Test
    @Mode(TestMode.LITE)
    public void testCDIServletFilterListenerDynamic() throws Exception {
        verifyStringsInResponse(new HttpClient(), SERVLET_DYNAMIC_CONTEXT_ROOT, SERVLET_VERIFIER_URL_FRAGMENT + "?operation=verify", new String[] {});
        HttpClient session1 = new HttpClient();
        verifyStringsInResponse(session1, SERVLET_DYNAMIC_CONTEXT_ROOT, SERVLET_DYNAMIC_URL_FRAGMENT + "?payload=" + "V1", SERVLET_EXPECTED_TEXT_1);
        verifyStringsInResponse(session1, SERVLET_DYNAMIC_CONTEXT_ROOT, SERVLET_DYNAMIC_URL_FRAGMENT + "?payload=" + "V2", SERVLET_EXPECTED_TEXT_2);
        HttpClient session2 = new HttpClient();
        verifyStringsInResponse(session2, SERVLET_DYNAMIC_CONTEXT_ROOT, SERVLET_DYNAMIC_URL_FRAGMENT + "?payload=" + "V3", SERVLET_EXPECTED_TEXT_3);
    }

    private void verifyStringsInResponse(HttpClient client, String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
      GetMethod get = new GetMethod("http://" + LS.getHostname() + ":" + LS.getHttpDefaultPort() + contextRoot + path);
      int responseCode = client.executeMethod(get);
      String responseBody = get.getResponseBodyAsString();
      LOG.info("Response : " + responseBody);

      assertEquals("Expected " + 200 + " status code was not returned!",
                   200, responseCode);

      for (String expectedResponse : expectedResponseStrings) {
          assertTrue("The response did not contain: " + expectedResponse, responseBody.contains(expectedResponse));
      }
  }
}
