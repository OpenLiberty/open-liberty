/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;
import java.io.InputStream;
import java.io.File;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.webcontainer.servlet31.fat.FATSuite;
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
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Test that the allowAbsoluteFileNameForPartWrite works as intended.
 * When true the filename in part.write method should be treated as an absolute location.
 * When false the filename will be handled as a relative path.
 * The default in servlet-6.0 and earlier is false. The default in servlet-6.1 and later is true.
 * @author Thomas Smith
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PH62271Test {

    private static final Logger LOG = Logger.getLogger(PH62271Test.class.getName());

    private static final String PH62271_APP_NAME = "PH62271";

    protected static final Class<?> c = PH62271Test.class;

    @Server("servlet31_PH62271")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Build the war app and add the dependencies
        WebArchive PH62271App = ShrinkHelper.buildDefaultApp(PH62271_APP_NAME + ".war", "com.ibm.ws.webcontainer.servlet_31_fat.PH62271");

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(server, PH62271App);

        server.startServer(c.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Mainly copied over from JSPServerHttpUnit#testFileUpload_test_getSubmittedFileName
     * Tests that the allowAbsoluteFileNameForPartWrite in the server.xml works correctly
     */ 
    @Test
    public void testPH62271 () throws Exception {

        if (JakartaEEAction.isEE11OrLaterActive()){
            ServerConfiguration config = server.getServerConfiguration();
            // Set to null for servlet 6.1 and higher to test default behavior
            config.getWebContainer().setAllowAbsoluteFileNameForPartWrite(null);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null);
        }

        LOG.info("\n /******************************************************************************/");
        LOG.info("\n [WebContainer | Part#write]: Testing Part.write");
        LOG.info("\n /******************************************************************************/");
        WebConversation wc = new WebConversation();
        String contextRoot = "/PH62271";
        wc.setExceptionsThrownOnErrorStatus(false);
        WebRequest request = new PostMethodWebRequest("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + "/index.jsp");

        WebResponse response = wc.getResponse(request);
        LOG.info(response.getText());

        WebForm loginForm = response.getForms()[0];
        request = loginForm.getRequest();

        String fileName = "myTempFile.txt";

        // Get test file
        InputStream in = this.getClass().getResourceAsStream("/com/ibm/ws/fat/resources/" + fileName);
        LOG.info(in == null ? "/com/ibm/ws/fat/resources/" + fileName + " in is null" : "/com/ibm/ws/fat/resources/" + fileName + " in is not null");

        String uploadFileName = "myFileUploadFile.txt";
        UploadFileSpec file = new UploadFileSpec(uploadFileName, in, "ISO-8859-1");
        request.setParameter("files", new UploadFileSpec[] { file });
        request.setParameter("location", server.getServerRoot());

        response = wc.getResponse(request);
        int code = response.getResponseCode();

        LOG.info("/*************************************************/");
        LOG.info("[WebContainer | Part#write]: Return Code is: " + code);
        LOG.info("[WebContainer | Part#write]: Response is: ");
        LOG.info(response.getText());
        LOG.info("/*************************************************/");

        assertTrue("Did not get 200 response code, got " + code + " response code instead.", code==200);

        File uploadedFile = new File(server.getServerRoot() + "/uploads/" + uploadFileName);
        assertTrue("Did not find the uploaded file at its absolute path location. The absolute path is: " + server.getServerRoot() + "/uploads/" + uploadFileName, uploadedFile.exists());
    }
}
