/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This class provides coverage for multiple overlapping WS-Policy scenarios:
 *
 * 1.) It tests co-located client/server Dynamic policy attachment files via the WEB-INF/policy-attachments-client.xml
 * and WEB-INF/policy-attachments-server.xml files.
 *
 * 2.) It tests the fix provided by PH09634 - If a policy-attachments-server.xml is placed in the same
 * location as the policy-attachements-client.xml, in the WEB-INF
 * folder of a Web-Module, the client policy is processed, but the
 * server policy file is simply ignored.
 *
 * 3.) It tests WS-Policy's WS-Addressing support for both the client and server side by setting the NonAnonymousResponses
 * property on expressions defined in the table below. The rows of the table marked "TESTED" have expressions that have WS-PolicyAttachment
 * policy attached to the specific domain type. (Not all domain expressions are applicable with WS-Addressing)
 *
 * TODO: Expand testing to ensure CO-Located WS-PolicyAttachment works on all of the domain expressions that IBM documents that we support.
 * The expressions that still need to have tests have been marked below as UNTESTED, the large majority of them aren't applicable to WS-Addressing
 * Policy so we will need to find an alternative non-WS-Addressing non-WS-Security Policy to attach to via the different domain expressions.
 *
 * ------ WS-PolicyAttachment Domain Expression table -----------
 * --------------------------------------------------------------
 * Status --- WSDL Part --------------------- WS-PA domain expression
 * --------------------------------------------------------------
 * TESTED --- Endpoint Reference ------------ wsa:EndpointReference
 * TESTED --- Service ----------------------- wsdl11.service(service)
 * UNTESTED - Definitions ------------------- wsdl11.definitions()
 * UNTESTED - Message ----------------------- wsdl11.message(message)
 * UNTESTED - Message/part ------------------ wsdl11.messagePart(message / part)
 * UNTESTED - portType ---------------------- wsdl11.portType(portType)
 * UNTESTED - portType/operation ------------ wsdl11.portTypeOperation(portType/operation)
 * UNTESTED - portType/operation/input ------ wsdl11.portTypeOperation.input(portType/operation)
 * UNTESTED - portType/operation/output ----- wsdl11.portTypeOperation.output(portType/operation)
 * UNTESTED - portType/operation/fault ------ wsdl11.portTypeOperation.fault(portType/operation/fault)
 * UNTESTED - Binding ----------------------- wsdl11.binding(binding)
 * UNTESTED - Binding/operation ------------- wsdl11.bindingOperation(binding/operation)
 * UNTESTED - Binding/operation/input ------- wsdl11.bindingOperation.input(binding/operation)
 * UNTESTED - Binding/operation/output ------ wsdl11.bindingOperation.output(binding/operation)
 * UNTESTED - Binding/operation/fault ------- wsdl11.bindingOperation.fault(binding/operation/fault)
 * UNTESTED - port -------------------------- wsdl11.port(service/port)
 *
 * ------ TEST STRUCTURE ------
 *
 * **** Note: the policyAttachmentClient1 test-application src and server are recycled version of the same app code in the `com.ibm.ws.jaxws.X.wsat_fat` bucket ***
 *
 * 1.) Test method makes an HTTP request to the `com.ibm.ws.policyattachments.client1.ClientServlet1` servlet.
 * The request contains: hostname, port, name Web Service to be invoked, name of Web Service operation invoked.
 * 2.) The servlet parses the above values from the request message, and the appropriate Web Service client is use to invoke the Web Service.
 * 3.) The test checks what type of response is expected - 200 for valid tests, 500 for negative tests. If a negative test is being run
 * the test will also verify that the proper exception occurs in the log
 *
 *
 */
@RunWith(FATRunner.class)
public class ColocatedDynamicPolicyAttachmentsTest {

    @Server("ColocatedDynamicPolicyAttachments")
    public static LibertyServer server;

    // Client with URI and EndpointReference policy attachment configurations
    public static String clientApp1 = "policyAttachmentsClient1";

    // Service with URI policy attachment configuration
    public static String service1 = "HelloService";
    public static String service2 = "HelloService2";
    public static String service3 = "HelloService3";
    public static String service4 = "HelloService4";

