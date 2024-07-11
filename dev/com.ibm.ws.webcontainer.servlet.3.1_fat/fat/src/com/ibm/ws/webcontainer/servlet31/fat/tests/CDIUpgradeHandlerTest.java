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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Map;

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
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.Header;

/**
 * CDI Tests
 *
 * These cases are tested:
 *
 * <ul>
 * <li>Constructor Injection
 * <li>PostConstruct
 * <li>Field Injection (Request Scoped, Qualified)
 * <li>Field Injection (Session Scoped)
 * <li>Field Injection (Application Scoped)
 * <li>Produces Injection
 * <li>Consumes Injection
 * <li>Initializer Method Injection
 * <li>PreDestroy
 * <li>Interceptor
 * </ul>
 *
 * These cases are not tested:
 * <ul>
 * <li>Decorator (not implemented)
 * </ul>
 */
@RunWith(FATRunner.class)
public class CDIUpgradeHandlerTest {

    private static final Logger LOG = Logger.getLogger(CDIUpgradeHandlerTest.class.getName());

    // Server instance ...
    @Server("servlet31_cdiUpgradeHandlerServer")
    public static LibertyServer LS;

    private static final String CDI12_TEST_V2_JAR_NAME = "CDI12TestV2";
    private static final String CDI12_TEST_V2_UPGRADE_APP_NAME = "CDI12TestV2Upgrade";

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the CDI12TestV2 jar to add to the war app as a lib
        JavaArchive CDI12TestV2Jar = ShrinkHelper.buildJavaArchive(CDI12_TEST_V2_JAR_NAME + ".jar",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2");
        CDI12TestV2Jar = (JavaArchive) ShrinkHelper.addDirectory(CDI12TestV2Jar, "test-applications/CDI12TestV2.jar/resources");
        // Build the war app CDI12TestV2Upgrade.war and add the dependencies
        WebArchive CDI12TestV2UpgradeApp = ShrinkHelper.buildDefaultApp(CDI12_TEST_V2_UPGRADE_APP_NAME + ".war",
                                                                        "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2upgrade.war.cdi.upgrade.handlers",
                                                                        "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2upgrade.war.cdi.upgrade.servlets");
        CDI12TestV2UpgradeApp = CDI12TestV2UpgradeApp.addAsLibrary(CDI12TestV2Jar);
        
        // Export the application.
        ShrinkHelper.exportDropinAppToServer(LS, CDI12TestV2UpgradeApp);

        // Start the server and use the class name so we can find logs easily
        LS.startServer(CDIUpgradeHandlerTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (LS != null && LS.isStarted()) {
            LS.stopServer();
        }
    }

    /**
     * Log text at the info level.
     *
     * @param text Text which is to be logged.
     */
    public static void logInfo(String text) {
        LOG.info(text);
    }

    // Test helpers ...

    /**
     * Answer the URL text for a relative URI for the server.
     *
     * @param relativeURL The relative URI for the URL.
     *
     * @return Text of the URL.
     */
    protected String getRequestURL(String relativeURL) {
        return "http://" + LS.getHostname() + ":" + LS.getHttpDefaultPort() + relativeURL;
    }

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

    public static final String LOG_CLASS_NAME = "CDIUpgradeHandlerTestImpl";

    private static void logStart(String methodName, String testName) {
        LOG.info("\n *****************START********** " + LOG_CLASS_NAME + ": TEST: " + testName + " (" + methodName + ")");
    }

    private static void logFinish(String methodName, String testName) {
        LOG.info("\n *****************FINISH********* " + LOG_CLASS_NAME + ": TEST: " + testName + " (" + methodName + ")");
    }

    @SuppressWarnings("unused")
    private static void logInfo(String methodName, String testName, String text) {
        LOG.info(text + ": " + LOG_CLASS_NAME + ": TEST: " + testName + " (" + methodName + ")");
    }

    private static void logAndFail(String methodName, String testName, String text, Exception e) {
        LOG.info(text + " in [ " + testName + " (" + methodName + ") ]");
        e.printStackTrace();
        fail("Exception from " + testName + " (" + methodName + "): " + e.getMessage() + " [ " + e + " ]");
    }

    /**
     * Verify that the upgrade servlet is available and responds correctly
     * with upgrade not requested.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testCDIUpgradeHandlerNoUpgrade() throws Exception {
        implTestCDINoUpgrade();
    }

    /**
     * Verify that the upgrade servlet is available and responds correctly
     * with upgrade requested.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testCDIUpgradeHandlerUpgrade() throws Exception {
        implTestCDIUpgrade();
    }

    // Test suite (implementation) ...
    //
    // Test a no-upgrade case, then an upgrade case.

    public static final String UPGRADE_CONTEXT_ROOT = "/CDI12TestV2Upgrade";
    public static final String UPGRADE_URL_FRAGMENT = "/CDIUpgrade";
    public static final String UPGRADE_URL = UPGRADE_CONTEXT_ROOT + UPGRADE_URL_FRAGMENT;

    public static final String EXPECTED_NO_UPGRADE_RESPONSE = "NoUpgrade";

    public static final String HEADER_FIELD_SHOW_LOG = "ShowLog";
    public static final String APPLICATION_LOG_VALUE = "Application";

    private Properties getNoUpgradeRequestProperties(String testName) {
        Properties noUpgradeRequestProperties = new Properties();
        noUpgradeRequestProperties.setProperty("TestName", testName);
        return noUpgradeRequestProperties;
    }

    private Properties getLogRequestProperties() {
        Properties logRequestProperties = new Properties();
        logRequestProperties.setProperty(HEADER_FIELD_SHOW_LOG, APPLICATION_LOG_VALUE);
        return logRequestProperties;
    }

    /**
     * Test that the servlet is up and running, and can handle a no-upgrade
     * request.
     *
     * The response must be "NoUpgrade".
     */
    public void implTestCDINoUpgrade() throws Exception {
        String methodName = "implTestCDINoUpgrade";
        String testName = "testCDINoUpgrade";

        logStart(methodName, testName);
        
        HttpClientParams params = new HttpClientParams();
        params.setCookiePolicy(CookiePolicy.DEFAULT);
        HttpClient client = new HttpClient(params);
        Properties propertiesS1R1 = getNoUpgradeRequestProperties(testName);
        // Taken directly from HttpClientBrowser in com.ibm.ws.fat.util.browser
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        if (propertiesS1R1 != null) {
            for (Map.Entry<Object, Object> entry : propertiesS1R1.entrySet()) {
                nameValuePairs.add(new NameValuePair((String) entry.getKey(), (String) entry.getValue()));
            }
        }

        NameValuePair[] postParams = nameValuePairs.toArray(new NameValuePair[] {});

        verifyStringsInResponse(client, UPGRADE_CONTEXT_ROOT, UPGRADE_URL_FRAGMENT, new String[] { EXPECTED_NO_UPGRADE_RESPONSE }, postParams);

        verifyOutputMatchesExpectedLog(EXPECTED_LOG1);

        logFinish(methodName, testName);
    }

    public static final String TEST_DATA = "0123456789abcdefghijklmnopqrstuvwxyz";

    public void implTestCDIUpgrade() throws Exception {
        String methodName = "implTestCDIUpgrade";
        String testName = "testCDIUpgrade";

        logStart(methodName, testName);

        performUpgrade();
        // make sure server side is finished onWritePossible
        // wait till see this message CDITestWriteListener: onWritePossible: EXIT
        LS.waitForStringInLogUsingLastOffset("CDITestWriteListener: onWritePossible: EXIT");

        LOG.info("implTestCDIUpgrade : Now check the results and compare it with [ EXPECTED_LOG2 ]");

        verifyOutputMatchesExpectedLog(EXPECTED_LOG2);

        performUpgrade();

        LS.waitForStringInLogUsingLastOffset("CDITestWriteListener: onWritePossible: EXIT");

        LOG.info("implTestCDIUpgrade : Now check the results and compare it with  [ EXPECTED_LOG3 ]");

        verifyOutputMatchesExpectedLog(EXPECTED_LOG3);

        logFinish(methodName, testName);
    }

    private void performUpgrade() throws Exception {
        String methodName = "performUpgrade";
        String testName = "testCDIUpgradeReadListener";

        URL url;
        try {
            url = new URL(getRequestURL(UPGRADE_URL)); // 'new URL' throws MalformedURLException
        } catch (MalformedURLException e) {
            String failureMessage = "Unexpected exception";
            logAndFail(methodName, testName, failureMessage, e); // Never returns
            return; // Only present to avoid a warning.
        }

        Socket socket = null;
        try {
            socket = openSocket(url, UPGRADE_URL);
            String outputData = upgradeAndExchange(testName, socket, UPGRADE_URL, TEST_DATA);
            assertEquals(TEST_DATA, outputData);

        } catch (Exception e) {
            String failureMessage = "Unexpected exception";
            logAndFail(methodName, testName, failureMessage, e); // Never returns
            return; // Only present to avoid a warning.

        } finally {
            if (socket != null) {
                closeSocket(socket, UPGRADE_URL);
            }
        }
    }

    // The core upgrade processing step ...

    private static final String CRLF = "\r\n";
    private static final String TEST_PROTOCOL = "TestUpgrade";
    private static final byte TERMINATION_CHAR = '\0';

    // Helper method common to all tests.  Read and write from the server.
    // Send and receive appropriate output and input for each test through a socket.

    private String upgradeAndExchange(String testName,
                                      Socket socket, String requestURL,
                                      String exchangeData) throws Exception {

        BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // 'getInputStream' throws IOException

        try {
            BufferedWriter socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            // 'getOutputStream' throws IOException

            try {
                LOG.info("Request upgrade to [ " + TEST_PROTOCOL + " ]");
                socketWriter.write("POST " + requestURL + " HTTP/1.1" + CRLF); // throws IOException
                socketWriter.write("Test: " + testName + CRLF); // throws IOException
                socketWriter.write("Upgrade: " + TEST_PROTOCOL + CRLF); // throws IOException
                socketWriter.write("Connection: Upgrade" + CRLF); // throws IOException
                socketWriter.write(CRLF); // throws IOException
                socketWriter.flush(); // throws IOException

                String confirmationLine = "";
                while ((confirmationLine = socketReader.readLine()) != null) { // throws IOException
                    LOG.info("\t" + confirmationLine);
                    if (confirmationLine.trim().equals("")) {
                        break;
                    }
                }
                LOG.info("Successful upgrade to [ " + TEST_PROTOCOL + " ]");

                LOG.info("Sending data [ " + exchangeData + " ]");
                socketWriter.write(exchangeData); // throws IOException
                socketWriter.write(TERMINATION_CHAR);
                socketWriter.flush(); // throws IOException

                LOG.info("Reading data");
                String responseData = socketReader.readLine(); // throws IOException
                LOG.info("Read data [ " + responseData + " ]");

                return responseData;

            } finally {
                socketWriter.close(); // throws IOException
            }

        } finally {
            socketReader.close(); // throws IOException
        }
    }

    // Socket utility ...

    private Socket openSocket(URL url, String urlText) throws Exception {
        LOG.info("Opening socket [ " + urlText + " ]");

        String urlHost = url.getHost();
        int urlPort = url.getPort();
        LOG.info("URL host [ " + urlHost + " ] Port [ " + Integer.toString(urlPort) + " ]");

        Socket socket = new Socket(urlHost, urlPort); // throws UnknownHostException, IOException

        LOG.info("Opened socket [ " + socket + " ]");
        return socket;
    }

    private void closeSocket(Socket socket, String urlText) throws Exception {
        if (socket.isClosed()) {
            LOG.info("Closing socket [ " + socket + " ] [ " + urlText + " ]: already closed");
            return;
        } else {
            LOG.info("Closing socket [ " + socket + " ] [ " + urlText + " ]");
        }

        IOException inputException;
        IOException outputException;

        try {
            socket.shutdownInput(); // throws IOException
            inputException = null;
        } catch (IOException e1) {
            inputException = e1;
            // Throw only after finishing the rest of the close.

        } finally {
            try {
                socket.shutdownOutput(); // throws IOException
                outputException = null;
            } catch (IOException e2) {
                outputException = e2;
                // Throw only after finishing the rest of the close.

            } finally {
                socket.close(); // throws IOException
                // OK to throw this one; nothing else left to do.
            }
        }

        if (outputException != null) {
            throw outputException;
        }
        if (inputException != null) {
            throw inputException;
        }

        LOG.info("Closed socket [ " + socket + " ] [ " + urlText + " ]");
    }

    //

    private static final String NULL_SESSION_ID = null;

    private void verifyOutputMatchesExpectedLog(String[] expectedLog) throws Exception {
        HttpClientParams params = new HttpClientParams();
        params.setCookiePolicy(CookiePolicy.DEFAULT);
        // Create our HttpClient
        HttpClient client = new HttpClient(params);

        // Make properties to mimic a form submission
        Properties logProperties = getLogRequestProperties();
        // Taken directly from HttpClientBrowser in com.ibm.ws.fat.util.browser
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        if (logProperties != null) {
            for (Map.Entry<Object, Object> entry : logProperties.entrySet()) {
                nameValuePairs.add(new NameValuePair((String) entry.getKey(), (String) entry.getValue()));
            }
        }

        // Convert properties into array that can be added as parameters to represent a form
        NameValuePair[] postParams = nameValuePairs.toArray(new NameValuePair[] {});

        verifyStringsInResponse(client, UPGRADE_CONTEXT_ROOT, UPGRADE_URL_FRAGMENT, expectedLog, postParams);
        
    }

    private void verifyStringsInResponse(HttpClient client, String contextRoot, String path, String[] expectedResponseStrings, NameValuePair[] postParams) throws Exception {
        PostMethod post = new PostMethod("http://" + LS.getHostname() + ":" + LS.getHttpDefaultPort() + contextRoot + path);
        post.addParameters(postParams);
        int responseCode = client.executeMethod(post);
        String responseBody = post.getResponseBodyAsString();
        LOG.info("Response : " + responseBody);
  
        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, responseCode);
  
        for (String expectedResponse : expectedResponseStrings) {
            assertTrue("The response did not contain: " + expectedResponse, responseBody.contains(expectedResponse));
        }
    }

