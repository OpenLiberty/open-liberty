/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * The web.xml has default encoding setting:
 * <request-encoding>UTF-8</request-encoding>
 * <response-encoding>Shift-JIS</response-encoding>
 */
@RunWith(FATRunner.class)
public class WCEncodingTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCEncodingTest.class.getName());
    final static String appNameEncoding = "TestEncoding";
    final static String appNameServlet40 = "TestServlet40";

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_wcServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void before() throws Exception {

        LOG.info("Setup : add TestEncoding to the server if not already present.");

        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(), appNameEncoding + ".war", true,
                                                  "testencoding.war.servlets");

        LOG.info("Setup : add TestServlet40 to the server if not already present.");

        WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestServlet40.ear", true,
                                                  "TestServlet40.war", true, "TestServlet40.jar", true, "testservlet40.war.servlets",
                                                  "testservlet40.war.listeners", "testservlet40.jar.servlets");

        ArrayList<String> expectedErrors = new ArrayList<String>();
        expectedErrors.add("CWWWC0401E:.*");
        SHARED_SERVER.getLibertyServer().addIgnoredErrors(expectedErrors);

        SHARED_SERVER.startIfNotStarted();

        LOG.info("Setup : wait for message to indicate apps have started");

        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* TestEncoding", 10000);
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* TestServlet40", 10000);

        LOG.info("Setup : wait for message to indicate app has started");

    }

    @AfterClass
    public static void testCleanup() throws Exception {

        SHARED_SERVER.getLibertyServer().stopServer(null);
    }

    /*
     * Request sends with Content-Type header charset=Shift-JIS. Ignore default
     * encoding (UTF-8) Expecting: request's encoding Shift-JIS
     */
    @Test
    public void testRequestEncodingPerCharset() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Content-Type", "text/html; charset=Shift-JIS");

        WebResponse response = getResponse("/" + appNameEncoding + "/ServletEncoding?type=request&expected=Shift-JIS",
                                           headers);
        String text = response.getText();

        LOG.info("Response text: " + text);
        // LOG.info("Response Content-Type [" + response.getContentType() +
        // "]");
        // LOG.info("Response getCharacterSet [" + response.getCharacterSet() +
        // "]");

        assertFalse("FAIL unexpected encoding in request", text.contains("FAIL"));
    }

    /*
     * request send a bad charset in Content-Type. It should be ignored by the
     * servlet and use the default encoding Expecting: default encoding UTF-8
     */
    @Test
    @Mode(TestMode.FULL)
    public void testRequestEncodingBadCharSet() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Content-Type", "text/html; charset=BAD-ENCODING");

        WebResponse response = getResponse(
                                           "/" + appNameEncoding + "/ServletEncoding?type=request&expected=BAD-ENCODING", headers);
        String text = response.getText();

        LOG.info("Response text: " + text);
        assertFalse("FAIL unexpected encoding from web module", text.contains("FAIL"));
    }

    /*
     * No charset, no explicit request.setCharacterEncoding Expecting: default
     * web.xml encoding UTF-8
     */
    @Test
    public void testRequestEncodingPerModule() throws Exception {
        // HashMap<String, String> headers = new HashMap<String, String>();
        WebResponse response = getResponse("/" + appNameEncoding + "/ServletEncoding?type=request&expected=UTF-8",
                                           null);
        String text = response.getText();

        LOG.info("Response text: " + text);
        assertFalse("FAIL unexpected encoding from web module", text.contains("FAIL"));
    }

    /*
     * no charset. explicit request.setCharacterEncoding("EUC-KR") Expected:
     * EUC-KR
     */
    @Test
    public void testRequestEncodingExplicit() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Explicit-ReqEnc", "EUC-KR");
        WebResponse response = getResponse("/" + appNameEncoding + "/ServletEncoding?type=request&expected=EUC-KR",
                                           headers);
        String text = response.getText();

        LOG.info("Response text: " + text);
        assertFalse("FAIL unexpected request encoding ", text.contains("FAIL"));
    }

    /*
     * Expected: default encoding Shift-JIS
     */
    @Test
    public void testResponseEncodingPerModule() throws Exception {

        WebResponse response = getResponse("/" + appNameEncoding + "/ServletEncoding?type=response&expected=Shift-JIS",
                                           null);
        String text = response.getText();

        LOG.info("Response text: " + text);
        assertFalse("FAIL unexpected response encoding in module", text.contains("FAIL"));
    }

    /*
     * default encoding is Shift-JIS servlet explicitly
     * response.setCharacterEncoding("EUC-KR"); Expected: response charset is
     * EUC-KR. Also verify the old encoding (before setCharacterEncoding) and
     * the new one are different.
     *
     */
    @Test
    public void testResponseEncodingExplicit() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Explicit-RespEnc", "EUC-KR");

        WebResponse response = getResponse("/" + appNameEncoding + "/ServletEncoding?type=response&expected=EUC-KR",
                                           headers);
        String text = response.getText();

        LOG.info("Response text: " + text);
        assertFalse("FAIL unexpected encoding in response", text.contains("FAIL"));
    }

    /*
     * Need SCI application. SCI set context.setRequestCharacterEncoding and
     * setResponseCharacterEncoding to "KSC5601" Servlet retrieve encoding from
     * context.getRequestCharacterEncoding and getResponseCharacterEncoding.
     * Also attempt to context.setRequestCharacterEncoding() again to cause
     * IllegalStateException.
     */
    @Test
    public void testContextEncoding() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Context-Encoding", "true");
        WebResponse response = getResponse("/TestServlet40/ServletEncoding?type=context", headers);
        String text = response.getText();

        LOG.info("Response text: " + text);
        assertFalse("FAIL unexpected request encoding ", text.contains("FAIL"));
    }

    private WebResponse getResponse(String uri, HashMap<String, String> headers) throws Exception {

        WebConversation wc = new WebConversation();

        if (headers != null && !headers.isEmpty()) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                wc.setHeaderField(key, headers.get(key));
            }
        }

        // WebRequest request = new
        // GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot +
        // uri));
        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, uri));
        WebResponse response = wc.getResponse(request);
        // String text = response.getText();
        // int code = response.getResponseCode();

        return response;

    }

}
