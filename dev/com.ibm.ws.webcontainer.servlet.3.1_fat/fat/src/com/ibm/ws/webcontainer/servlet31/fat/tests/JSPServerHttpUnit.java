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

import java.io.InputStream;
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
import com.meterware.httpunit.PostMethodWebRequest;
//import com.meterware.httpunit.protocol.UploadFileSpec;
import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests to execute on the wcServer that use HttpUnit.
 */
@RunWith(FATRunner.class)
public class JSPServerHttpUnit extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(JSPServerHttpUnit.class.getName());
    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    private static final String SESSION_ID_LISTENER_JAR_NAME = "SessionIdListener";
    private static final String TEST_SERVLET_31_JAR_NAME = "TestServlet31";
    private static final String TEST_SERVLET_31_APP_NAME = "TestServlet31";

    @ClassRule
    public static SharedServer SHARED_JSP_SERVER = new SharedServer("servlet31_jspServer");

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the jars to add to the war app as a lib
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
        // Verify if the apps are in the server before trying to deploy them
        if (SHARED_JSP_SERVER.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SHARED_JSP_SERVER.getLibertyServer().getInstalledAppNames(TEST_SERVLET_31_APP_NAME);
            LOG.info("addAppToServer : " + TEST_SERVLET_31_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
              ShrinkHelper.exportDropinAppToServer(SHARED_JSP_SERVER.getLibertyServer(), TestServlet31App);
          }
        SHARED_JSP_SERVER.startIfNotStarted();
        SHARED_JSP_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + TEST_SERVLET_31_APP_NAME);
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (SHARED_JSP_SERVER.getLibertyServer() != null && SHARED_JSP_SERVER.getLibertyServer().isStarted()) {
            SHARED_JSP_SERVER.getLibertyServer().stopServer(null);
        }
    }

    @Test
    @Mode(TestMode.LITE)
    public void testFileUpload_test_getSubmittedFileName() throws Exception {

        LOG.info("\n /******************************************************************************/");
        LOG.info("\n [WebContainer | FileUpload]: Testing Part.getSubmittedFileName");
        LOG.info("\n /******************************************************************************/");
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestServlet31";
        wc.setExceptionsThrownOnErrorStatus(false);
        WebRequest request = new PostMethodWebRequest(SHARED_JSP_SERVER.getServerUrl(true, contextRoot + "/index_getSubmittedFileName.jsp"));

        WebResponse response = wc.getResponse(request);
        LOG.info(response.getText());

        WebForm loginForm = response.getForms()[0];
        request = loginForm.getRequest();

        InputStream in = this.getClass().getResourceAsStream("/com/ibm/ws/fat/resources/myTempFile.txt");
        LOG.info(in == null ? "/com/ibm/ws/fat/resources/myTempFile.txt in is null" : "/com/ibm/ws/fat/resources/myTempFile.txt in is not null");

        UploadFileSpec file = new UploadFileSpec("myFileUploadFile.txt", in, "ISO-8859-1");
        request.setParameter("files", new UploadFileSpec[] { file });

        response = wc.getResponse(request);
        int code = response.getResponseCode();

        LOG.info("/*************************************************/");
        LOG.info("[WebContainer | FileUpload]: Return Code is: " + code);
        LOG.info("[WebContainer | FileUpload]: Response is: ");
        LOG.info(response.getText());
        LOG.info("/*************************************************/");

        boolean return_code = false;
        if (code == 200)
            return_code = true;
        assertTrue(failMsg(200), return_code);

        String search_msg = null;

        search_msg = "Part.getSubmittedFileName = myFileUploadFile.txt";
        assertTrue(failMsg(search_msg), response.getText().indexOf(search_msg) != -1);
    }

    private String failMsg(String search_msg) {
        String fail_msg = "\n FileUpload: Fail to find string: " + search_msg + "\n";
        return fail_msg;
    }

    private String failMsg(int search_msg) {
        String fail_msg = "\n FileUpload: Fail to find string: " + search_msg + "\n";
        return fail_msg;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_JSP_SERVER;
    }

}
