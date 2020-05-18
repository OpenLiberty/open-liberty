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

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.webcontainer.security.test.servlets.PostParamsClient;

import componenttest.annotation.SkipForRepeat;
import componenttest.topology.impl.LibertyServer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

//Temporarily skipped for EE9 jakarta until jakarta repeat bug is fixed and jakartaee 9 feature is developed
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@SkipForRepeat(EE9_FEATURES)
public class FormLoginReadListenerTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(FormLoginReadListenerTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_wcServer");

    private static final String FORM_LOGIN_READ_LISTENER_APP_NAME = "FormLogin_ReadListener";

    private static final String READ_LISTENER_SERVLET_URL = "/FormLogin_ReadListener/TestAsyncReadServlet";
    protected static final String CONTEXT_ROOT = "/FormLogin_ReadListener/";
    protected static final String SERVLET_NAME = "TestAsyncReadServlet";
    protected static PostParamsClient myClient;

    protected static HttpEntity entity;

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the war app and add the dependencies
        WebArchive FormLoginReadListenerApp = ShrinkHelper.buildDefaultApp(FORM_LOGIN_READ_LISTENER_APP_NAME + ".war",
                                                                           "com.ibm.ws.webcontainer.servlet_31_fat.formlogin_readlistener.war.web");
        FormLoginReadListenerApp = (WebArchive) ShrinkHelper.addDirectory(FormLoginReadListenerApp, "test-applications/FormLogin_ReadListener.war/resources");
        // Verify if the apps are in the server before trying to deploy them
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(FORM_LOGIN_READ_LISTENER_APP_NAME);
            LOG.info("addAppToServer : " + FORM_LOGIN_READ_LISTENER_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
              ShrinkHelper.exportAppToServer(SHARED_SERVER.getLibertyServer(), FormLoginReadListenerApp);
          }
        //Replace config for the other server
        LibertyServer wlp = SHARED_SERVER.getLibertyServer();
        wlp.saveServerConfiguration();
        wlp.setServerConfigurationFile("FormLogin_ReadListener/server.xml");
        
        SHARED_SERVER.startIfNotStarted();
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + FORM_LOGIN_READ_LISTENER_APP_NAME);
    }
    
    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        SHARED_SERVER.getLibertyServer().restoreServerConfiguration();
        if (SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer("SRVE8015E:.*");
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