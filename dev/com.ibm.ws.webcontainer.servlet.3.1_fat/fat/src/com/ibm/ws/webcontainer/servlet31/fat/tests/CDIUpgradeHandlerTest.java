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
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertEquals;
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

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

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
@MinimumJavaLevel(javaLevel = 7)
public class CDIUpgradeHandlerTest extends LoggingTest {

    // Server instance ...

    /** A single shared server used by all of the tests. */
    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_cdiUpgradeHandlerServer");

    /**
     * Log text at the info level. Use the shared server to perform the logging.
     * 
     * @param text Text which is to be logged.
     */
    public static void logInfo(String text) {
        SHARED_SERVER.logInfo(text);
    }

    /**
     * Wrapper for {@link #createWebBrowserForTestCase()} with relaxed protection.
     * 
     * @return A web brower.
     */
    protected WebBrowser createWebBrowser() {
        return createWebBrowserForTestCase();
    }

    // Test helpers ...

    protected static final boolean IS_HTTP_URL = true;
    protected static final boolean IS_NOT_HTTP_URL = false;

    /**
     * Answer the URL text for a relative URI for the shared server.
     * 
     * @param relativeURL The relative URI for the URL.
     * 
     * @return Text of the URL.
     */
    protected String getRequestURL(String relativeURL) {
        return SHARED_SERVER.getServerUrl(IS_HTTP_URL, relativeURL);
    }

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
     * @param webBrowser Simulated web browser instance through which the request is made.
     * @param requestPath The path which will be requested.
     * @param expectedResponses Expected response text. All elements are tested.
     * @param unexpectedResponses Unexpected response text. All elements are tested.
     * @return The encapsulated response.
     * 
     * @throws Exception Thrown if the expected response text is not present or if the
     *             unexpected response text is present.
     */
    protected WebResponse verifyResponse(WebBrowser webBrowser, String resourceURL, String[] expectedResponses, String[] unexpectedResponses) throws Exception {
        return SHARED_SERVER.verifyResponse(webBrowser, resourceURL, expectedResponses, unexpectedResponses); // throws Exception
    }

    /** Standard failure text. Usually unexpected. */
    public static final String[] FAILED_RESPONSE = new String[] { "FAILED" };

    public static final String LOG_CLASS_NAME = "CDIUpgradeHandlerTestImpl";

    private static final Logger LOG = Logger.getLogger(CDIUpgradeHandlerTest.class.getName());

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

        WebBrowser browserS1R1 = createWebBrowser();
        browserS1R1.setAutoRedirect(false);
        browserS1R1.setAcceptCookies(true);

        Properties propertiesS1R1 = getNoUpgradeRequestProperties(testName);
        browserS1R1.setFormValues(propertiesS1R1);

        // @formatter:off
        WebResponse responseS1R1 =
            verifyResponse(browserS1R1, UPGRADE_URL,
                                           new String[] { EXPECTED_NO_UPGRADE_RESPONSE },
                                           CDIUpgradeHandlerTest.FAILED_RESPONSE);
        // @formatter:on

        List<String> sessionS1R1 = responseS1R1.getCookie("JSESSIONID", true);
        String sessionIdS1R1;
        if ((sessionS1R1 != null) && (!sessionS1R1.isEmpty())) {
            sessionIdS1R1 = sessionS1R1.get(0);
        } else {
            sessionIdS1R1 = null;
        }

        displayLog(sessionIdS1R1, EXPECTED_LOG1, CDIUpgradeHandlerTest.FAILED_RESPONSE);

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
        SHARED_SERVER.getLibertyServer().waitForStringInLogUsingLastOffset("CDITestWriteListener: onWritePossible: EXIT");

        LOG.info("implTestCDIUpgrade : Now check the results and compare it with [ EXPECTED_LOG2 ]");

        displayLog(NULL_SESSION_ID,
                   EXPECTED_LOG2,
                   CDIUpgradeHandlerTest.FAILED_RESPONSE);

        performUpgrade();

        SHARED_SERVER.getLibertyServer().waitForStringInLogUsingLastOffset("CDITestWriteListener: onWritePossible: EXIT");

        LOG.info("implTestCDIUpgrade : Now check the results and compare it with  [ EXPECTED_LOG3 ]");

        displayLog(NULL_SESSION_ID,
                   EXPECTED_LOG3,
                   CDIUpgradeHandlerTest.FAILED_RESPONSE);

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

    private void displayLog(String sessionId, String[] expectedLog, String[] unexpectedLog) throws Exception {
        WebBrowser logBrowser = createWebBrowser();
        logBrowser.setAutoRedirect(false);
        logBrowser.setAcceptCookies(true);

        Properties logProperties = getLogRequestProperties();

        if (sessionId != null) {
            logProperties.setProperty("JSESSIONID", sessionId);
        }

        logBrowser.setFormValues(logProperties);

        verifyResponse(logBrowser, UPGRADE_URL,
                       expectedLog,
                       unexpectedLog);
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