    //

    // @formatter:off
    public static final String[] EXPECTED_LOG1 = {
        "Header [ Upgrade ] [ null ]: No upgrade",
        "Parameter [ ShowLog ] [ Application ]: Show application log",
        "Application Log"
    };

    public static final String[] EXPECTED_LOG2 = {
        "Header [ Upgrade ] [ TestUpgrade ]: Upgrade",
        "Parameter [ ShowLog ] [ Application ]: Show application log",
        "Application Log",

        ":CDITestHttpUpgradeHandler:init:Entry:",
        ":CDITestHttpUpgradeHandler:init:Exit:",
        ":CDITestHttpUpgradeHandler:init:Field:Application:ApplicationFieldBean:Null:",
        ":CDITestHttpUpgradeHandler:init:Field:Dependent:DependentFieldBean:Null:",
        ":CDITestHttpUpgradeHandler:init:Method:Dependent:MethodBean:Null:",

        ":CDITestHttpUpgradeHandler:start:PostConstruct:",
        ":CDITestHttpUpgradeHandler:stop:PreDestroy:",
        ":CDITestHttpUpgradeHandler:logProducesText:Application:UpgradeProducesBean:0:",

        ":CDITestHttpUpgradeHandler:setReadListener:Entry:",
        ":CDITestHttpUpgradeHandler:setReadListener:Exit:",
        ":CDITestHttpUpgradeHandler:setWriteListener:Entry:",
        ":CDITestHttpUpgradeHandler:setWriteListener:Exit:",

        ":CDITestReadListener:onDataAvailable:Entry:",
        ":CDITestReadListener:onDataAvailable:Exit:",
        ":CDITestReadListener:onDataAvailable:Field:Application:ApplicationFieldBean:RV:",
        ":CDITestReadListener:onDataAvailable:Field:Dependent:DependentFieldBean:RV:",
        ":CDITestReadListener:onDataAvailable:Method:Dependent:MethodBean:RV:",

        ":CDITestWriteListener:onWritePossible:Entry:",
        ":CDITestWriteListener:onWritePossible:Exit:",
        ":CDITestWriteListener:onWritePossible:Field:Application:ApplicationFieldBean:RV:WP:RA:",
        ":CDITestWriteListener:onWritePossible:Field:Dependent:DependentFieldBean:RV:WP:RA:",
        ":CDITestWriteListener:onWritePossible:Method:Dependent:MethodBean:RV:WP:RA:",

        ":CDITestReadListener:onAllDataRead:Entry:",
        ":CDITestReadListener:onAllDataRead:Exit:",
        ":CDITestReadListener:onAllDataRead:Field:Dependent:DependentFieldBean:RV:WP:RA:",
        ":CDITestReadListener:onAllDataRead:Field:Application:ApplicationFieldBean:RV:WP:RA:",
        ":CDITestReadListener:onAllDataRead:Method:Dependent:MethodBean:RV:WP:RA:",

        ":CDITestHttpUpgradeHandler:destroy:Entry:",
        ":CDITestHttpUpgradeHandler:destroy:Exit:",
        ":CDITestHttpUpgradeHandler:destroy:Field:Application:ApplicationFieldBean:RV:WP:",
        ":CDITestHttpUpgradeHandler:destroy:Field:Dependent:DependentFieldBean:RV:WP:",
        ":CDITestHttpUpgradeHandler:destroy:Method:Dependent:MethodBean:RV:WP:"
    };

