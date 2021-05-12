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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.PostParamsClient;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class FormLoginReadListenerTest {
    private static final Logger LOG = Logger.getLogger(FormLoginReadListenerTest.class.getName());

    @Server("servlet31_formLoginReadListener")
    public static LibertyServer server;

    private static final String FORM_LOGIN_READ_LISTENER_APP_NAME = "FormLogin_ReadListener";

    private static final String READ_LISTENER_SERVLET_URL = "/FormLogin_ReadListener/TestAsyncReadServlet";
    protected static final String CONTEXT_ROOT = "/FormLogin_ReadListener/";
    protected static final String SERVLET_NAME = "TestAsyncReadServlet";
    protected static PostParamsClient myClient;

    protected static HttpEntity entity;

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the war app and add the dependencies
        WebArchive formLoginReadListenerApp = ShrinkHelper.buildDefaultApp(FORM_LOGIN_READ_LISTENER_APP_NAME + ".war",
                                                                           "com.ibm.ws.webcontainer.servlet_31_fat.formlogin_readlistener.war.web");
        formLoginReadListenerApp = (WebArchive) ShrinkHelper.addDirectory(formLoginReadListenerApp, "test-applications/FormLogin_ReadListener.war/resources");

        // Export the application.
        ShrinkHelper.exportAppToServer(server, formLoginReadListenerApp);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(FormLoginReadListenerTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE8015E:.*");
        }
    }

    /*
     * Tests where ReadListener methods throw an exception to make sure onError is called correctly.
     */
    @Test
    public void test_Login_AsyncRL_Exceptions() throws Exception {
        HashMap<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("TestToCall", "onDataAvailableException");
        String response = test_Login_AsyncRL(10, headerMap, false);
        assertTrue("Response does not contain : onError called", response.contains("onError called : TestAsyncReadServlet : onDataAvailableException : IOException thrown"));

        headerMap.put("TestToCall", "onAllDataReadException");
        response = test_Login_AsyncRL(10, headerMap, false);
        assertTrue("Response does not contain : onError called", response.contains("onError called : TestAsyncReadServlet : onAllDataReadException : IOException thrown"));

    }

    /*
     * test sending post data of varying sizes to the servlet to test basic functionality of readListener .The servlet
     * echoes back post data to the client. The size of data received is then compared with the size of the post data sent.
     */

    @Test
    public void test_Login_AsyncRL_variousDataLengths() throws Exception {
        test_Login_AsyncRL(2, null, true);
        test_Login_AsyncRL(20, null, true);
        test_Login_AsyncRL(200, null, true);
        test_Login_AsyncRL(2000, null, true);
    }

    private String test_Login_AsyncRL(int numValues, Map<String, String> headers, boolean checkContentLength) throws Exception {

        Log.info(getClass(), "test_Login_AsyncRL", "Start test with " + numValues + " parameters");

        String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;

        HttpPost postMethod = new HttpPost(URLString);

        setPostParams(postMethod, numValues);
        int postDataLength = (int) postMethod.getEntity().getContentLength();

        myClient = new PostParamsClient(server, SERVLET_NAME, CONTEXT_ROOT);

        String responseContent = myClient.accessAndAuthenticate(postMethod, "user1", "user1Login", PostParamsClient.STORE_SESSION, 200, headers).trim();

        Log.info(getClass(), "test_Login_AsyncRL", "post data length =  " + postDataLength + ", response length = " + responseContent.length());
        if (postDataLength != responseContent.length())
            Log.info(getClass(), "test_Login_AsyncRL", "responseContent :" + responseContent + ":");

        myClient.formLogout();
        myClient.resetClientState();

        if (checkContentLength)
            assertEquals("Expected " + postDataLength + "bytes of data but got " + responseContent.length(), postDataLength, responseContent.length());

        return responseContent;
    }

    private void setPostParams(HttpPost postMethod, int numValues) {

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

        nvps.add(new BasicNameValuePair("servlet", SERVLET_NAME));

        for (int i = 1; i <= numValues; i++) {
            nvps.add(new BasicNameValuePair("parameter" + i, "value" + i));
        }

        try {
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail("unexpecgted exception " + e);
        }

    }
}