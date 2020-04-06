/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.protocol.UploadFileSpec;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests to execute on the wcServer that use HttpUnit.
 */
@MinimumJavaLevel(javaLevel = 7)
public class JSPServerHttpUnit extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(JSPServerHttpUnit.class.getName());
    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @ClassRule
    public static SharedServer SHARED_JSP_SERVER = new SharedServer("servlet31_jspServer");

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
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

}
