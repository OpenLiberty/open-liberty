/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
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
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * All Servlet 3.1 tests with all applicable server features enabled.
 */
@RunWith(FATRunner.class)
public class WCServerTest extends LoggingTest {

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    private static final String SESSION_ID_LISTENER_JAR_NAME = "SessionIdListener";
    private static final String TEST_SERVLET_31_JAR_NAME = "TestServlet31";
    private static final String TEST_METADATA_COMPLETE_JAR_NAME = "TestMetadataComplete";
    private static final String TEST_METADATA_COMPLETE_EXCLUDED_FRAGMENT_JAR_NAME = "TestMetadataCompleteExcludedFragment";
    private static final String TEST_PROGRAMATIC_LISTENER_ADDITION_JAR_NAME = "TestProgrammaticListenerAddition";
    private static final String SINGLETON_STORE_JAR_NAME = "SingletonStore";
    private static final String TEST_SERVLET_31_APP_NAME = "TestServlet31";
    private static final String TEST_METADATA_COMPLETE_APP_NAME = "TestMetadataComplete";
    private static final String SESSION_ID_ADD_LISTENER_APP_NAME = "SessionIdListenerAddListener";
    private static final String SESSION_ID_LISTENER_APP_NAME = "SessionIdListener";
    private static final String SERVLET_CONTEXT_ADD_LISTENER_APP_NAME = "ServletContextAddListener";
    private static final String SERVLET_CONTEXT_CREATE_LISTENER_APP_NAME = "ServletContextCreateListener";
    private static final String TEST_PROGRAMATIC_LISTENER_ADDITION_APP_NAME = "TestProgrammaticListenerAddition";
    private static final String TEST_SERVLET_MAPPING_APP_NAME = "TestServletMapping";
    private static final String TEST_SERVLET_MAPPING_ANNO_APP_NAME = "TestServletMappingAnno";


    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_wcServer");

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
        JavaArchive TestProgrammaticListenerJar = ShrinkHelper.buildJavaArchive(TEST_PROGRAMATIC_LISTENER_ADDITION_JAR_NAME + ".jar",
                                                                                "com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners");
        TestProgrammaticListenerJar = (JavaArchive) ShrinkHelper.addDirectory(TestProgrammaticListenerJar, "test-applications/TestProgrammaticListenerAddition.jar/resources");
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

        WebArchive TestProgrammaticListenerApp = ShrinkHelper.buildDefaultApp(TEST_PROGRAMATIC_LISTENER_ADDITION_APP_NAME + ".war");
        TestProgrammaticListenerApp = TestProgrammaticListenerApp.addAsLibraries(TestServlet31Jar, TestProgrammaticListenerJar);
        WebArchive TestServletMappingApp = ShrinkHelper.buildDefaultApp(TEST_SERVLET_MAPPING_APP_NAME + ".war",
                                                                        "com.ibm.ws.webcontainer.servlet_31_fat.testservletmapping.war.servlets");
        TestServletMappingApp = (WebArchive) ShrinkHelper.addDirectory(TestServletMappingApp, "test-applications/TestServletMapping.war/resources");
        WebArchive TestServletMappingAnnoApp = ShrinkHelper.buildDefaultApp(TEST_SERVLET_MAPPING_ANNO_APP_NAME + ".war",
                                                                            "com.ibm.ws.webcontainer.servlet_31_fat.testservletmappinganno.war.servlets");
        TestServletMappingAnnoApp = (WebArchive) ShrinkHelper.addDirectory(TestServletMappingAnnoApp, "test-applications/TestServletMappingAnno.war/resources");
        
