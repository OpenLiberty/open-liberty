/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE8_OR_LATER_FEATURES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * All Servlet 3.1 tests with all applicable server features enabled.
 */
@RunWith(FATRunner.class)
public class WCServerTest {

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    private static final String SESSION_ID_LISTENER_JAR_NAME = "SessionIdListener";
    private static final String TEST_SERVLET_31_JAR_NAME = "TestServlet31";
    private static final String TEST_METADATA_COMPLETE_JAR_NAME = "TestMetadataComplete";
    private static final String TEST_METADATA_COMPLETE_EXCLUDED_FRAGMENT_JAR_NAME = "TestMetadataCompleteExcludedFragment";
    private static final String SINGLETON_STORE_JAR_NAME = "SingletonStore";
    private static final String TEST_SERVLET_31_APP_NAME = "TestServlet31";
    private static final String TEST_METADATA_COMPLETE_APP_NAME = "TestMetadataComplete";
    private static final String SESSION_ID_ADD_LISTENER_APP_NAME = "SessionIdListenerAddListener";
    private static final String SESSION_ID_LISTENER_APP_NAME = "SessionIdListener";
    private static final String SERVLET_CONTEXT_ADD_LISTENER_APP_NAME = "ServletContextAddListener";
    private static final String SERVLET_CONTEXT_CREATE_LISTENER_APP_NAME = "ServletContextCreateListener";

