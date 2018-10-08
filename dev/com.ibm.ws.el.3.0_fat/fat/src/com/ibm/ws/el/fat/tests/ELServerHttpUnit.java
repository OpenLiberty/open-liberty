/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.MinimumJavaLevel;

/**
 * Tests to execute on the jspServer that use HttpUnit.
 */
@MinimumJavaLevel(javaLevel = 7)
public class ELServerHttpUnit extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(ELServerHttpUnit.class.getName());
    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("elServer");

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
    }

    /**
     * A sample HttpUnit test case for EL 3.0. Just ensure that the basic application is reachable.
     *
     * @throws Exception
     */
    @Test
    public void sampleTest() throws Exception {
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestEL3.0";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + "/SimpleTestServlet"));
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Hello World"));
    }

    /**
     * A test case to verify that the EL 2.2 operators continue to function correcttly via the EL 3.0 implementation.
     * The tests are defined in the servlet EL22OperatorsServlet. Here we just check to make sure nothing failed.
     */
    @Test
    public void testEL22Operators() throws Exception {
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestEL3.0";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + "/EL22OperatorsServlet"));
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        // Check the servlet output for "Test Failed!"
        assertTrue(response.getText(), !response.getText().contains("Test Failed!"));
    }

    /**
     * A test case to verify that the EL 3.0 operators function correctly via the EL 3.0 implementation.
     * The tests are defined in the servlet EL30OperatorsServlet. Here we just check to make sure nothing failed.
     */
    @Test
    public void testEL30Operators() throws Exception {
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestEL3.0";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + "/EL30OperatorsServlet"));
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        // Check the servlet output for "Test Failed!"
        assertTrue(response.getText(), !response.getText().contains("Test Failed!"));
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
