/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * All Servlet 3.1 tests with all applicable server features enabled.
 */
@MinimumJavaLevel(javaLevel = 7)
public class WCServerTest extends LoggingTest {

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_wcServer");

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
    }

    protected WebResponse verifyResponse(String resource, String expectedResponse) throws Exception {
        return SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse);
    }

    protected WebResponse verifyResponse(String resource, String expectedResponse, int numberToMatch, String extraMatch) throws Exception {
        return SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse, numberToMatch, extraMatch);
    }

    protected WebResponse verifyResponse(WebBrowser wb, String resource, String expectedResponse) throws Exception {
        return SHARED_SERVER.verifyResponse(wb, resource, expectedResponse);
    }

    protected WebResponse verifyResponse(WebBrowser wb, String resource, String... expectedResponseStrings) throws Exception {
        return SHARED_SERVER.verifyResponse(wb, resource, expectedResponseStrings);
    }

    protected WebResponse verifyResponse(WebBrowser wb, String resource, String[] expectedResponses, String[] unexpectedResponses) throws Exception {
        return SHARED_SERVER.verifyResponse(wb, resource, expectedResponses, unexpectedResponses);
    }

    protected String parseResponse(WebResponse wr, String beginText, String endText) {
        String s;
        String body = wr.getResponseBody();
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
     * Sample test
     *
     * @throws Exception
     *             if something goes horribly wrong
     */
    @Test
    //@Mode(TestMode.FULL)
    public void testServlet() throws Exception {
        WebResponse response = this.verifyResponse("/TestServlet31/MyServlet", "Hello World");

        // verify the X-Powered-By Response header
        response.verifyResponseHeaderEquals("X-Powered-By", false, "Servlet/3.1",
                                            true, false);
    }

    @Test
    //@Mode(TestMode.FULL)
    public void testProgrammaticallyAddedServlet() throws Exception {
        // 130998: This tests that the servlet that was programmatically
        // added with a different servlet name in "MyServletContextListener"
        // was created and is accessible.
        this.verifyResponse("/TestServlet31/ProgrammaticServlet", "Hello World");
    }

    @Test
    public void testMetadataCompleteHandlesTypesServlet() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        this.verifyResponse(wb, "/TestMetadataComplete/DisplayInits", "ParentServletInitializer: servlets.DisplayInits", "HashSetChildInitializer: stack.HelperMethodChild",
                            "HashSetChildInitializer: stack.HelperMethod");
    }

    //This isn't duplicating testMetadataCompleteHandlesTypesServlet since we want granularity on the functions.
    //This is for excluded JARs only, the one above is for general SCI function.
    @Test
    public void testMetadataCompleteExcludedHandlesTypesServlet() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();

        String[] expected = new String[] {};

        String[] unexpected = new String[] { "ExcludedServletInitializer: servlets.ExcludedServlet", "ExcludedServletInitializer: servlets.DisplayInits",
                                             "ParentServletInitializer: servlets.ExcludedServlet" };

        this.verifyResponse(wb, "/TestMetadataComplete/DisplayInits", expected, unexpected);
    }

    /**
     * Verifies that container lifecycle events are behaving correctly (JSR-299 section 11.5).
     *
     * @throws Exception
     *             if validation fails, or if an unexpected error occurs
     */
    @Test
    public void testSessionIdListenerChangeServlet() throws Exception {
        // Make sure the test framework knows that SRVE9014E is expected
        SHARED_SERVER.setExpectedErrors("SRVE9014E:.*");

        testSessionIdListener("/TestServlet31/SessionIdListenerChangeServlet");
    }

    /**
     * Perform the same test as "testSessionIdListenerChangeServlet" except for this time the HttpSessionIdListener is
     * registered in the web.xml and not via the @WebListener annotation. The SessionIdListener application is used
     * for this test.
     *
     * @throws Exception
     */
    @Test
    public void testSessionIdListenerRegisteredWebXml() throws Exception {
        // Make sure the test framework knows that SRVE9014E is expected
        SHARED_SERVER.setExpectedErrors("SRVE9014E:.*");

        testSessionIdListener("/SessionIdListener/SessionIdListenerChangeServlet");
    }

    /**
     * Perform the same test as "testSessionIdListenerChangeServlet" except for this time the HttpSessionIdListener is
     * registered via a ServletContextListener using the ServletContext.addListener API. The SessionIdListenerAddListener
     * application is used for this test.
     *
     * @throws Exception
     */
    @Test
    public void testSessionIdListenerServletContextAddListener() throws Exception {
        // Make sure the test framework knows that SRVE9014E is expected
        SHARED_SERVER.setExpectedErrors("SRVE9014E:.*");

        testSessionIdListener("/SessionIdListenerAddListener/SessionIdListenerChangeServlet");
    }

    /*
     * Common test code for HttpSessionIdListener tests.
     */
    private void testSessionIdListener(String url) throws Exception {
        this.verifyResponse(url, "Expected IllegalStateException");
        WebBrowser wb = createWebBrowserForTestCase();
        WebResponse wr = this.verifyResponse(wb, url + "?getSessionFirst=true", "Session id returned from changeSessionId",
                                             "Change count = 1");
        String oldSessionId = parseResponse(wr, "Original session id = <sessionid>", "</sessionid>");
        String newSessionId = parseResponse(wr, "Session id returned from changeSessionId = <sessionid>", "</sessionid>");
        Assert.assertTrue("ids are equal: old=" + oldSessionId + ":new=" + newSessionId, !oldSessionId.equals(newSessionId));
        this.verifyResponse(wb, url, "Original session id = <sessionid>" + newSessionId + "</sessionid>");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testRequestedSessionId() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        this.verifyResponse(wb, "/TestServlet31/SessionIdTest;jsessionid=mysessionid", "Requested session id was mysessionid",
                            "Requested Session id is invalid");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testGetServerInfo() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();

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

        this.verifyResponse(wb, "/TestServlet31/GetServerInfoTest", "GetServerInfoTest: ServletContext.getServerInfo()=IBM WebSphere Liberty/" + v);
    }

    /**
     * Verifies that a response.reset works
     *
     * @throws Exception
     *             if validation fails, or if an unexpected error occurs
     */
    @Test
    //@Mode(TestMode.FULL)
    public void testResponseReset() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        String url = "/TestServlet31/ResponseReset?firstType=pWriter&secondType=pWriter";
        WebResponse wr = this.verifyResponse(wb, url, "SUCCESS");
        String body = wr.getResponseBody();
        Assert.assertTrue("contained content before the reset: url=" + url + "::" + body, body.indexOf("FAILURE") == -1);
        url = "/TestServlet31/ResponseReset?firstType=pWriter&secondType=outputStream";
        wr = this.verifyResponse(wb, url, "SUCCESS");
        body = wr.getResponseBody();
        Assert.assertTrue("contained content before the reset: url=" + url + "::" + body, body.indexOf("FAILURE") == -1);
        url = "/TestServlet31/ResponseReset?firstType=outputStream&secondType=pWriter";
        wr = this.verifyResponse(wb, url, "SUCCESS");
        body = wr.getResponseBody();
        Assert.assertTrue("contained content before the reset: url=" + url + "::" + body, body.indexOf("FAILURE") == -1);
        url = "/TestServlet31/ResponseReset?firstType=outputStream&secondType=outputStream";
        wr = this.verifyResponse(wb, url, "SUCCESS");
        body = wr.getResponseBody();
        Assert.assertTrue("contained content before the reset: url=" + url + "::" + body, body.indexOf("FAILURE") == -1);
    }

    /**
     * Verifies that the ServletContext.getMinorVersion() returns 1 for Servlet 3.1.
     *
     * @throws Exception
     */
    @Test
    public void testServletContextMinorVersion() throws Exception {
        this.verifyResponse("/TestServlet31/MyServlet?TestMinorVersion=true",
                            "minorVersion: 1");
    }

    /**
     * Verify that a duplicate <servlet-mapping> element results in a deployment error. Servlet 3.1 spec, section 12.2
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.wsspi.adaptable.module.UnableToAdaptException", "com.ibm.ws.container.service.metadata.MetaDataException" })
    public void testServletMapping() throws Exception {

        LibertyServer wlp = SHARED_SERVER.getLibertyServer();
        wlp.setMarkToEndOfLog();
        SHARED_SERVER.setExpectedErrors("SRVE9016E:.*", "CWWKZ0002E:.*");

        wlp.saveServerConfiguration();
        // copy server.xml for TestServletMapping.war
        // should use updateServerConfiguration(wlp.getServerRoot() +
        wlp.setServerConfigurationFile("TestServletMapping/server.xml");
        // check for error message
        String logmsg = wlp.waitForStringInLogUsingMark("CWWKZ0002E:.*TestServletMapping");
        Assert.assertNotNull("TestServletMapping application should have failed to start ", logmsg);

        // application failed to start, verify that it is because of a duplicate servlet-mapping
        logmsg = wlp.waitForStringInLogUsingMark("SRVE9016E:");
        Assert.assertNotNull("TestServletMapping application deployment did not result in  message SRVE9016E: ", logmsg);

        wlp.setMarkToEndOfLog();
        // copy server.xml for TestServletMappingAnno.war
        wlp.setServerConfigurationFile("TestServletMappingAnno/server.xml");
        // check for error message
        logmsg = wlp.waitForStringInLogUsingMark("CWWKZ0002E:.*TestServletMappingAnno");
        Assert.assertNotNull("TestServletMappingAnno application should have failed to start ", logmsg);

        // application failed to start, verify that it is because of a duplicate servlet-mapping
        logmsg = wlp.waitForStringInLogUsingMark("SRVE9016E:");
        Assert.assertNotNull("TestServletMappingAnno application deployment did not result in  message SRVE9016E ", logmsg);

        wlp.restoreServerConfiguration();
    }

    /**
     * Verifies that the correct message is printed out when the class name specified for the ServletContext.addListener(String className)
     * API can not be found.
     *
     * @throws Exception
     */
    @Test
    public void testServletContextAddListener() throws Exception {

        // Make sure the test framework knows that SRVE8015E is expected
        SHARED_SERVER.setExpectedErrors("SRVE8015E:.*");

        this.verifyResponse("/ServletContextAddListener/SimpleTestServlet", "Hello World");

        // application should be initialized with the above request, now check the logs for the proper output.
        Assert.assertNotNull(SHARED_SERVER.getLibertyServer().findStringsInLogs("SRVE8015E:.*ThisListenerDoesNotExist"));
    }

    /**
     * This test case will use a ServletContainerInitializer to add a ServletContextListener in a
     * programmatic way. Then in the ServletContextListener contextInitialized method calls a method
     * on the ServletContext.
     *
     * This method should throw an UnsupportedOperationException according to the Servlet 3.1 ServletContext API.
     *
     * Check to ensure that this message is thrown and the NLS message is resolved correctly.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testProgrammaticListenerAddition() throws Exception {
        SHARED_SERVER.setExpectedErrors("SRVE9002E:.*");

        // Drive a request to the SimpleTestServlet to initialize the application
        this.verifyResponse("/TestProgrammaticListenerAddition/SimpleTestServlet", "Hello World");

        // Ensure that the proper exception was output
        LibertyServer server = SHARED_SERVER.getLibertyServer();

        server.resetLogMarks();

        // PI41941: Changed the message. Wait for the full message.
        String logMessage = server.waitForStringInLog("SRVE9002E:.*\\(Operation: getVirtualServerName \\| Listener: listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged.", logMessage);

    }

    /**
     * This test case uses the ServletContext.createListener API to try and create a listener that
     * does not implement one of the expected listener interfaces. The test will ensure that the proper
     * exception is thrown in this scenario.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testServletContextCreateListenerBadListener() throws Exception {

        // Make sure the test framework knows that SRVE9014E is expected
        SHARED_SERVER.setExpectedErrors("SRVE9014E:.*");

        // Drive a request to the SimpleTestServlet to initialize the application
        this.verifyResponse("/ServletContextCreateListener/SimpleTestServlet", "Hello World");

        // Ensure that the proper exception was output
        LibertyServer server = SHARED_SERVER.getLibertyServer();
        List<String> logMessage = server.findStringsInLogs("SRVE8014E:");

        Assert.assertNotNull("The correct message was not logged.", logMessage);

        // Ensure a REQUEST_INITIALIZED can be found in the logs.  If it can't be that means that a
        // correct listener was not created and added as expected.
        logMessage = server.findStringsInLogs("REQUEST_INITIALIZED");
        Assert.assertNotNull("REQUEST_INITIALIZED was not found in the logs. The listener must not have been created and added correctly",
                             logMessage);

    }
}