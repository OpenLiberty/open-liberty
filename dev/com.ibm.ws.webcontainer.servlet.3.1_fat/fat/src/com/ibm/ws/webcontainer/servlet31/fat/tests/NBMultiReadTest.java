/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class NBMultiReadTest {
    private static final Logger LOG = Logger.getLogger(NBMultiReadTest.class.getName());
    private static LibertyServer server;

    private static final String MULTI_NB_READ_APP_NAME = "multiNBReadApp";
    private final static Class<?> c = NBMultiReadTest.class;
    protected static final int CONN_TIMEOUT = 5;
    private final String contextRoot = "/multiNBReadApp";

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setupClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("NBmultiReadServer");
        WebArchive NBMultiReadApp = ShrinkHelper.buildDefaultApp(MULTI_NB_READ_APP_NAME + ".war",
                                                                  "com.ibm.ws.webcontainer.servlet_31_fat.multinbreadapp.war.test.filters",
                                                                  "com.ibm.ws.webcontainer.servlet_31_fat.multinbreadapp.war.test.listeners",
                                                                  "com.ibm.ws.webcontainer.servlet_31_fat.multinbreadapp.war.test.servlets");
        server.startServer(NBMultiReadTest.class.getSimpleName() + ".log");
        // Verify if the apps are in the server before trying to deploy them
        if (server.isStarted()) {
            Set<String> appInstalled = server.getInstalledAppNames(MULTI_NB_READ_APP_NAME);
            LOG.info("addAppToServer : " + MULTI_NB_READ_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
              ShrinkHelper.exportDropinAppToServer(server, NBMultiReadApp);
          }
        assertNotNull("The application NBmultiread did not appear to have started", server.waitForStringInLog("CWWKZ0001I.* " + MULTI_NB_READ_APP_NAME));
    }

    /**
     * @param path
     * @return
     * @throws MalformedURLException
     */
    private URL getURL(String path) throws MalformedURLException {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + contextRoot + path);
        Log.info(c, "getURL", " url -->" + url);

        return url;
    }

    /**
     * @param path
     * @return
     * @throws MalformedURLException
     */
    private URL getNofilterURL(String path) throws MalformedURLException {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + contextRoot + "/Nofilter" + path);
        Log.info(c, "getNofilterURL", " url -->" + url);
        return url;
    }

    /**
     * @throws Throwable
     */
    //1.
    @Test
    public void testAppInstalled() throws Throwable {
        checkResponseCode(getURL("/index.html"), 200);
    }

    // async RL getParameter (not allowed )
    // expect no parameter
    //2.
    @Test
    public void test_NoFilter_NBReadParamterServlet() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadParameterServlet"), "F003449Test=NBReadParameterServlet", "getParamNotAllowed");
    }

    // async RL getParameter (not allowed ) , async getInputStream read (allowed ) close
    //3.
    @Test
    public void test_NoFilter_NBReadParamterServletReadStream() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadParameterServletReadStream"), "F003449Test=NBReadParameterServletReadStream", "getParamNotAllowedReadStream");
    }

    // NoFilter, async RL getInputStream (allowed ) close , sync getInputStream read  (allowed ) close
    //4.
    @Test
    public void test_NoFilter_NBReadPostDataFromInputStreamServlet() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadPostDataFromInputStreamServlet"), "F003449Test=NBReadPostDataFromInputStreamServlet", "NBReadPostDataFromInputStreamServlet");
    }

    // Filter,, async RL getInputStream (allowed ) close , sync getInputStream read  (allowed ) close, nothing to do for servlet
    //5.
    @Test
    public void test_ReadPostDataFromNBInputStreamFilter_NoWorkServlet() throws Throwable {

        checkPostRequest(getURL("/ReadPostDataFromNBInputStreamFilter/NoWorkServlet"), "F003449Test=ReadPostDataFromNBInputStreamFilter_NoWorkServlet",
                         "ReadPostDataFromNBInputStreamFilter_NoWorkServlet");
    }

    // Filter,, non-async : getParameter , async RL getInputStream (allowed ) close, nothing to do for servlet
    //6.
    @Test
    public void test_ReadParamterFilter_ReadPostDataFromInputStreamServlet() throws Throwable {

        checkPostRequest(getURL("/ReadParameterFilter/NBReadPostDataFromInputStreamServlet"), "F003449Test=ReadParameterFilter_ReadPostDataFromInputStreamServlet",
                         "ReadParameterFilter_ReadPostDataFromInputStreamServlet");
    }

    // Filter,, non-async : getParameter , async RL getInputStream (allowed ) close, nothing to do for servlet
    //6.
    @Test
    public void test_ReadParamterReadPostDataFromInputStreamFilter_NoWorkServlet() throws Throwable {

        checkPostRequest(getURL("/ReadParameterNBReadFilter/NoWorkServlet"), "F003449Test=ReadParameterNBReadFilter_NoWorkServlet", "ReadParameterNBReadFilter_NoWorkServlet");
    }

    //NoFilter, async RL Read Large getInputStream (allowed ) close , Read three times sync getInputStream read  (allowed ) close
    //7.
    @Test
    public void test_NoFilter_InputStream_ReadLargePostData() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadLargePostDataInputStreamServlet"), "F003449Test=Large,Num=3,Data=" + generateData(16384),
                         "NBReadLargePostDataInputStreamServlet Large");
    }

    //8.
    //NoFilter, async RL Read Large getInputStream (allowed ) close , Read three times sync getInputStream Skip read  (allowed ) close
    @Test
    public void test_NoFilter_InputStream_SkipPostData() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadLargePostDataInputStreamServlet"), "F003449Test=Skip,Num=3,Len=24,Data=" + generateData(128),
                         "NBReadLargePostDataInputStreamServlet Skip");
    }

    //9.
    @Test
    public void test_NoFilter_InputStream_ReadAsBytesPostData() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadLargePostDataInputStreamServlet"), "F003449Test=ReadAsBytes,Num=3,Data=" + generateData(26),
                         "NBReadLargePostDataInputStreamServlet ReadAsBytes");
    }

    //10.
    @Test
    public void test_NoFilter_InputStream_ReadAsByteArraysPostData() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadLargePostDataInputStreamServlet"), "F003449Test=ReadAsByteArrays,Num=3,Data=" + generateData(256),
                         "NBReadLargePostDataInputStreamServlet ReadAsByteArrays");
    }

    //11.
    @Test
    public void test_NoFilter_InputStream_AvailablePostData() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadLargePostDataInputStreamServlet"), "F003449Test=Available,Num=3,Len=32,Data=" + generateData(583),
                         "NBReadLargePostDataInputStreamServlet Available");
    }

    //12.
    @Test
    public void test_NoFilter_InputStream_MixitPostData() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadLargePostDataInputStreamServlet"), "F003449Test=Mixit,Num=3,Len=18,Data=" + generateData(88),
                         "NBReadLargePostDataInputStreamServlet Mixit");
    }

    //13.
    @Test
    public void test_NoFilter_NBInputStream_NBInputStream() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadStreamNBReadStreamServlet"), "F003449Test=NBReadStreamNBReadStreamServlet " + generateData(100), "NBReadStreamNBReadStreamServlet");
    }

    //14.
    @Test
    public void test_NoFilter_NBInputStream_Reader() throws Throwable {

        checkPostRequest(getNofilterURL("/NBReadPostDataFromInputStreamServlet"), "F003449Test=NBInputStream_Reader " + generateData(100), "NBInputStream_Reader");
    }

    /**
     * @param resource
     * @param responseCode
     * @throws Throwable
     */
    private void checkResponseCode(URL url, int responseCode) throws Throwable {
        HttpURLConnection con = null;
        try {

            con = HttpUtils.getHttpConnection(url, responseCode, CONN_TIMEOUT);

            int respCode = con.getResponseCode();
            assertEquals("Response code is not as expected", responseCode, respCode);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

    }

    /**
     * @param URLString
     * @param postData
     * @throws Throwable
     */
    private void checkPostRequest(URL url, String postData, String testTorun) throws Throwable {

        int responseCode = 0;
        String resp = "";
        try {

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            Log.info(c, "checkPostRequest", "Posting " + postData.length() + " bytes of data to " + url + "?" + postData.substring(0, Math.min(36, postData.length())));
            //System.out.println("Posting " + postData.length() + " bytes of data to " + url + "?" + postData.substring(0, Math.min(36, postData.length())));

            con.setRequestMethod("POST");
            if (testTorun != null)
                con.setRequestProperty("TestToCall", testTorun);
            con.setDoOutput(true);
            con.setRequestProperty("Content-Length", Long.toString(postData.length()));
            OutputStream os = con.getOutputStream();
            os.write(postData.getBytes());
            os.flush();
            os.close();

            responseCode = con.getResponseCode();
            Log.info(c, "checkPostRequest", "Response code : " + responseCode);
            //System.out.println("Response code : " + responseCode);

            assertEquals(url + " : Response code is not 200.", 200, responseCode);

            java.io.InputStream data = con.getInputStream();
            StringBuffer dataBuffer = new StringBuffer();
            byte[] dataBytes = new byte[1024];
            for (int n; (n = data.read(dataBytes)) != -1;) {
                dataBuffer.append(new String(dataBytes, 0, n));
            }
            Log.info(c, "checkPostRequest", "Response was: " + dataBuffer);
            //System.out.println("Response was: " + dataBuffer);

            boolean testWorked = dataBuffer.indexOf("PASS") == 0;
            assertTrue(url + " : " + resp, testWorked);

            con.disconnect();
            Log.info(c, "checkPostRequest", "-----------------------------[" + testTorun + "] Finish---------------------------------");
            Log.info(c, "checkPostRequest", "----------------------------------------------------------------------------------------");

        } catch (Exception e) {
            Log.info(c, "checkPostRequest", "Exception from request: " + e.getMessage());
            //System.out.println("Exception from request: " + e.getMessage());
        }

    }

    @SuppressWarnings("unused")
    private String generateData(int num) {
        String data = "";
        String dataString = "TestingTesting123";
        int dataStringLen = dataString.length();
        for (int i = 0; i < num; i += dataStringLen) {
            data += "TestingTesting123";
        }
        return data;
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        Log.info(c, "tearDownClass", "Stopping the server...");
        server.stopServer();
    }

}
