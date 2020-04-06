/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
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
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;

/**
 * Tests to execute on the wcServer that use HttpUnit.
 */
@RunWith(FATRunner.class)
public class VHServerHttpUnit extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(VHServerHttpUnit.class.getName());
    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @ClassRule
    public static SharedServer ALTVHOST_SERVER = new SharedServer("servlet31_vhServer");

    private static final String SESSION_ID_LISTENER_JAR_NAME = "SessionIdListener";
    private static final String TEST_SERVLET_31_JAR_NAME = "TestServlet31";
    private static final String TEST_SERVLET_31_APP_NAME = "TestServlet31";
    private static final String TEST_SERVLET_31_ALT_HOST_V1_APP_NAME = "TestServlet31AltVHost1";
    private static final String TEST_SERVLET_31_ALT_HOST_V2_APP_NAME = "TestServlet31AltVHost2";

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build thejars to add to the war app as a lib
        JavaArchive SessionIdListenerJar = ShrinkHelper.buildJavaArchive(SESSION_ID_LISTENER_JAR_NAME + ".jar",
                                                                         "com.ibm.ws.webcontainer.servlet_31_fat.sessionidlistener.jar.listeners",
                                                                         "com.ibm.ws.webcontainer.servlet_31_fat.sessionidlistener.jar.servlets");
        JavaArchive TestServlet31Jar = ShrinkHelper.buildJavaArchive(TEST_SERVLET_31_JAR_NAME + ".jar",
                                                                     "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.jar.servlets");
        // Build the war app and add the dependencies
        WebArchive TestServlet31App = ShrinkHelper.buildDefaultApp(TEST_SERVLET_31_APP_NAME + ".war",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.listeners");
        TestServlet31App = (WebArchive) ShrinkHelper.addDirectory(TestServlet31App, "test-applications/TestServlet31.war/resources");
        TestServlet31App = TestServlet31App.addAsLibraries(SessionIdListenerJar, TestServlet31Jar);
        WebArchive TestServlet31AltvHost1App = ShrinkHelper.buildDefaultApp(TEST_SERVLET_31_ALT_HOST_V1_APP_NAME + ".war",
                                                                            "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31altvhost1.war.servlets");
        TestServlet31AltvHost1App = (WebArchive) ShrinkHelper.addDirectory(TestServlet31AltvHost1App, "test-applications/TestServlet31AltVHost1.war/resources");
        WebArchive TestServlet31AltvHost2App = ShrinkHelper.buildDefaultApp(TEST_SERVLET_31_ALT_HOST_V2_APP_NAME + ".war",
                                                                            "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31altvhost2.war.servlets");
        TestServlet31AltvHost2App = (WebArchive) ShrinkHelper.addDirectory(TestServlet31AltvHost2App, "test-applications/TestServlet31AltVHost2.war/resources");
        // Verify if the apps are in the server before trying to deploy them
        if (ALTVHOST_SERVER.getLibertyServer().isStarted()) {
            Set<String> appInstalled = ALTVHOST_SERVER.getLibertyServer().getInstalledAppNames(TEST_SERVLET_31_APP_NAME);
            LOG.info("addAppToServer : " + TEST_SERVLET_31_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
              ShrinkHelper.exportDropinAppToServer(ALTVHOST_SERVER.getLibertyServer(), TestServlet31App);

            appInstalled = ALTVHOST_SERVER.getLibertyServer().getInstalledAppNames(TEST_SERVLET_31_ALT_HOST_V1_APP_NAME);
            LOG.info("addAppToServer : " + TEST_SERVLET_31_ALT_HOST_V1_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(ALTVHOST_SERVER.getLibertyServer(), TestServlet31AltvHost1App);

            appInstalled = ALTVHOST_SERVER.getLibertyServer().getInstalledAppNames(TEST_SERVLET_31_ALT_HOST_V2_APP_NAME);
            LOG.info("addAppToServer : " + TEST_SERVLET_31_ALT_HOST_V2_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(ALTVHOST_SERVER.getLibertyServer(), TestServlet31AltvHost2App);
        }
        ALTVHOST_SERVER.startIfNotStarted();
        ALTVHOST_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + TEST_SERVLET_31_APP_NAME);
        ALTVHOST_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + TEST_SERVLET_31_ALT_HOST_V1_APP_NAME);
        ALTVHOST_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + TEST_SERVLET_31_ALT_HOST_V2_APP_NAME);
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (ALTVHOST_SERVER.getLibertyServer() != null && ALTVHOST_SERVER.getLibertyServer().isStarted()) {
            ALTVHOST_SERVER.getLibertyServer().stopServer(null);
        }
    }

    @Test
    public void testDefaultGetVirtulServerName() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | DefaultGetVirtulServerNameTest : virtual host is default");
        LOG.info("\n /************************************************************************************/");

        String serverVName = "default_host";
        LOG.info("Expecting virtualServerName : " + serverVName);
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestServlet31";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(ALTVHOST_SERVER.getServerUrl(true, contextRoot + "/GetVirtualServerNameServlet?serverName=" + serverVName));
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().startsWith("SUCCESS"));

    }

    @Test
    public void testConfiguredGetVirtualServerName1() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | GetConfiguredServerNameTest : virtual host is alternateHost1");
        LOG.info("\n /************************************************************************************/");

        String serverVName = "alternateHost1";

        LOG.info("Expecting virtualServerName : " + serverVName);
        WebConversation wc = new WebConversation();

        String hostName = ALTVHOST_SERVER.getLibertyServer().getHostname();
        String hostPort = "18080";
        String contextRoot = "/TestServlet31AltVHost1";

        wc.setExceptionsThrownOnErrorStatus(false);

        String url = "http://" + hostName + ":" + hostPort + contextRoot + "/GetAltVirtualServerNameServlet1?serverName=" + serverVName;

        LOG.info("Request Url : " + url);
        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().startsWith("SUCCESS"));

    }

    @Test
    public void testConfiguredGetVirtualServerName2() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | GetConfiguredServerNameTest : virtual host is alternateHost2");
        LOG.info("\n /************************************************************************************/");

        String serverVName = "alternateHost2";

        LOG.info("Expecting virtualServerName : " + serverVName);
        WebConversation wc = new WebConversation();

        String hostName = ALTVHOST_SERVER.getLibertyServer().getHostname();
        String hostPort = "18082";
        String contextRoot = "/TestServlet31AltVHost2";

        wc.setExceptionsThrownOnErrorStatus(false);

        String url = "http://" + hostName + ":" + hostPort + contextRoot + "/GetAltVirtualServerNameServlet2?serverName=" + serverVName;

        LOG.info("Request Url : " + url);
        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().startsWith("SUCCESS"));

    }

    @Test
    public void testConfiguredGetVirtualServerName3() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | GetConfiguredServerNameTest : virtual host is alternateHost2");
        LOG.info("\n /************************************************************************************/");

        String serverVName = "alternateHost2";

        LOG.info("Expecting virtualServerName : " + serverVName);
        WebConversation wc = new WebConversation();

        String hostName = ALTVHOST_SERVER.getLibertyServer().getHostname();
        String hostPort = "18443";
        String contextRoot = "/TestServlet31AltVHost2";

        wc.setExceptionsThrownOnErrorStatus(false);

        String url = "http://" + hostName + ":" + hostPort + contextRoot + "/GetAltVirtualServerNameServlet2?serverName=" + serverVName;

        LOG.info("Request Url : " + url);
        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().startsWith("SUCCESS"));

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return ALTVHOST_SERVER;
    }

}
