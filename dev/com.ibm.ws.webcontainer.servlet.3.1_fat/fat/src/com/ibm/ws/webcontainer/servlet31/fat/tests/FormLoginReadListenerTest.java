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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.security.PostParamsClient;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@MinimumJavaLevel(javaLevel = 7)
@Mode(TestMode.FULL)
public class FormLoginReadListenerTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(FormLoginReadListenerTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_formLoginServer");

    private static final String READ_LISTENER_SERVLET_URL = "/FormLogin_ReadListener/TestAsyncReadServlet";
    protected static final String CONTEXT_ROOT = "/FormLogin_ReadListener/";
    protected static final String SERVLET_NAME = "TestAsyncReadServlet";
    protected static PostParamsClient myClient;

    protected static HttpEntity entity;

    @Before
    public void before() {
        try {
            SHARED_SERVER.startIfNotStarted();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
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

        String URLString = SHARED_SERVER.getServerUrl(true, READ_LISTENER_SERVLET_URL);

        HttpPost postMethod = new HttpPost(URLString);

        setPostParams(postMethod, numValues);
        int postDataLength = (int) postMethod.getEntity().getContentLength();

        myClient = new PostParamsClient(SHARED_SERVER.getLibertyServer(), SERVLET_NAME, CONTEXT_ROOT);

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