    @Server("servlet31_wcServer")
    public static LibertyServer LS;

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the jars to add to the war app as a lib
        JavaArchive SessionIdListenerJar = ShrinkHelper.buildJavaArchive(SESSION_ID_LISTENER_JAR_NAME + ".jar",
                                                                         "com.ibm.ws.webcontainer.servlet_31_fat.sessionidlistener.jar.listeners",
                                                                         "com.ibm.ws.webcontainer.servlet_31_fat.sessionidlistener.jar.servlets");
        JavaArchive TestServlet31Jar = ShrinkHelper.buildJavaArchive(TEST_SERVLET_31_JAR_NAME + ".jar",
                                                                     "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.jar.servlets");
        JavaArchive TestMetadataCompleteJar = ShrinkHelper.buildJavaArchive(TEST_METADATA_COMPLETE_JAR_NAME + ".jar",
                                                                            "com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacomplete.jar.initializer");
        TestMetadataCompleteJar = (JavaArchive) ShrinkHelper.addDirectory(TestMetadataCompleteJar, "test-applications/TestMetadataComplete.jar/resources");
        JavaArchive TestMetadataCompleteExcludeJar = ShrinkHelper.buildJavaArchive(TEST_METADATA_COMPLETE_EXCLUDED_FRAGMENT_JAR_NAME + ".jar",
                                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacompleteexcludedfragment.jar.initializer",
                                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacompleteexcludedfragment.jar.servlets");
        TestMetadataCompleteExcludeJar = (JavaArchive) ShrinkHelper.addDirectory(TestMetadataCompleteExcludeJar,
                                                                                 "test-applications/TestMetadataCompleteExcludedFragment.jar/resources");

        JavaArchive SingletonStoreJar = ShrinkHelper.buildJavaArchive(SINGLETON_STORE_JAR_NAME + ".jar",
                                                                      "com.ibm.ws.webcontainer.servlet_31_fat.singletonstore.jar.teststorage");
        // Build the war apps and add the dependencies
        WebArchive TestServlet31App = ShrinkHelper.buildDefaultApp(TEST_SERVLET_31_APP_NAME + ".war",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.listeners");
        TestServlet31App = (WebArchive) ShrinkHelper.addDirectory(TestServlet31App, "test-applications/TestServlet31.war/resources");
        TestServlet31App = TestServlet31App.addAsLibraries(SessionIdListenerJar, TestServlet31Jar);
        WebArchive TestMetadataCompleteApp = ShrinkHelper.buildDefaultApp(TEST_METADATA_COMPLETE_APP_NAME + ".war",
                                                                          "com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacomplete.war.servlets",
                                                                          "com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacomplete.war.stack");
        TestMetadataCompleteApp = TestMetadataCompleteApp.addAsLibraries(TestMetadataCompleteJar, TestMetadataCompleteExcludeJar, SingletonStoreJar);
        TestMetadataCompleteApp = (WebArchive) ShrinkHelper.addDirectory(TestMetadataCompleteApp, "test-applications/TestMetadataComplete.war/resources");
        WebArchive SessionIdAddListenerApp = ShrinkHelper.buildDefaultApp(SESSION_ID_ADD_LISTENER_APP_NAME + ".war",
                                                                          "com.ibm.ws.webcontainer.servlet_31_fat.sessionidlisteneraddlistener.war.listeners");
        SessionIdAddListenerApp = (WebArchive) ShrinkHelper.addDirectory(SessionIdAddListenerApp, "test-applications/SessionIdListenerAddListener.war/resources");
        SessionIdAddListenerApp = SessionIdAddListenerApp.addAsLibraries(SessionIdListenerJar);
        WebArchive SessionIdListenerApp = ShrinkHelper.buildDefaultApp(SESSION_ID_LISTENER_APP_NAME + ".war");
        SessionIdListenerApp = (WebArchive) ShrinkHelper.addDirectory(SessionIdListenerApp, "test-applications/SessionIdListener.war/resources");
        SessionIdListenerApp = SessionIdListenerApp.addAsLibraries(SessionIdListenerJar);
        WebArchive ServletContextAddListenerApp = ShrinkHelper.buildDefaultApp(SERVLET_CONTEXT_ADD_LISTENER_APP_NAME + ".war",
                                                                               "com.ibm.ws.webcontainer.servlet_31_fat.servletcontextaddlistener.war.listeners");
        ServletContextAddListenerApp = ServletContextAddListenerApp.addAsLibraries(TestServlet31Jar);
        WebArchive ServletContextCreateListenerApp = ShrinkHelper.buildDefaultApp(SERVLET_CONTEXT_CREATE_LISTENER_APP_NAME + ".war",
                                                                                  "com.ibm.ws.webcontainer.servlet_31_fat.servletcontextcreatelistener.war.listeners");
        ServletContextCreateListenerApp = ServletContextCreateListenerApp.addAsLibraries(TestServlet31Jar);

        // Export the applications
        ShrinkHelper.exportDropinAppToServer(LS, TestServlet31App);
        ShrinkHelper.exportDropinAppToServer(LS, TestMetadataCompleteApp);
        ShrinkHelper.exportDropinAppToServer(LS, SessionIdAddListenerApp);
        ShrinkHelper.exportDropinAppToServer(LS, SessionIdListenerApp);
        ShrinkHelper.exportDropinAppToServer(LS, ServletContextAddListenerApp);
        ShrinkHelper.exportDropinAppToServer(LS, ServletContextCreateListenerApp);

        // Start the server and use the class name so we can find logs easily.
        LS.startServer(WCServerTest.class.getSimpleName() + ".log");

        LS.waitForStringInLog("CWWKZ0001I.* " + TEST_SERVLET_31_APP_NAME);
        LS.waitForStringInLog("CWWKZ0001I.* " + SESSION_ID_ADD_LISTENER_APP_NAME);
        LS.waitForStringInLog("CWWKZ0001I.* " + SERVLET_CONTEXT_ADD_LISTENER_APP_NAME);
        LS.waitForStringInLog("CWWKZ0001I.* " + SERVLET_CONTEXT_CREATE_LISTENER_APP_NAME);
        LS.waitForStringInLog("CWWKZ0001I.* " + SESSION_ID_LISTENER_APP_NAME);
        LS.waitForStringInLog("CWWKZ0001I.* " + TEST_METADATA_COMPLETE_APP_NAME);

    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (LS != null && LS.isStarted()) {
            LS.stopServer("SRVE9016E:.*", "CWWKZ0002E:.*", "SRVE8015E:.*", "SRVE9002E:.*", "SRVE9014E:.*");
        }
    }

    protected String parseResponse(String text, String beginText, String endText) {
        String s;
        String body = text;
        int beginTextIndex = body.indexOf(beginText);
        if (beginTextIndex < 0)
            return "begin text, " + beginText + ", not found";
        int endTextIndex = body.indexOf(endText, beginTextIndex);
        if (endTextIndex < 0)
            return "end text, " + endText + ", not found";
        s = body.substring(beginTextIndex + beginText.length(), endTextIndex);
        return s;
    }

    /**
     * Sample test for running with Servlet 3.1
     * This test is skipped for servlet-4.0, servlet-5.0, servlet-6.0, and servlet-6.1 because
     * there is already a test for this in the 4.0 fat bucket.
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    @SkipForRepeat(EE8_OR_LATER_FEATURES)
    public void test_Servlet31() throws Exception {
        Header[] headers = verifyStringsInResponse_getResponseHeaders(new HttpClient(), "/TestServlet31", "/MyServlet", new String[] { "Hello World" });

        // verify the X-Powered-By Response header
        for (Header h : headers) {
            if (h.getName().equals("X-Powered-By")) {
                LOG.info("X-Powered-By Header Found");
                assertEquals("X-Powered-By Response header was not 'Servlet/3.1'.", h.getValue(), "Servlet/3.1");
            }
        }
    }

    @Test
    public void test_ProgrammaticallyAddedServlet() throws Exception {
        // 130998: This tests that the servlet that was programmatically
        // added with a different servlet name in "MyServletContextListener"
        // was created and is accessible.
        verifyStringsInResponse(new HttpClient(), "/TestServlet31", "/ProgrammaticServlet", new String[] { "Hello World" });
    }

    @Test
    public void test_MetadataCompleteHandlesTypesServlet() throws Exception {
        verifyStringsInResponse(new HttpClient(), "/TestMetadataComplete", "/DisplayInits",
                                new String[] { "ParentServletInitializer: com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacomplete.war.servlets.DisplayInits",
                                               "HashSetChildInitializer: com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacomplete.war.stack.HelperMethodChild",
                                               "HashSetChildInitializer: com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacomplete.war.stack.HelperMethod" });
    }

    //This isn't duplicating testMetadataCompleteHandlesTypesServlet since we want granularity on the functions.
    //This is for excluded JARs only, the one above is for general SCI function.
    @Test
    public void test_MetadataCompleteExcludedHandlesTypesServlet() throws Exception {
        verifyStringsInResponse(new HttpClient(), "/TestMetadataComplete", "/DisplayInits", new String[] {});
    }

    /**
     * Verifies that container lifecycle events are behaving correctly (JSR-299 section 11.5).
     *
     * @throws Exception
     *                       if validation fails, or if an unexpected error occurs
     */
    @Test
    public void test_SessionIdListenerChangeServlet() throws Exception {
        // Make sure the test framework knows that SRVE9014E is expected

        test_SessionIdListener("/TestServlet31/SessionIdListenerChangeServlet");
    }

    /**
     * Perform the same test as "test_SessionIdListenerChangeServlet" except for this time the HttpSessionIdListener is
     * registered in the web.xml and not via the @WebListener annotation. The SessionIdListener application is used
     * for this test.
     *
     * @throws Exception
     */
    @Test
    public void test_SessionIdListenerRegisteredWebXml() throws Exception {
        // Make sure the test framework knows that SRVE9014E is expected

        test_SessionIdListener("/SessionIdListener/SessionIdListenerChangeServlet");
    }

    /**
     * Perform the same test as "test_SessionIdListenerChangeServlet" except for this time the HttpSessionIdListener is
     * registered via a ServletContextListener using the ServletContext.addListener API. The SessionIdListenerAddListener
     * application is used for this test.
     *
     * @throws Exception
     */
    @Test
    public void test_SessionIdListenerServletContextAddListener() throws Exception {
        // Make sure the test framework knows that SRVE9014E is expected

        test_SessionIdListener("/SessionIdListenerAddListener/SessionIdListenerChangeServlet");
    }

    /*
     * Common test code for HttpSessionIdListener tests.
     */
    private void test_SessionIdListener(String url) throws Exception {
        verifyStringsInResponse(new HttpClient(), url, "", new String[] { "Expected IllegalStateException" });
        HttpClient client = new HttpClient();
        String responseBody = verifyStringsInResponse_getResponseBody(client, url, "?getSessionFirst=true",
                                                                      new String[] { "Session id returned from changeSessionId", "Change count = 1" });
        String oldSessionId = parseResponse(responseBody, "Original session id = <sessionid>", "</sessionid>");
        String newSessionId = parseResponse(responseBody, "Session id returned from changeSessionId = <sessionid>", "</sessionid>");
        Assert.assertTrue("ids are equal: old=" + oldSessionId + ":new=" + newSessionId, !oldSessionId.equals(newSessionId));
        verifyStringsInResponse(client, url, "", new String[] { "Original session id = <sessionid>" + newSessionId + "</sessionid>" });
    }

    @Test
    public void test_RequestedSessionId() throws Exception {
        verifyStringsInResponse(new HttpClient(), "/TestServlet31", "/SessionIdTest;jsessionid=mysessionid",
                                new String[] { "Requested session id was mysessionid", "Requested Session id is invalid" });
    }

    @Test
    public void test_GetServerInfo() throws Exception {
        String v = System.getProperty("version", "");
        File f = new File("../build.image/wlp/lib/versions/WebSphereApplicationServer.properties");
        if (f.exists()) {
            Properties props = new Properties();
            try {
                props.load(new FileReader(f));
                v = props.getProperty("com.ibm.websphere.productVersion");
            } catch (IOException e) {
            }
        }
        verifyStringsInResponse(new HttpClient(), "/TestServlet31", "/GetServerInfoTest",
                                new String[] { "GetServerInfoTest: ServletContext.getServerInfo()=IBM WebSphere Liberty/" + v });
    }

    /**
     * Verifies that a response.reset works
     *
     * @throws Exception
     *                       if validation fails, or if an unexpected error occurs
     */
    @Test
    public void test_ResponseReset() throws Exception {
        String url = "/TestServlet31/ResponseReset?firstType=pWriter&secondType=pWriter";
        String body = verifyStringsInResponse_getResponseBody(new HttpClient(), url, "", new String[] { "SUCCESS" });
        Assert.assertTrue("contained content before the reset: url=" + url + "::" + body, body.indexOf("FAILURE") == -1);
        url = "/TestServlet31/ResponseReset?firstType=pWriter&secondType=outputStream";
        body = verifyStringsInResponse_getResponseBody(new HttpClient(), url, "", new String[] { "SUCCESS" });
        Assert.assertTrue("contained content before the reset: url=" + url + "::" + body, body.indexOf("FAILURE") == -1);
        url = "/TestServlet31/ResponseReset?firstType=outputStream&secondType=pWriter";
        body = verifyStringsInResponse_getResponseBody(new HttpClient(), url, "", new String[] { "SUCCESS" });
        Assert.assertTrue("contained content before the reset: url=" + url + "::" + body, body.indexOf("FAILURE") == -1);
        url = "/TestServlet31/ResponseReset?firstType=outputStream&secondType=outputStream";
        body = verifyStringsInResponse_getResponseBody(new HttpClient(), url, "", new String[] { "SUCCESS" });
        Assert.assertTrue("contained content before the reset: url=" + url + "::" + body, body.indexOf("FAILURE") == -1);
    }

    /**
     * Verifies that the ServletContext.getMinorVersion() returns 1 and
     * ServletContext.getMajorVersion() returns 3 for Servlet 3.1.
     * This test is skipped for servlet-4.0, servlet-5.0, servlet-6.0, and servlet-6.1 because
     * there is already a test for this in the 4.0 fat bucket.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(EE8_OR_LATER_FEATURES)
    public void test_ServletContextMinorMajorVersion() throws Exception {
        verifyStringsInResponse(new HttpClient(), "/TestServlet31", "/MyServlet?TestMajorMinorVersion=true", new String[] { "majorVersion: 3", "minorVersion: 1" });
    }

    /**
     * Verifies that the correct message is printed out when the class name specified for the ServletContext.addListener(String className)
     * API can not be found.
     *
     * @throws Exception
     */
    @Test
    public void test_ServletContextAddListener() throws Exception {

        // Make sure the test framework knows that SRVE8015E is expected

        verifyStringsInResponse(new HttpClient(), "/ServletContextAddListener", "/SimpleTestServlet", new String[] { "Hello World" });

        // application should be initialized with the above request, now check the logs for the proper output.
        Assert.assertNotNull(LS.findStringsInLogs("SRVE8015E:.*ThisListenerDoesNotExist"));
    }

    /**
     * This test case uses the ServletContext.createListener API to try and create a listener that
     * does not implement one of the expected listener interfaces. The test will ensure that the proper
     * exception is thrown in this scenario.
     */
    @Test
    public void test_ServletContextCreateListenerBadListener() throws Exception {

        // Make sure the test framework knows that SRVE9014E is expected

        // Drive a request to the SimpleTestServlet to initialize the application
        verifyStringsInResponse(new HttpClient(), "/ServletContextCreateListener", "/SimpleTestServlet", new String[] { "Hello World" });

        // Ensure that the proper exception was output
        LibertyServer server = LS;
        List<String> logMessage = server.findStringsInLogs("SRVE8014E:");

        Assert.assertNotNull("The correct message was not logged.", logMessage);

        // Ensure a REQUEST_INITIALIZED can be found in the logs.  If it can't be that means that a
        // correct listener was not created and added as expected.
        logMessage = server.findStringsInLogs("REQUEST_INITIALIZED");
        Assert.assertNotNull("REQUEST_INITIALIZED was not found in the logs. The listener must not have been created and added correctly",
                             logMessage);

    }

    /**
     * This test case verifies that writing Strings with more than one bytes per char to the ServletOutputStream
     * works as expected.
     */
    @Test
    public void test_ServletOutputStream_MultiByteCharEncoding() throws Exception {
        verifyResponseStringLength("/TestServlet31/MultiByteEncodingServlet?isAsync=false", "Привет мир");
    }

    /**
     * This test case verifies that writing Strings with more than one bytes per char to the ServletOutputStream
     * via a WriteListener works as expected.
     */
    @Test
    public void test_ServletOutputStream_MultiByteCharEncoding_WriteListener() throws Exception {
        verifyResponseStringLength("/TestServlet31/MultiByteEncodingServlet?isAsync=true", "Привет мир");
    }

    /**
     * Verify that the number of response characters matches the number of characters in a target string
     */
    private void verifyResponseStringLength(String path, String target) throws Exception {
        LOG.info("Expected text: " + target + " length: " + target.length());

        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod("http://" + LS.getHostname() + ":" + LS.getHttpDefaultPort() + path);
        int responseCode = client.executeMethod(get);
        String responseBody = get.getResponseBodyAsString().trim();
        Assert.assertEquals("Expected " + 200 + " status code was not returned!", 200, responseCode);
        LOG.info("Response text: " + responseBody + " length: " + responseBody.length());

        Assert.assertTrue("The response length was incorrect: " + responseBody.length() + " != " + target.length(),
                          responseBody.length() == target.length());
    }

    private void verifyStringsInResponse(HttpClient client, String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        getResponse(client, contextRoot, path, expectedResponseStrings);
    }

    private Header[] verifyStringsInResponse_getResponseHeaders(HttpClient client, String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        return getResponse(client, contextRoot, path, expectedResponseStrings).getResponseHeaders();
    }

    private String verifyStringsInResponse_getResponseBody(HttpClient client, String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        return getResponse(client, contextRoot, path, expectedResponseStrings).getResponseBodyAsString();
    }

    private GetMethod getResponse(HttpClient client, String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        GetMethod get = new GetMethod("http://" + LS.getHostname() + ":" + LS.getHttpDefaultPort() + contextRoot + path);
        int responseCode = client.executeMethod(get);
        String responseBody = get.getResponseBodyAsString();
        LOG.info("Response : " + responseBody);

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, responseCode);

        for (String expectedResponse : expectedResponseStrings) {
            assertTrue("The response did not contain: " + expectedResponse, responseBody.contains(expectedResponse));
        }

        return get;
    }
}
