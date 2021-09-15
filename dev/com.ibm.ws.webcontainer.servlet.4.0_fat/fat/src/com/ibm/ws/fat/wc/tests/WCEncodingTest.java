/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * The web.xml has default encoding setting:
 * <request-encoding>UTF-8</request-encoding>
 * <response-encoding>Shift-JIS</response-encoding>
 */
@RunWith(FATRunner.class)
public class WCEncodingTest {

    private static final Logger LOG = Logger.getLogger(WCEncodingTest.class.getName());
    private static final String APP_NAME_ENCODING = "TestEncoding";

    @Server("servlet40_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        LOG.info("Setup : add TestEncoding to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME_ENCODING + ".war", "testencoding.servlets");

        LOG.info("Setup : add TestServlet40 to the server if not already present.");

        JavaArchive testServlet40Jar = ShrinkWrap.create(JavaArchive.class, "TestServlet40.jar");
        ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + "TestServlet40.jar" + "/resources");
        testServlet40Jar.addPackage("testservlet40.jar.servlets");

        WebArchive testServlet40War = ShrinkWrap.create(WebArchive.class, "TestServlet40.war");
        testServlet40War.addAsLibrary(testServlet40Jar);
        testServlet40War.addPackage("testservlet40.servlets");
        testServlet40War.addPackage("testservlet40.listeners");

        ShrinkHelper.exportDropinAppToServer(server, testServlet40War);

        ArrayList<String> expectedErrors = new ArrayList<String>();
        expectedErrors.add("CWWWC0401E:.*");
        server.addIgnoredErrors(expectedErrors);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCEncodingTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Request sends with Content-Type header charset=Shift-JIS. Ignore default
     * encoding (UTF-8) Expecting: request's encoding Shift-JIS
     */
    @Test
    public void testRequestEncodingPerCharset() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Content-Type", "text/html; charset=Shift-JIS");

        WebResponse response = getResponse("/" + APP_NAME_ENCODING + "/ServletEncoding?type=request&expected=Shift-JIS",
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
                                           "/" + APP_NAME_ENCODING + "/ServletEncoding?type=request&expected=BAD-ENCODING", headers);
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
        WebResponse response = getResponse("/" + APP_NAME_ENCODING + "/ServletEncoding?type=request&expected=UTF-8",
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
        WebResponse response = getResponse("/" + APP_NAME_ENCODING + "/ServletEncoding?type=request&expected=EUC-KR",
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
        WebResponse response = getResponse("/" + APP_NAME_ENCODING + "/ServletEncoding?type=response&expected=Shift-JIS",
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

        WebResponse response = getResponse("/" + APP_NAME_ENCODING + "/ServletEncoding?type=response&expected=EUC-KR",
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

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + uri;
        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);

        return response;

    }

}