    public static final String[] EXPECTED_LOG3 = {
        "Header [ Upgrade ] [ TestUpgrade ]: Upgrade",
        "Parameter [ ShowLog ] [ Application ]: Show application log",
        "Application Log",

        ":CDITestHttpUpgradeHandler:destroy:Entry:",
        ":CDITestHttpUpgradeHandler:destroy:Exit:",
        ":CDITestHttpUpgradeHandler:destroy:Field:Application:ApplicationFieldBean:RV:WP:RA:RV:WP:",
        ":CDITestHttpUpgradeHandler:destroy:Field:Dependent:DependentFieldBean:RV:WP:",
        ":CDITestHttpUpgradeHandler:destroy:Method:Dependent:MethodBean:RV:WP:",

        ":CDITestHttpUpgradeHandler:init:Entry:",
        ":CDITestHttpUpgradeHandler:init:Exit:",
        ":CDITestHttpUpgradeHandler:init:Field:Application:ApplicationFieldBean:RV:WP:RA:",
        ":CDITestHttpUpgradeHandler:init:Field:Dependent:DependentFieldBean:Null:",
        ":CDITestHttpUpgradeHandler:init:Method:Dependent:MethodBean:Null:",

        ":CDITestHttpUpgradeHandler:logProducesText:Application:UpgradeProducesBean:0:",
        ":CDITestHttpUpgradeHandler:start:PostConstruct:",
        ":CDITestHttpUpgradeHandler:stop:PreDestroy:",

        ":CDITestHttpUpgradeHandler:setReadListener:Entry:",
        ":CDITestHttpUpgradeHandler:setReadListener:Exit:",

        ":CDITestReadListener:onDataAvailable:Entry:",
        ":CDITestReadListener:onDataAvailable:Exit:",
        ":CDITestReadListener:onDataAvailable:Field:Application:ApplicationFieldBean:RV:WP:RA:RV:",
        ":CDITestReadListener:onDataAvailable:Field:Dependent:DependentFieldBean:RV:",
        ":CDITestReadListener:onDataAvailable:Method:Dependent:MethodBean:RV:",

        ":CDITestReadListener:onAllDataRead:Entry:",
        ":CDITestReadListener:onAllDataRead:Exit:",
        ":CDITestReadListener:onAllDataRead:Field:Dependent:DependentFieldBean:RV:WP:RA:",
        ":CDITestReadListener:onAllDataRead:Field:Application:ApplicationFieldBean:RV:WP:RA:",
        ":CDITestReadListener:onAllDataRead:Method:Dependent:MethodBean:RV:WP:RA:",

        ":CDITestHttpUpgradeHandler:setWriteListener:Entry:",
        ":CDITestHttpUpgradeHandler:setWriteListener:Exit:",

        ":CDITestWriteListener:onWritePossible:Entry:",
        ":CDITestWriteListener:onWritePossible:Exit:",
        ":CDITestWriteListener:onWritePossible:Field:Application:ApplicationFieldBean:RV:WP:RA:RV:WP:",
        ":CDITestWriteListener:onWritePossible:Field:Dependent:DependentFieldBean:RV:WP:RA:",
        ":CDITestWriteListener:onWritePossible:Method:Dependent:MethodBean:RV:WP:RA:"
    };
    // @formatter:off
}
