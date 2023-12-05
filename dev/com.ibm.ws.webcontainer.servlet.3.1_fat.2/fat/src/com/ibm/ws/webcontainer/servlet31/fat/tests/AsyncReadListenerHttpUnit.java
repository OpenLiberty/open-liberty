/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.webcontainer.servlet31.fat.FATSuite;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AsyncReadListenerHttpUnit {
    private static final Logger LOG = Logger.getLogger(AsyncReadListenerHttpUnit.class.getName());

    @Server("servlet31_asyncReadListenerServer")
    public static LibertyServer server;

    private static final String LIBERTY_WRITE_LISTENER_FILTER_APP_NAME = "LibertyWriteListenerFilterTest";
    private static final String LIBERTY_READ_LISTENER_FILTER_APP_NAME = "LibertyReadListenerFilterTest";
    private static final String LIBERTY_READ_WRITE_LISTENER_APP_NAME = "LibertyReadWriteListenerTest";

    private static final String READ_LISTENER_SERVLET_URL = "/LibertyReadWriteListenerTest/TestAsyncReadServlet";
    private static final String READ_LISTENER_SERVLET_FALSE_URL = "/LibertyReadWriteListenerTest/BasicReadListenerAsyncFalseServlet";
    private static final String READ_LISTENER__FILTER_SERVLET_URL = "/LibertyReadListenerFilterTest/ReadListenerFilterServlet";

    private static boolean runningNetty = false;

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the war apps and add the dependencies
        WebArchive libertyWriteListenerFilterApp = ShrinkHelper.buildDefaultApp(LIBERTY_WRITE_LISTENER_FILTER_APP_NAME + ".war",
                                                                                "com.ibm.ws.webcontainer.servlet_31_fat.libertywritelistenerfiltertest.war.writeListener");
        libertyWriteListenerFilterApp = (WebArchive) ShrinkHelper.addDirectory(libertyWriteListenerFilterApp, "test-applications/LibertyWriteListenerFilterTest.war/resources");
        WebArchive libertyReadListenerFilterApp = ShrinkHelper.buildDefaultApp(LIBERTY_READ_LISTENER_FILTER_APP_NAME + ".war",
                                                                               "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadlistenerfiltertest.war.readListener");
        libertyReadListenerFilterApp = (WebArchive) ShrinkHelper.addDirectory(libertyReadListenerFilterApp, "test-applications/LibertyReadListenerFilterTest.war/resources");
        WebArchive libertyReadWriteListenerApp = ShrinkHelper.buildDefaultApp(LIBERTY_READ_WRITE_LISTENER_APP_NAME + ".war",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.readListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.writeListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.upgradeHandler");
        libertyReadWriteListenerApp = (WebArchive) ShrinkHelper.addDirectory(libertyReadWriteListenerApp, "test-applications/LibertyReadWriteListenerTest.war/resources");

        // Export the applications.
        ShrinkHelper.exportDropinAppToServer(server, libertyWriteListenerFilterApp);
        ShrinkHelper.exportDropinAppToServer(server, libertyReadListenerFilterApp);
        ShrinkHelper.exportDropinAppToServer(server, libertyReadWriteListenerApp);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(AsyncReadListenerHttpUnit.class.getSimpleName() + ".log");

        if (FATSuite.isWindows) {
            FATSuite.setDynamicTrace(server, "*=info=enabled");
        }

        // Go through Logs and check if Netty is being used
        // Wait for endpoints to finish loading and get the endpoint started messages
        server.waitForStringInLog("CWWKO0219I.*");
        List<String> test = server.findStringsInLogs("CWWKO0219I.*");
        String CLASS_NAME = AsyncReadListenerHttpUnit.class.getName();
        if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, CLASS_NAME, "test()", "Got port list...... " + Arrays.toString(test.toArray()));
            LOG.logp(Level.INFO, CLASS_NAME, "test()", "Looking for port: " + server.getHttpDefaultPort());
        }
        for (String endpoint : test) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.logp(Level.INFO, CLASS_NAME, "test()", "Endpoint: " + endpoint);
            }
            if (!endpoint.contains("port " + Integer.toString(server.getHttpDefaultPort())))
                continue;
            if (LOG.isLoggable(Level.INFO)) {
                LOG.logp(Level.INFO, CLASS_NAME, "test()", "Netty? " + endpoint.contains("io.openliberty.netty.internal.tcp.TCPUtils"));
            }
            runningNetty = endpoint.contains("io.openliberty.netty.internal.tcp.TCPUtils");
            break;
        }

    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE9010E:.*", "SRVE9004E:.*", "SRVE8015E:.*", "SRVE9008E:.*", "SRVE9006E:.*", "SRVE8015E:.*");
        }
    }

    /*
     * test sending chunk data of varying sizes to the servlet to test basic functionality of readListener .The servlet
     * passes back to the client, the size of data received, which is then compared with the data size sent.
     * test passes
     */

    @Test
    public void test_ReadVariousInputDataSizes_AsyncRL() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ReadVariousInputDataSizes_AsyncRL Start");
        LOG.info("\n /************************************************************************************/");

        String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        int[] PostDataSize = { 100, 1024, 10240, 102400, 1024000 };
        URL url = new URL(URLString);
        HttpURLConnection con = null;
        OutputStream os = null;
        int b = 1;
        int ResponseCode = 0;
        String ReturnedDataSize = "";
        int inputData = 0;
        try {

            for (int j = 0; j < PostDataSize.length; j++) {
                inputData = PostDataSize[j];
                LOG.info("Sending data -> " + inputData);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("TestToCall", "test_ReadVariousInputDataSizes_AsyncRL");
                LOG.info("\n [TestRequestProperty]: test_ReadVariousInputDataSizes_AsyncRL");
                con.setRequestProperty("TestInputData", String.valueOf(inputData)); //Use these request headers to correlate the requests in server log
                con.setDoOutput(true);
                con.setDoInput(true);
                //con.setChunkedStreamingMode(32768);           //see RTC 273647
                con.setFixedLengthStreamingMode(inputData);
                con.connect();
                LOG.info("\n Writing the request to the server");
                os = con.getOutputStream();
                for (long i = 0; i < PostDataSize[j]; i++) {
                    os.write(b);
                }
                os.flush();
                os.close();
                String status = con.getHeaderField(0);
                ResponseCode = con.getResponseCode();
                ReturnedDataSize = con.getHeaderField("PostDataRead");
                LOG.info("\n Response posted data : " + ReturnedDataSize);
                con.disconnect();
                assertEquals(URLString + " : Response status = " + status + " : Response code is not " + 200, 200, ResponseCode);
                if (PostDataSize[j] > 0) {
                    assertEquals(URLString + " : Wrong length of posted data.", Long.toString(PostDataSize[j]), ReturnedDataSize);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ReadVariousInputDataSizes_AsyncRL Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * test to check if the onError method in the servlet gets called. We send more data than maximum data that can
     * be sent over a connection to cause an error,
     * we expect an IOException which gets thrown.
     * test passes.
     */

    @Test(expected = IOException.class)
    public void test_OnError_AsyncRL() throws IOException {

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_OnError_AsyncRL Start");
        LOG.info("\n /************************************************************************************/");

        String UrlParameters = "param1=testValue";
        byte[] PostDataBytes = UrlParameters.toString().getBytes("UTF-8");
        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String ExpectedData = "OnError method successfully called !!!";

        URL url = new URL(request);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("TestToCall", "test_OnError_AsyncRL");
            LOG.info("\n [TestRequestProperty]: test_OnError_AsyncRL");
            connection.setFixedLengthStreamingMode(1);
            connection.connect();
            LOG.info("\n Writing the request to the server");
            connection.getOutputStream().write(PostDataBytes);
            LOG.info("\n Start reading the response");
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            LOG.info("\n Data read from the response :  " + sb.toString());
            connection.disconnect();

            assertEquals(request + " : The response did not equal the expected data. Response = " + sb.toString() + ", Expected data = " + ExpectedData, ExpectedData,
                         sb.toString());
        } finally {
            LOG.info("\n /************************************************************************************/");
            LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_OnError_AsyncRL Finish");
            LOG.info("\n /************************************************************************************/");
        }
    }

    /*
     * this test registers a second ReadListener. This is not permitted and the test throws an IllegalStateException.
     * test passes
     */
    @Test
    public void test_Exception_onSecondReadListener() throws Exception {

        // Make sure the test framework knows that SRVE9008E is expected

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_Exception_onSecondReadListener Start");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String ExpectedData = "java.lang.IllegalStateException: SRVE9008E: An attempt to set a ReadListener failed because the ReadListener is already set.";
        URL url = new URL(request);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("TestToCall", "test_Exception_onSecondReadListener");
            LOG.info("\n [TestRequestProperty]: test_Exception_onSecondReadListener");
            connection.setRequestMethod("POST");
            connection.connect();
            LOG.info("\n Start reading the response");
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            LOG.info("\n Data read from the response :  " + sb.toString());
            connection.disconnect();

            assertEquals(request + " : The response did not equal the expected data. Response = " + sb.toString() + ", Expected data = " + ExpectedData, ExpectedData,
                         sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_Exception_onSecondReadListener Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * this test registers a null ReadListener. This is not permitted and the test throws an NullPointerException.
     * test passes
     */
    @Test
    public void test_Exception_onNullReadListener() throws Exception {

        // Make sure the test framework knows that SRVE9004E is expected

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_Exception_onNullReadListener Start");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String ExpectedData = "SRVE9004E";
        URL url = new URL(request);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("TestToCall", "test_Exception_onNullReadListener");
            LOG.info("\n [TestRequestProperty]: test_Exception_onNullReadListener");
            connection.setRequestMethod("POST");
            connection.connect();
            LOG.info("\n Start reading the response");
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            LOG.info("\n Data read from the response : " + sb.toString());
            connection.disconnect();
            assertTrue(request + " : Response = " + sb.toString() + " , Response does not contain to: " + ExpectedData, sb.toString().contains(ExpectedData));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_Exception_onNullReadListener Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * Read data after isReady() in onDataAvailable method in readListenerImpl called from the servlet
     * has returned false - should get an IllegalStateException.
     */

    @Test
    public void test_Exception_onReadingData_isReadyFalse() throws Exception {

        // Make sure the test framework knows that SRVE9010E is expected

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_Exception_onReadingData_isReadyFalse Start");
        LOG.info("\n /************************************************************************************/");

        if(runningNetty){
            LOG.info("\n /************************************************************************************/");
            LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_Exception_onReadingData_isReadyFalse Skipped due to Netty difference");
            LOG.info("\n /************************************************************************************/");
            return;
        }

        final String EXPECTED_DATA = "java.lang.IllegalStateException: SRVE9010E: An attempt to read failed because isReady API returns false";
        String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        //We don't need a very large post since the size is irrelevant here. We just want to
        //end up triggering isReady returning false
        int PostDataSize = 2000;
        URL url = new URL(URLString);
        HttpURLConnection con = null;
        OutputStream os = null;
        int b = 1;
        int ResponseCode = 0;

        String line = "";
        StringBuilder sb = new StringBuilder();
        try {

            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("TestToCall", "test_Exception_onReadingData_isReadyFalse");
            LOG.info("\n [TestRequestProperty]: test_Exception_onReadingData_isReadyFalse");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setChunkedStreamingMode(32768);
            con.connect();
            os = con.getOutputStream();
            LOG.info("\n Writing the request to the server ");
            //We don't need to write a whole lot of data. All we need to write is enough for the server to
            //read it and then think it's not done. By not calling close right away this side won't send
            //the 0 byte chunk and the server will think there is more data to read
            //The above will trigger isReady to return false in the app code
            for (long i = 0; i < PostDataSize / 2; i++) {
                os.write(b);
            }
            //Forcing the write of data so a chunk will be written out
            os.flush();

            LOG.info("\n Writing complete. Sleeping for 10 seconds so the server has time to read the data ");
            //Sleep so the server has plenty of time to read the data and think there is still more
            Thread.sleep(5000);
            //Close the connection as we're done at this point
            //This triggers sending the 0 byte chunk
            os.close();

            //Get the response from the server and find out if we have passed or not
            //We're expecting a 500 status code(set by our ReadListener) and the IllegalStateException message
            ResponseCode = con.getResponseCode();
            InputStream is = con.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            //We should be seeing the exception message as the body data
            LOG.info("\n Data read from the response : " + sb.toString());

            con.disconnect();

            assertEquals(URLString + " : Response code is not " + 500, 500, ResponseCode);
            assertEquals(URLString + " : The response did not equal the expected data. Response = " + sb.toString() + ", Expected data = " + EXPECTED_DATA, EXPECTED_DATA,
                         sb.toString());
        }

        catch (Exception e) {
            e.printStackTrace();
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_Exception_onReadingData_isReadyFalse Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * Call isReady() within the ReadLister after it has already returned false. Previously this would result in an exception
     * but now it should be passing and we get the expected exception below.
     *
     * Previously the test behavior would return:
     * com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException: Illegal chunk length digit: 0
     */

    @Test
    public void test_IsReadyAfterIsReadyFalse() throws Exception {

        // Make sure the test framework knows that SRVE9010E is expected

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_IsReadyAfterIsReadyFalse Start");
        LOG.info("\n /************************************************************************************/");

        final String EXPECTED_DATA = "java.lang.IllegalStateException: SRVE9010E: An attempt to read failed because isReady API returns false";
        String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        //We don't need a very large post since the size is irrelevant here. We just want to
        //end up triggering isReady returning false
        int PostDataSize = 2000;
        URL url = new URL(URLString);
        HttpURLConnection con = null;
        OutputStream os = null;
        int b = 1;
        int ResponseCode = 0;

        String line = "";
        StringBuilder sb = new StringBuilder();
        try {

            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("TestToCall", "test_IsReadyAfterIsReadyFalse");
            LOG.info("\n [TestRequestProperty]: test_IsReadyAfterIsReadyFalse");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setChunkedStreamingMode(32768);
            con.connect();
            os = con.getOutputStream();
            LOG.info("\n Writing the request to the server ");
            //We don't need to write a whole lot of data. All we need to write is enough for the server to
            //read it and then think it's not done. By not calling close right away this side won't send
            //the 0 byte chunk and the server will think there is more data to read
            //The above will trigger isReady to return false in the app code
            for (long i = 0; i < PostDataSize / 2; i++) {
                os.write(b);
            }
            //Forcing the write of data so a chunk will be written out
            os.flush();

            LOG.info("\n Writing complete. Sleeping for 10 seconds so the server has time to read the data ");
            //Sleep so the server has plenty of time to read the data and think there is still more
            Thread.sleep(5000);
            //Close the connection as we're done at this point
            //This triggers sending the 0 byte chunk
            os.close();

            //Get the response from the server and find out if we have passed or not
            //We're expecting a 200 status code and the IllegalStateException message
            ResponseCode = con.getResponseCode();
            InputStream is = con.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            //We should be seeing the exception message as the body data
            LOG.info("\n Data read from the response : " + sb.toString());

            con.disconnect();

            assertEquals(URLString + " : Response code is not " + 200, 200, ResponseCode);
            assertEquals(URLString + " : The response did not equal the expected data. Response = " + sb.toString() + ", Expected data = " + EXPECTED_DATA, EXPECTED_DATA,
                         sb.toString());
        }

        catch (Exception e) {
            e.printStackTrace();
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_IsReadyAfterIsReadyFalse Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * Have onDataAvailable in servlet throw an exception -
     * test passes
     */

    @Test
    public void test_HandleException_ThrownByOnDataAvailable() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_HandleException_ThrownByOnDataAvailable Start");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String POST_DATA = "testKey=testValue";
        final String EXPECTED_DATA = POST_DATA + ", Exception thrown from onDataAvailable";
        byte[] utf8Bytes = POST_DATA.getBytes("UTF-8");
        try {
            URL url = new URL(request);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("TestToCall", "test_HandleException_ThrownByOnDataAvailable");
            LOG.info("\n [TestRequestProperty]: test_HandleException_ThrownByOnDataAvailable");
            connection.connect();
            LOG.info("\n Writing the request to the server");
            OutputStream os = connection.getOutputStream();
            os.write(utf8Bytes);
            os.flush();
            os.close();
            LOG.info("\n Start reading the response");
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            LOG.info("\n Data read from the response : " + sb.toString());
            connection.disconnect();
            assertEquals(request + " : The response did not equal the expected data. Response = " + sb.toString() + ", Expected data = " + EXPECTED_DATA, EXPECTED_DATA,
                         sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_HandleException_ThrownByOnDataAvailable Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * Read some post data from getting an ServletInputStream and then try registering a readListener.
     * No exceptions occur. This is expected.
     * Test passes
     */
    @Test
    public void test_ReadData_BeforeRL() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ReadData_BeforeRL Start");
        LOG.info("\n /************************************************************************************/");

        final String PostData = "helloworld-before registering ReadListener";
        final String EXPECTED_DATA = PostData + "All Data Read from client before ReadListener invoked!";
        byte[] utf8Bytes = PostData.getBytes("UTF-8");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        // final String ExpectedData = "java.lang.IllegalStateException";

        try {

            URL url = new URL(request);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("TestToCall", "test_ReadData_BeforeRL");
            LOG.info("\n [TestRequestProperty]: test_ReadData_BeforeRL");
            connection.connect();
            LOG.info("\n Writing the request to the server");
            OutputStream os = connection.getOutputStream();
            os.write(utf8Bytes);
            os.flush();
            os.close();
            LOG.info("\n Start reading the response");
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            LOG.info("\n Data read from the response : " + sb.toString());
            connection.disconnect();
            assertEquals("The response did not equal the expected data. Response = " + sb.toString()
                         + ", Expected data = " + EXPECTED_DATA, EXPECTED_DATA, sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ReadData_BeforeRL Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * register a readListener from a filter
     * Test passes.
     */
    @Test
    public void test_ReadData_setRLinFilter() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ReadData_setRLinFilter Start");
        LOG.info("\n /************************************************************************************/");
        try {
            HttpClient HttpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER__FILTER_SERVLET_URL);
            post.addParameter("testKey", "testValue");
            final String POST_DATA = "testKey=testValue";

            int responseCode = HttpClient.executeMethod(post);
            LOG.info("Status Code = " + responseCode);

            String responseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + responseBody);

            post.releaseConnection();

            Assert.assertTrue("The response body did not equal the post data. Response body = " + responseBody + " post data = " + POST_DATA,
                              responseBody.equals(POST_DATA));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ReadData_setRLinFilter Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * Have onAllDataRead() method in servlet throw an exception -
     * Test passes
     */
    @Test
    public void test_HandleException_ThrownByOnAllDataRead() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_HandleException_ThrownByOnAllDataRead Start");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String POST_DATA = "testKey=testValue";
        final String EXPECTED_DATA = POST_DATA + ", Exception thrown from onAllDataRead";
        byte[] utf8Bytes = POST_DATA.getBytes("UTF-8");
        URL url = new URL(request);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("TestToCall", "test_HandleException_ThrownByOnAllDataRead");
            LOG.info("\n [TestRequestProperty]: test_HandleException_ThrownByOnAllDataRead");
            connection.connect();
            LOG.info("\n Writing the request to the server");
            OutputStream os = connection.getOutputStream();
            os.write(utf8Bytes);
            os.flush();
            os.close();
            LOG.info("\n Start reading the response");
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            LOG.info("\n Data read from the response : " + sb.toString());
            connection.disconnect();
            assertEquals(request + " : The response did not equal the expected data. Response = " + sb.toString() + ", Expected data = " + EXPECTED_DATA, EXPECTED_DATA,
                         sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_HandleException_ThrownByOnAllDataRead Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * Add a readListener to a non-async servlet. This should result in an Illegal State Exception.
     * Test passes
     */

    @Test
    public void test_Exception_setRL_onNonAsyncServlet() throws HttpException, IOException {

        // Make sure the test framework knows that SRVE9006E is expected

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_Exception_setRL_onNonAsyncServlet Start");
        LOG.info("\n /************************************************************************************/");

        String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_FALSE_URL;

        URL url = new URL(URLString);
        HttpURLConnection con = null;

        String ExpectedData = "java.lang.IllegalStateException: SRVE9006E: An attempt to set a ReadListener failed because the associated request does not have async started or the request is not upgraded.";
        try {

            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("TestToCall", "test_Exception_setRL_onNonAsyncServlet");
            LOG.info("\n [TestRequestProperty]: test_Exception_setRL_onNonAsyncServlet");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setChunkedStreamingMode(32768);
            con.connect();

            String line = "";
            StringBuilder sb = new StringBuilder();

            con.getOutputStream().close();
            LOG.info("\n Start reading the response");
            InputStream is = con.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            LOG.info("\n Data read from the response : " + sb.toString());
            con.disconnect();
            assertEquals(URLString + " : The response did not equal the expected data. Response = " + sb.toString() + ", Expected data = " + ExpectedData, ExpectedData,
                         sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_Exception_setRL_onNonAsyncServlet Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * Return from onDataAvailable even though isReady() returns true. No abnormal behavior noticed.
     * No exceptions thrown. Data is not read.
     * test passes.
     */
    @Test
    public void test_ReadData_onDataAvailableReturn() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ReadData_onDataAvailableReturn Start");
        LOG.info("\n /************************************************************************************/");

        try {
            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL);
            post.addParameter("testKey", "testValue");
            post.setRequestHeader("TestToCall", "test_ReadData_onDataAvailableReturn");
            LOG.info("\n [TestRequestProperty]: test_ReadData_onDataAvailableReturn");
            final String POST_DATA = "testKey=testValue";

            int ResponseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + ResponseCode);

            String ResponseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + ResponseBody);

            post.releaseConnection();

            Assert.assertFalse("The response body did not equal the post data. Response body = " + ResponseBody + " post data = " + POST_DATA,
                               ResponseBody.equals(POST_DATA));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ReadData_onDataAvailableReturn Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * Have onAllDataRead() method in servlet throw an exception -
     * Test passes
     */
    @Test
    public void test_OnReadParameter_WhenRLset() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_OnReadParameter_WhenRLset Start");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL + "?parameter=value";
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String POST_DATA = "testKey=testValue";
        byte[] utf8Bytes = POST_DATA.getBytes("UTF-8");
        URL url = new URL(request);
        String ExpectedData = "parameter=value";
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("TestToCall", "test_OnReadParameter_WhenRLset");
            LOG.info("\n [TestRequestProperty]: test_OnReadParameter_WhenRLset");
            connection.connect();
            LOG.info("\n Writing the request to the server");
            OutputStream os = connection.getOutputStream();
            os.write(utf8Bytes);
            os.flush();
            os.close();
            LOG.info("\n Start reading the response");
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            LOG.info("\n Data read from the response : " + sb.toString());
            connection.disconnect();
            boolean testPass = sb.toString().contains(ExpectedData);
            assertTrue(request + " : Response = " + sb.toString() + " ,  Response does not contain to: " + ExpectedData, testPass);
        } catch (Exception e) {
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_OnReadParameter_WhenRLset Finish");
        LOG.info("\n /************************************************************************************/");
    }

    /*
     * Verify in the ReadListener that the Contexts have transfered properly
     * Test passes
     */
    @Test
    public void test_ContextTransferProperly_WhenRLset() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ContextTransferProperly_WhenRLset Start");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + READ_LISTENER_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String ExpectedData = "javax.naming.NameNotFoundException: javax.naming.NameNotFoundException: java:comp/UserTransaction";
        URL url = new URL(request);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("TestToCall", "test_ContextTransferProperly_WhenRLset");
            LOG.info("\n [TestRequestProperty]: test_ContextTransferProperly_WhenRLset");
            connection.setRequestMethod("POST");
            connection.connect();
            LOG.info("\n Start reading the response");
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            connection.disconnect();
            LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ContextTransferProperly_WhenRLset String returned : " + sb.toString());
            assertEquals(request + " : The response did not equal the expected data. Response = " + sb.toString() + ", Expected data = " + ExpectedData, ExpectedData,
                         sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: test_ContextTransferProperly_WhenRLset Finish");
        LOG.info("\n /************************************************************************************/");
    }
}