    public static String ClientServlet1 = "ClientServlet1";

    public static String helloWithoutPolicyResult = "helloWithoutPolicy invoked";
    public static String helloWithPolicyResult = "helloWithPolicy invoked";
    public static String helloWithOptionalPolicyResult = "helloWithOptionalPolicy invoked";
    public static String helloWithYouWantResult = "helloWithYouWant invoked";
    private final static String failureResult = "Internal Server Error";

    private final String failureNonAnonymous = "org.apache.cxf.binding.soap.SoapFault: Found anonymous address but non-anonymous required";

    public static String helloWithoutPolicy = "helloWithoutPolicy";
    public static String helloWithPolicy = "helloWithPolicy";
    public static String helloWithOptionalPolicy = "helloWithOptionalPolicy";
    public static String helloWithYouWant = "helloWithYouWant";

    private static final int CONN_TIMEOUT = 300;

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive war = ShrinkHelper.buildDefaultApp("policyAttachmentsClient1", "com.ibm.ws.policyattachments.service1", "com.ibm.ws.policyattachments.client1",
                                                      "com.ibm.ws.policyattachments.client1.service1");

        ExplodedShrinkHelper.explodedArchiveToDestination(server, war, "apps");
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (server == null) {
            return;
        }

        if (server.isStarted()) {
            server.stopServer("SRVE0777E", "SRVE0315E");
        }
    }

    @Before
    public void start() throws Exception {
        if (server != null) {
            server.startServer();
        }
    }

    @After
    public void stop() throws Exception {
        if (server.isStarted()) {
            server.stopServer("SRVE0777E", "SRVE0315E");
        }

    }

    /*
     * Positive Test: Validates that the WS-Addressing Policy `NonAnonymousResponses` can be attached
     * to both WebService and Client via the policy-attachment-client/server.xml files.
     *
     * WS-Addressing's `NonAnonymousResponses` property requires that we validate that the <ReplyTo> header
     * has any other value `http://www.w3.org/2005/08/addressing/anonymous`
     *
     * The `HelloClientReplyToHandler`is used to modify the <ReplyTo> value to a static address prior to the Client side request message going out over the wire .
     * This does cause the reply to go to a second address, but that portion of the test can be safely ignored.
     *
     * Service and Client used by test: Web Service impl - HelloService1, Web Service Client - HelloService
     */
    @Test
    public void testPolicyAttachments_serviceURI_NonAnonymousResponses() {
        commonTest(clientApp1, ClientServlet1, service1, "helloWithPolicy", helloWithPolicyResult);
    }

    /*
     * Positive Test: Validates that the WS-Addressing Policy `AnonymousResponses` can be attached
     * to both WebService and Client via the policy-attachment-client/server.xml files.
     *
     * WS-Addressing's `AnonymousResponses` property requires that we validate that the <ReplyTo> header
     * has a `http://www.w3.org/2005/08/addressing/anonymous` value
     *
     * Service and Client used by test: Web Service impl - HelloService2, Web Service Client - HelloService2
     */
    @Test
    public void testPolicyAttachments_serviceURI_AnonymousResponses() throws Exception {
        commonTest(clientApp1, ClientServlet1, service2, "helloWithPolicy", helloWithPolicyResult);
    }

    /*
     * Negative Test: Validates that if the Client side policy uses `NonAnonymousResponses` and the service sides
     * uses `AnonymousResponses` that a SoapFault is generated for mismatched policies.
     *
     * The `HelloClientReplyToHandler`is used to modify the <ReplyTo> value to a static address prior to the Client side request message going out over the wire .
     *
     * Service and Client used by test: Web Service impl - HelloService3, Web Service Client - HelloService3
     *
     */
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
    @Test
    @ExpectedFFDC("javax.xml.ws.soap.SOAPFaultException")
    public void testPolicyAttachments_serviceURI_Annon_NonResponses_Failure() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        commonTest(clientApp1, ClientServlet1, service3, "helloWithPolicy", failureResult);

        assertNotNull("Expected to find " + failureNonAnonymous + " but error not found in logs", server.findStringsInLogsAndTraceUsingMark(failureNonAnonymous));
    }

    /*
     * Negative Test: Jakarta Version of above test
     */
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION })
    @Test
    @ExpectedFFDC("jakarta.xml.ws.soap.SOAPFaultException")
    public void testPolicyAttachments_serviceURI_Annon_NonResponses_JakartaFailure() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        commonTest(clientApp1, ClientServlet1, service3, "helloWithPolicy", failureResult);

        assertNotNull("Expected to find " + failureNonAnonymous + " but error not found in logs", server.findStringsInLogsAndTraceUsingMark(failureNonAnonymous));
    }

    /*
     * Negative Test: Validates that if the Client side policy uses `NonAnonymousResponses` and the service sides
     * uses `AnonymousResponses` that a SoapFault is generated for mismatched policies.
     *
     * The `HelloClientReplyToHandler`is used to modify the <ReplyTo> value to a static address prior to the Client side request message going out over the wire .
     *
     * Service and Client used by test: Web Service impl - HelloService4, Web Service Client - HelloService4
     */
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
    @Test
    @ExpectedFFDC("javax.xml.ws.soap.SOAPFaultException")
    public void testPolicyAttachments_serviceURI_Non_AnnonResponses_Failure() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        commonTest(clientApp1, ClientServlet1, service4, "helloWithPolicy", failureResult);

        assertNotNull("Expected to find " + failureNonAnonymous + " but error not found in logs", server.findStringsInLogsAndTraceUsingMark(failureNonAnonymous));
    }

    /*
     * Negative Test: Jakarta Version of above test
     */
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION })
    @Test
    @ExpectedFFDC("jakarta.xml.ws.soap.SOAPFaultException")
    public void testPolicyAttachments_serviceURI_Non_AnnonResponses_JakartaFailure() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        commonTest(clientApp1, ClientServlet1, service3, "helloWithPolicy", failureResult);

        assertNotNull("Expected to find " + failureNonAnonymous + " but error not found in logs", server.findStringsInLogsAndTraceUsingMark(failureNonAnonymous));
    }

    /*
     * Utility method that constructs the URL of the request using paramters passed by the test methods
     * then checks if the test is a negative test or a positive test. Depending on the result it will
     * call either the executeFailureApp method or executeApp method
     */
    public static void commonTest(String clientName, String servletName, String serviceName, String testMethod, String expectedResult) {
        Log.info(ColocatedDynamicPolicyAttachmentsTest.class, testMethod, "This test is for " + clientName + " and " + serviceName + ", method " + expectedResult);

        String resultURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + clientName + "/" + servletName + "?portName=" + serviceName + "&method="
                           + testMethod;

        String result = "";

        if (expectedResult == failureResult) {
            try {
                server.setMarkToEndOfLog(server.getDefaultLogFile());
                result = executeFailureApp(resultURL);

                System.out.println("Expect result is " + expectedResult);
                System.out.println("Executed result is " + result);
                assertTrue("Expected result " + result + " to match expectedResult " + expectedResult, result.contains(failureResult));
            } catch (Exception e) {

            }
        } else {
            try {

                result = executeApp(resultURL);
                System.out.println("Expect result is " + expectedResult);
                System.out.println("Actual result is " + result);
                //assertTrue("Check result, expect is " + expectResult
                //           + ", result is " + result, expectResult.equals(result));

            } catch (Exception e) {
                fail("Exception happens: " + e.toString());
            }
        }
    }

    /*
     * Utility method that invokes the ClientServlet1 servlet which expects a 200 or good response
     */
    public static String executeApp(String url) throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(url),
                                                            HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();
        Log.info(ColocatedDynamicPolicyAttachmentsTest.class, "executeApp", "Execute WS-Addressing Policy Attachment test from " + url);
        return result;
    }

    /*
     * Utility method that invokes the ClientServlet1 servlet which expects a 500 or INTERNAL_SERVER_ERROR response
     */
    public static String executeFailureApp(String url) throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(url),
                                                            HttpURLConnection.HTTP_INTERNAL_ERROR, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();
        Log.info(ColocatedDynamicPolicyAttachmentsTest.class, "executeFailureApp", "Execute Failing WS-Addressing Policy Attachment test from " + url);
        return result;
    }

    /*
     * Utility method for building base address of the serlvet.
     */
    protected String getBaseUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }

}
