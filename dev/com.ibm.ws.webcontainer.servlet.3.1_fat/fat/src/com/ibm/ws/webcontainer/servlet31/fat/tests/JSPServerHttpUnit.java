/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the wcServer that use HttpUnit.
 */
@RunWith(FATRunner.class)
public class JSPServerHttpUnit {
    private static final Logger LOG = Logger.getLogger(JSPServerHttpUnit.class.getName());
    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    private static final String SESSION_ID_LISTENER_JAR_NAME = "SessionIdListener";
    private static final String TEST_SERVLET_31_JAR_NAME = "TestServlet31";
    private static final String TEST_SERVLET_31_APP_NAME = "TestServlet31";

    @Server("servlet31_jspServer")
    public static LibertyServer jspServer;

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

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(jspServer, TestServlet31App);

        // Start the server and use the class name so we can find logs easily.
        jspServer.startServer(JSPServerHttpUnit.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (jspServer != null && jspServer.isStarted()) {
            jspServer.stopServer();
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
        WebRequest request = new PostMethodWebRequest("http://" + jspServer.getHostname() + ":" + jspServer.getHttpDefaultPort() + "/" + contextRoot
                                                      + "/index_getSubmittedFileName.jsp");

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
}