        // Verify if the apps are in the server before trying to deploy them
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(TEST_SERVLET_31_APP_NAME);
            LOG.info("addAppToServer : " + TEST_SERVLET_31_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
              ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), TestServlet31App);
            
            appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(SESSION_ID_LISTENER_APP_NAME);
            LOG.info("addAppToServer : " + SESSION_ID_LISTENER_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), SessionIdListenerApp);

            appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(SESSION_ID_ADD_LISTENER_APP_NAME);
            LOG.info("addAppToServer : " + SESSION_ID_ADD_LISTENER_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), SessionIdAddListenerApp);

            appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(SERVLET_CONTEXT_ADD_LISTENER_APP_NAME);
            LOG.info("addAppToServer : " + SERVLET_CONTEXT_ADD_LISTENER_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), ServletContextAddListenerApp);
            
            appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(TEST_PROGRAMATIC_LISTENER_ADDITION_APP_NAME);
            LOG.info("addAppToServer : " + TEST_PROGRAMATIC_LISTENER_ADDITION_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), TestProgrammaticListenerApp);
            
            appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(SERVLET_CONTEXT_CREATE_LISTENER_APP_NAME);
            LOG.info("addAppToServer : " + SERVLET_CONTEXT_CREATE_LISTENER_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), ServletContextCreateListenerApp);

            appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(TEST_METADATA_COMPLETE_APP_NAME);
            LOG.info("addAppToServer : " + TEST_METADATA_COMPLETE_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), TestMetadataCompleteApp);

            appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(TEST_SERVLET_MAPPING_APP_NAME);
            LOG.info("addAppToServer : " + TEST_SERVLET_MAPPING_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportAppToServer(SHARED_SERVER.getLibertyServer(), TestServletMappingApp);

            appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(TEST_SERVLET_MAPPING_ANNO_APP_NAME);
            LOG.info("addAppToServer : " + TEST_SERVLET_MAPPING_ANNO_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportAppToServer(SHARED_SERVER.getLibertyServer(), TestServletMappingAnnoApp);
          }
        
        
        SHARED_SERVER.startIfNotStarted();

        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + TEST_SERVLET_31_APP_NAME);
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + SESSION_ID_ADD_LISTENER_APP_NAME);
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + SERVLET_CONTEXT_ADD_LISTENER_APP_NAME);
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + TEST_PROGRAMATIC_LISTENER_ADDITION_APP_NAME);
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + SERVLET_CONTEXT_CREATE_LISTENER_APP_NAME);
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + SESSION_ID_LISTENER_APP_NAME);
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + TEST_METADATA_COMPLETE_APP_NAME);

    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer("SRVE9016E:.*", "CWWKZ0002E:.*", "SRVE8015E:.*", "SRVE9002E:.*", "SRVE9014E:.*");
        }
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
     * Sample test for running with Servlet 3.1
     * This test is skipped for servlet-4.0 and servlet-5.0 because
     * there is already a test for this in the 4.0 fat bucket.
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    @SkipForRepeat({EE8_FEATURES, EE9_FEATURES})
    public void testServlet31() throws Exception {
        WebResponse response = this.verifyResponse("/TestServlet31/MyServlet", "Hello World");

        // verify the X-Powered-By Response header
        response.verifyResponseHeaderEquals("X-Powered-By", false, "Servlet/3.1",
                                            true, false);
    }
    

    @Test
    public void testProgrammaticallyAddedServlet() throws Exception {
        // 130998: This tests that the servlet that was programmatically
        // added with a different servlet name in "MyServletContextListener"
        // was created and is accessible.
        this.verifyResponse("/TestServlet31/ProgrammaticServlet", "Hello World");
    }

    @Test
    public void testMetadataCompleteHandlesTypesServlet() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        this.verifyResponse(wb, "/TestMetadataComplete/DisplayInits",
                            new String[] { "ParentServletInitializer: com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacomplete.war.servlets.DisplayInits",
                                           "HashSetChildInitializer: com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacomplete.war.stack.HelperMethodChild",
                                           "HashSetChildInitializer: com.ibm.ws.webcontainer.servlet_31_fat.testmetadatacomplete.war.stack.HelperMethod" });
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
     *                       if validation fails, or if an unexpected error occurs
     */
    @Test
    public void testSessionIdListenerChangeServlet() throws Exception {
        // Make sure the test framework knows that SRVE9014E is expected

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

        testSessionIdListener("/SessionIdListenerAddListener/SessionIdListenerChangeServlet");
    }

    /*
     * Common test code for HttpSessionIdListener tests.
     */
    private void testSessionIdListener(String url) throws Exception {
        this.verifyResponse(url, "Expected IllegalStateException");
        WebBrowser wb = createWebBrowserForTestCase();
        WebResponse wr = this.verifyResponse(wb, url + "?getSessionFirst=true",
                                             new String[] { "Session id returned from changeSessionId",
                                                            "Change count = 1" });
        String oldSessionId = parseResponse(wr, "Original session id = <sessionid>", "</sessionid>");
        String newSessionId = parseResponse(wr, "Session id returned from changeSessionId = <sessionid>", "</sessionid>");
        Assert.assertTrue("ids are equal: old=" + oldSessionId + ":new=" + newSessionId, !oldSessionId.equals(newSessionId));
        this.verifyResponse(wb, url, "Original session id = <sessionid>" + newSessionId + "</sessionid>");
    }

    @Test
    public void testRequestedSessionId() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        this.verifyResponse(wb, "/TestServlet31/SessionIdTest;jsessionid=mysessionid",
                            new String[] { "Requested session id was mysessionid",
                                           "Requested Session id is invalid" });
    }

    @Test
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
     *                       if validation fails, or if an unexpected error occurs
     */
    @Test
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
     * Verifies that the ServletContext.getMinorVersion() returns 1 and 
     * ServletContext.getMajorVersion() returns 3 for Servlet 3.1.
     * This test is skipped for servlet-4.0 and servlet-5.0 because
     * there is already a test for this in the 4.0 fat bucket.
     * @throws Exception
     */
    @Test
    @SkipForRepeat({EE8_FEATURES, EE9_FEATURES})
    public void testServletContextMinorMajorVersion() throws Exception {
        this.verifyResponse("/TestServlet31/MyServlet?TestMajorMinorVersion=true", 
                            "majorVersion: 3");
        this.verifyResponse("/TestServlet31/MyServlet?TestMajorMinorVersion=true",
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
    public void testProgrammaticListenerAddition() throws Exception {

        // Drive a request to the SimpleTestServlet to initialize the application
        this.verifyResponse("/TestProgrammaticListenerAddition/SimpleTestServlet", "Hello World");

        // Ensure that the proper exception was output
        LibertyServer server = SHARED_SERVER.getLibertyServer();

        server.resetLogMarks();

        // PI41941: Changed the message. Wait for the full message.
        String logMessage = server.waitForStringInLog("SRVE9002E:.*\\(Operation: getVirtualServerName \\| Listener: com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged.", logMessage);

    }

    /**
     * This test case uses the ServletContext.createListener API to try and create a listener that
     * does not implement one of the expected listener interfaces. The test will ensure that the proper
     * exception is thrown in this scenario.
     */
    @Test
    public void testServletContextCreateListenerBadListener() throws Exception {

        // Make sure the test framework knows that SRVE9014E is expected

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

                 
    /**
     * This test case verifies a plus in a URL is decoded to a space when default
     * decodeUrlPlusSign = "true" is in effect.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDecodeUrlPlusSignDefault() throws Exception {
        this.verifyResponse("/TestServlet31/noplus+sign.html", "This file has a space in the name");
    }


    /**
     * This test case verifies that WC property decodeUrlPlusSign="false" leaves "+" undecoded.
     * For servlet-3.1 and servlet-4.0, the default for decodeUrlPlusSign has been true,
     * which decodes "+" to blank.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDecodeUrlPlusSign() throws Exception {
        LibertyServer wlp = SHARED_SERVER.getLibertyServer();
        wlp.setMarkToEndOfLog();
        wlp.saveServerConfiguration();
        wlp.setServerConfigurationFile("decodeUrlPlusSign_false.server.xml");
        String logmsg = wlp.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated");
        LOG.info("testDecodeUrlPlusSign: server.xml updated for false case");
        this.verifyResponse("/TestServlet31/plus+sign.html", "This file has a plus sign in the name");

        wlp.restoreServerConfiguration();
        LOG.info("testDecodeUrlPlusSign: server.xml restored");
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
