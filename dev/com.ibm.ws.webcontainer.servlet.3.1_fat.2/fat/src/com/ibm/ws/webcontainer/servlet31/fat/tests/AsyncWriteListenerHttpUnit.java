/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
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

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AsyncWriteListenerHttpUnit {

    @Server("servlet31_asyncWriteListenerServer")
    public static LibertyServer server;

    private static final String LIBERTY_WRITE_LISTENER_FILTER_APP_NAME = "LibertyWriteListenerFilterTest";
    private static final String LIBERTY_READ_WRITE_LISTENER_APP_NAME = "LibertyReadWriteListenerTest";

    private static final String WRITE_LISTENER_SERVLET_URL = "/LibertyReadWriteListenerTest/TestAsyncWriteServlet";
    private static final String WRITE_LISTENER__FILTER_SERVLET_URL = "/LibertyWriteListenerFilterTest/WriteListenerFilterServlet";
    private static final String WRITE_LISTENER_SERVLET_FALSE_URL = "/LibertyReadWriteListenerTest/TestAsyncFalseWriteServlet";

    private static final Logger LOG = Logger.getLogger(AsyncWriteListenerHttpUnit.class.getName());

    //1) TestWrite_DontCheckisReady_fromWL
    //2) TestWriteFromServlet_AftersetWL
    //3) TestWL_Write_Medium
    //4) TestWL_Write_Large
    //5) TestWL_Println_Large
    //6) TestWL_Write_MediumChunks
    //7) TestWL_Write_LargeChunks
    //8) TestWL_Println_MediumChunks
    //9) TestWL_Println_LargeChunks
    //10) Test_ISE_SetWL_NonAsyncServlet
    //11) Test_ISE_setSecondWriteListener
    //12) Test_NPE_setNullWriteListener
    //13) Test_Return_onWritePossible
    //14) TestWL_IOE_AfterWrite
    //15)TestWL_onError
    //16)TestWL_ContextTransferProperly
    //17)TestWL_Write_Less_InFilter
    //18)TestWriteFromFilter_AftersetWL

    @BeforeClass
    public static void setupClass() throws Exception {
        // SHARED_SERVER.getLibertyServer().stopServer(false, "SRVE8015E:.*");
        // Build the war app CDI12TestV2Injection.war and add the dependencies
        WebArchive libertyWriteListenerFilterApp = ShrinkHelper.buildDefaultApp(LIBERTY_WRITE_LISTENER_FILTER_APP_NAME + ".war",
                                                                                "com.ibm.ws.webcontainer.servlet_31_fat.libertywritelistenerfiltertest.war.writeListener");
        libertyWriteListenerFilterApp = (WebArchive) ShrinkHelper.addDirectory(libertyWriteListenerFilterApp, "test-applications/LibertyWriteListenerFilterTest.war/resources");
        WebArchive libertyReadWriteListenerApp = ShrinkHelper.buildDefaultApp(LIBERTY_READ_WRITE_LISTENER_APP_NAME + ".war",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.readListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.writeListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.upgradeHandler");
        libertyReadWriteListenerApp = (WebArchive) ShrinkHelper.addDirectory(libertyReadWriteListenerApp, "test-applications/LibertyReadWriteListenerTest.war/resources");

        // Export the applications.
        ShrinkHelper.exportDropinAppToServer(server, libertyWriteListenerFilterApp);
        ShrinkHelper.exportDropinAppToServer(server, libertyReadWriteListenerApp);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(AsyncWriteListenerHttpUnit.class.getSimpleName() + ".log");
        server.waitForStringInLogUsingMark("CWWKO0219I*");

        if (FATSuite.isWindows) {
            FATSuite.setDynamicTrace(server, "*=info=enabled");
        }
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0918E:.*", "SRVE8015E:.*", "SRVE9007E:.*", "SRVE9009E:.*", "SRVE9005E:.*", "SRVE8015E:.*", "SRVE8055E*");
        }
    }

    @Test
    public void TestWrite_DontCheckisReady_fromWL() throws Exception {
        // Make sure the test framework knows that SRVE0918E is expected

        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        String testToCall = "TestWrite_DontCheckisReady_fromWL";

        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int expectedResponseSize = 100;
        try {
            int actualResponseSize = connectSendExpectDataSizeInResponse(expectedResponseSize, testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);
            //Also check for exception in logs

            String message = server.waitForStringInLogUsingMark("SRVE0918E");
            LOG.info(testToCall + " Entries found in log : " + message);
            assertNotNull("Could not find message", message);

        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + " ,exception is: " + e.toString());
        } finally {
            server.setMarkToEndOfLog();
        }
    }

    // TestWLWriteLessData is now already done in this
    @Test
    public void TestWriteFromServlet_AftersetWL() throws Exception {
        // Make sure the test framework knows that SRVE0918E is expected

        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        String testToCall = "TestWriteFromServlet_AftersetWL";

        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int expectedResponseSize = 1000;
        try {
            int actualResponseSize = connectSendExpectDataSizeInResponse(expectedResponseSize, testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);

            //Also check for this message in logs
//            String msg = "isReady always false from the thread which sets the WL";
//
//            String message = SHARED_SERVER.getLibertyServer().waitForStringInLogUsingMark(msg);
//            LOG.info(testToCall + " Entries found in log : " + message);
//            assertNotNull("Could not find message", message);

            //Also check for exception in logs

            String message = server.waitForStringInLogUsingMark("SRVE0918E");
            LOG.info(testToCall + " Entries found in log : " + message);
            assertNotNull("Could not find message", message);

        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + " ,exception is: " + e.toString());
        } finally {
            server.setMarkToEndOfLog();
        }
    }

    @Test
    public void TestWL_Write_Medium() throws Exception {
        String testToCall = "TestWL_Write_Medium";
        LOG.info("[WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int expectedResponseSize = 500000;
        String type = "write";
        try {
            int actualResponseSize = sendOneorMultiChunks_ExpectDataSizeInResponse(expectedResponseSize, type, "one", testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);
        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + "exception is: " + e.toString());

        }
    }

    @Test
    public void TestWL_Write_Large() throws Exception {
        String testToCall = "TestWL_Write_Large";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int expectedResponseSize = 1000000;
        try {
            int actualResponseSize = sendOneorMultiChunks_ExpectDataSizeInResponse(expectedResponseSize, "write", "one", testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);
        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + " ,exception is: " + e.toString());

        }
    }

    @Test
    public void TestWL_Println_Large() throws Exception {
        String testToCall = "TestWL_Println_Large";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "************");
        int expectedResponseSize = 1000000;

        try {
            int actualResponseSize = sendOneorMultiChunks_ExpectDataSizeInResponse(expectedResponseSize, "println", "one", testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);
        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + " ,exception is: " + e.toString());

        }
    }

    /*
     * Add a writeListener to a non-async servlet. This should result in an Illegal State Exception.
     */
    @Test
    public void Test_ISE_SetWL_NonAsyncServlet() throws Exception {
        String testToCall = "Test_ISE_SetWL_NonAsyncServlet";
        // Make sure the test framework knows that SRVE9014E is expected

        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int responseCode = 0;

        try {
            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_FALSE_URL);
            post.addParameter("testKey", "testValue");
            post.setRequestHeader("TestToCall", testToCall);

            responseCode = httpClient.executeMethod(post);
            LOG.info(testToCall + " Status Code = " + responseCode);

            String responseBody = post.getResponseBodyAsString();
            LOG.info(testToCall + " returned response body is " + responseBody.toString());
            String expectedData1 = "java.lang.IllegalStateException";
            String expectedData2 = "SRVE9007E";
            Boolean pass = false;
            if (responseBody.contains(expectedData1) && responseBody.contains(expectedData2)) {
                pass = true;
            }

            // responseBody-->"java.lang.IllegalStateException: SRVE9007E: An attempt to set a WriteListener failed because the associated request does not have async started or the request is not upgraded.";
            post.releaseConnection();

            org.junit.Assert.assertTrue(pass);
        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage());
        } finally {
            server.setMarkToEndOfLog();
        }
    }

    /*
     * this test registers a second WriteListener. This is not permitted and the test throws an IllegalStateException.
     * test passes
     */
    @Test
    public void Test_ISE_setSecondWriteListener() throws Exception {

        String testToCall = "Test_ISE_setSecondWriteListener";
        // Make sure the test framework knows that SRVE9009E is expected

        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));

        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");

        try {

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_URL);
            post.setRequestHeader("TestToCall", testToCall);
            int responseCode = httpClient.executeMethod(post);
            LOG.info(testToCall + " Status Code = " + responseCode);

            String expectedData1 = "java.lang.IllegalStateException";
            String expectedData2 = "SRVE9009E";

            String message = server.waitForStringInLogUsingMark(expectedData1);
            LOG.info(testToCall + " Entries found in log : " + message);
            assertNotNull("Could not find message", message);

            message = server.waitForStringInLogUsingMark(expectedData2);
            LOG.info(testToCall + " Entries found in log : " + message);
            assertNotNull("Could not find message", message);

//            Since this exception is thrown from servlet after listener is set then it cannot be in response so comment the following

//            String responseBody = post.getResponseBodyAsString();
//            LOG.info("responseBody-->" + responseBody.toString());
//            // responseBody-->java.lang.IllegalStateException: SRVE9009E: An attempt to set a WriteListener failed because the WriteListener is already set.
//
//
//            Boolean pass = false;
//            if (responseBody.contains(expectedData1) && responseBody.contains(expectedData2)) {
//                pass = true;
//            }
//            post.releaseConnection();
//            org.junit.Assert.assertTrue(pass);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        } finally {
            server.setMarkToEndOfLog();
        }
    }

    @Test
    public void Test_NPE_setNullWriteListener() throws Exception {

        String testToCall = "Test_NPE_setNullWriteListener";
        // Make sure the test framework knows that SRVE9014E is expected

        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        try {
            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_URL);
            post.setRequestHeader("TestToCall", testToCall);
            int responseCode = httpClient.executeMethod(post);
            LOG.info(testToCall + " Status Code = " + responseCode);

            String responseBody = post.getResponseBodyAsString();
            LOG.info(testToCall + " responseBody-->" + responseBody.toString());
            // responseBody-->Error 500: java.lang.NullPointerException: SRVE9005E: An attempt to set a WriteListener failed because the WriteListener object was null.

            String expectedData = "SRVE9005E";
            Boolean pass = false;
            if (responseBody.contains(expectedData)) {
                pass = true;
            }
            post.releaseConnection();
            org.junit.Assert.assertTrue(pass);

        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage());
        }
    }

    /*
     * Verify in the ReadListener that the Contexts have transfered properly
     * Test passes
     */
    @Test
    public void TestWL_ContextTransferProperly() throws Exception {

        String testToCall = "TestWL_ContextTransferProperly";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");

        try {

            final String ExpectedData = "javax.naming.NameNotFoundException: javax.naming.NameNotFoundException: java:comp/UserTransaction";

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_URL);
            post.setRequestHeader("TestToCall", testToCall);

            int responseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + responseCode);

            String responseBody = post.getResponseBodyAsString();

            post.releaseConnection();
            LOG.info("response body: " + responseBody);
            assertEquals(ExpectedData, responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage());
        }

    }

    @Test
    public void TestWL_Write_MediumChunks() throws Exception {
        String testToCall = "TestWL_Write_MediumChunks";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int expectedResponseSize = 500000;
        String type = "write";
        try {
            int actualResponseSize = sendOneorMultiChunks_ExpectDataSizeInResponse(expectedResponseSize, type, "multi", testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);
        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + "exception is: " + e.toString());

        }
    }

    @Test
    public void TestWL_Write_LargeChunks() throws Exception {
        String testToCall = "TestWL_Write_LargeChunks";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int expectedResponseSize = 1000000;
        String type = "write";
        try {
            int actualResponseSize = sendOneorMultiChunks_ExpectDataSizeInResponse(expectedResponseSize, type, "multi", testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);
        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + "exception is: " + e.toString());

        }
    }

    @Test
    public void TestWL_Println_MediumChunks() throws Exception {
        String testToCall = "TestWL_Println_MediumChunks";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int expectedResponseSize = 500000;
        String type = "println";
        try {
            int actualResponseSize = sendOneorMultiChunks_ExpectDataSizeInResponse(expectedResponseSize, type, "multi", testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);
        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + "exception is: " + e.toString());

        }
    }

    @Test
    public void TestWL_Println_LargeChunks() throws Exception {
        String testToCall = "TestWL_Println_LargeChunks";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int expectedResponseSize = 1000000;
        String type = "println";
        try {
            int actualResponseSize = sendOneorMultiChunks_ExpectDataSizeInResponse(expectedResponseSize, type, "multi", testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);
        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + "exception is: " + e.toString());

        }
    }

    @Test
    public void Test_Return_onWritePossible() throws Exception {

        String testcase = "Test_Return_onWritePossible";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testcase + "*************");
        try {
            StringBuilder sb = new StringBuilder();
            int PostDataSize = 1000;
            char[] buffer = new char[1000];
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("TestToCall", testcase);
            con.setRequestProperty("ContentSizeSent", String.valueOf(PostDataSize));
            con.setDoOutput(true);
            con.setDoInput(true);
            con.connect();
            InputStream is = con.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            while (rd.read(buffer) != -1) {
                sb.append(buffer);
                sb.trimToSize();
            }
            con.disconnect();
            sb.trimToSize();
            LOG.info("Contents of sb is " + sb.toString());
            assertEquals(0, sb.toString().length());//compare with 0

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }
    }

    /*
     * Have onwritePossible in servlet throw an exception - a FFDC gets created with the thrown exception
     * Ignoring test for now.
     */
    @Test
    public void TestWL_IOE_AfterWrite() throws Exception {

        String testcase = "TestWL_IOE_AfterWrite";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testcase + "*************");
        try {
            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_URL);
            int PostDataSize = 1000;

            post.addRequestHeader("ContentSizeSent", String.valueOf(PostDataSize));
            post.setRequestHeader("TestToCall", testcase);

            int responseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + responseCode);

            final String ExpectedData = "ThrowExceptionAfterWrite onError method is called ! ThrowExceptionAfterWrite";
            String responseBody = post.getResponseBodyAsString();
            LOG.info("testExceptionAfterWriteOnWritePossible responseBody-->" + responseBody.toString());
            boolean data = responseBody.contains(ExpectedData);

            post.releaseConnection();

            assertTrue(data);

        } catch (Exception e) {
            e.printStackTrace();
            fail(testcase + " Exception from request: " + e.getMessage());
        }

    }

    /*
     * test to check if the onError method in the writeListenerImpl gets called.
     */
    @Test
    public void TestWL_onError() throws Exception {

        String testcase = "TestWL_onError";
        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testcase + "*************");
        try {

            final String ExpectedData = "TestonError onError method is called ! TestonError";

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_URL);
            post.setRequestHeader("TestToCall", testcase);

            int responseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + responseCode);

            String responseBody = post.getResponseBodyAsString();
            LOG.info("responseBody-->" + responseBody.toString());
            post.releaseConnection();
            assertEquals(ExpectedData, responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            fail(testcase + " Exception from request: " + e.getMessage());
        }
    }

    /*
     * register a writeListener from a filter
     * Test passes.
     */
    @Test
    public void TestWL_Write_Less_InFilter() throws Exception {

        String testToCall = "TestWL_Write_Less_InFilter";

        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");

        try {

            int PostDataSize = 1000;

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER__FILTER_SERVLET_URL);

            post.addRequestHeader("TestToCall", testToCall);
            post.addRequestHeader("ContentSizeSent", String.valueOf(PostDataSize));

            int responseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + responseCode);

            byte[] responseBody = post.getResponseBody();
            int returnedSize = responseBody.length;
            post.releaseConnection();
            assertEquals(PostDataSize, returnedSize);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }
    }

    @Test
    public void TestWriteFromFilter_AftersetWL() throws Exception {
        // Make sure the test framework knows that SRVE0918E is expected

        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        String testToCall = "TestWriteFromFilter_AftersetWL";

        LOG.info("\n [WebContainer | AsyncWriteListenerHttpUnit]: *************" + testToCall + "*************");
        int expectedResponseSize = 100000;
        try {
            int actualResponseSize = connectSendExpectDataSizeInResponse(expectedResponseSize, testToCall);
            assertEquals(expectedResponseSize, actualResponseSize);

            //Also check for this message in logs
//            String msg = "isReady always false from the thread which sets the WL";
//
//            String message = SHARED_SERVER.getLibertyServer().waitForStringInLogUsingMark(msg);
//            LOG.info(testToCall + " Entries found in log : " + message);
//            assertNotNull("Could not find message :: ", message);

            //Also check for exception in logs

            String message = server.waitForStringInLogUsingMark("SRVE0918E");
            LOG.info(testToCall + " Entries found in log : " + message);
            assertNotNull("Could not find message :: ", message);

        } catch (Exception e) {
            e.printStackTrace();
            fail(testToCall + " Exception from request: " + e.getMessage() + " ,exception is: " + e.toString());
        } finally {
            server.setMarkToEndOfLog();
        }
    }

    /**
     * @param ExpectdResponseSize
     * @return
     * @throws Exception
     *
     *                       This method will not take care of println
     */
    private int connectSendExpectDataSizeInResponse(int ExpectdResponseSize, String testtocall) throws Exception {

        String URLString = null;
        if (testtocall.equals("TestWriteFromFilter_AftersetWL")) {
            URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER__FILTER_SERVLET_URL;
        } else
            URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_URL;

        URL url = null;
        HttpURLConnection con = null;
        StringBuilder sb = new StringBuilder();

        LOG.info("\n Request URL : " + URLString);

        //char[] buffer = new char[1000];

        url = new URL(URLString);
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("TestToCall", testtocall);
        con.setRequestProperty("ContentSizeSent", Integer.toString(ExpectdResponseSize));
        con.setDoOutput(true);
        con.setDoInput(true);
        con.connect();

        LOG.info("Start reading the response.  Expected response size : " + ExpectdResponseSize);

        java.io.InputStream data = con.getInputStream();
        byte[] dataBytes = new byte[32768];
        int readLen = data.read(dataBytes);
        int total = 0;
        while (readLen != -1) {
            total += readLen;
            sb.append(new String(dataBytes, 0, readLen));
            readLen = data.read(dataBytes);
        }

        LOG.info(total + " bytes read for the resposne.");
        if (total != ExpectdResponseSize) {
            LOG.info("Response data : " + sb.toString());
        }

        con.disconnect();
        LOG.info("Actual response size : " + sb.toString().length());
        //LOG.info("Contents of sb is " + sb.toString());
        //assertEquals(PostDataSize, sb.toString().length());

        return total;

    }
//    private int connectSendExpectDataSizeInResponse(int expectedResponseSize, String testToCall) throws Exception {
//        String urlString;
//        if (testToCall.equals("TestWriteFromFilter_AftersetWL")) {
//            urlString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER__FILTER_SERVLET_URL;
//        } else {
//            urlString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_URL;
//        }
//
//        URL url = new URL(urlString);
//        HttpURLConnection con = (HttpURLConnection) url.openConnection();
//        con.setRequestMethod("POST");
//        con.setRequestProperty("TestToCall", testToCall);
//        con.setRequestProperty("ContentSizeSent", Integer.toString(expectedResponseSize));
//        con.setDoOutput(true);
//        con.setDoInput(true);
//
//        LOG.info("\nRequest URL: " + urlString);
//
//        try {
//            LOG.info("Start reading the response. Expected response size: " + expectedResponseSize);
//
//            int responseCode = con.getResponseCode();
//            LOG.info("HTTP Response Code: " + responseCode);
//            if (responseCode != HttpURLConnection.HTTP_OK) {
//                LOG.info("HTTP Response Code: " + responseCode);
//                return 0; // Handle the error as needed
//            }
//
//            int total = 0;
//            StringBuilder sb = new StringBuilder();
//            byte[] dataBytes = new byte[8192];
//            int readLen;
//
//            try (InputStream data = con.getInputStream()) {
//                while ((readLen = data.read(dataBytes)) != -1) {
//                    total += readLen;
//                    sb.append(new String(dataBytes, 0, readLen));
//                }
//            }
//
//            LOG.info(total + " bytes read for the response.");
//            if (total != expectedResponseSize) {
//                LOG.info("Response data: " + sb.toString());
//            }
//
//            LOG.info("Actual response size: " + sb.length());
//
//            return total;
//        } catch (IOException e) {
//            LOG.info("Error reading response: " + e.getMessage());
//
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            e.printStackTrace(pw);
//            LOG.info("Stack:\n" + sw.toString());
//
//            e.printStackTrace();
//            return 0; // Handle the error as needed
//        } finally {
//            con.disconnect();
//        }
//    }

    /**
     * @param ExpectdResponseSize
     * @return
     * @throws Exception
     */
    private int sendOneorMultiChunks_ExpectDataSizeInResponse(int ExpectdResponseSize, String type, String noOfChunks, String testName) throws Exception {

        String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + WRITE_LISTENER_SERVLET_URL;
        URL url = null;
        HttpURLConnection con = null;

        LOG.info("Request URL : " + URLString);

        url = new URL(URLString);
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");

        if (noOfChunks.equalsIgnoreCase("multi")) {
//            if (testName.equalsIgnoreCase("Write_MediumDataChunks")) {
//                con.setRequestProperty("TestToCall", "Write_MediumDataChunks");
//            }
//            else if (testName.equalsIgnoreCase("Println_MediumDataChunks")) {
//                con.setRequestProperty("TestToCall", "Println_MediumDataChunks");
//            }
//            else if (testName.equalsIgnoreCase("Write_LargeDataChunks")) {
//                con.setRequestProperty("TestToCall", "Write_LargeDataChunks");
//            }
//            else if (testName.equalsIgnoreCase("Println_LargeDataChunks")) {
//                con.setRequestProperty("TestToCall", "Println_LargeDataChunks");
//            }
//            else
            con.setRequestProperty("TestToCall", testName);
        } else if (noOfChunks.equalsIgnoreCase("one")) {
//            if (testName.equalsIgnoreCase("TestWL_Println_Large")) {
//                con.setRequestProperty("TestToCall", testName);
//            }
//            else if (testName.equalsIgnoreCase("WriteLargeData")) {
//                con.setRequestProperty("TestToCall", "WriteLargeData");
//            }
//            else if (testName.equalsIgnoreCase("TestWL_Write_Medium")) {
//                con.setRequestProperty("TestToCall", "TestWL_Write_Medium");
//            }
//            else
            con.setRequestProperty("TestToCall", testName);
        }
        con.setRequestProperty("ContentSizeSent", Integer.toString(ExpectdResponseSize));
        if (type != null)
            con.setRequestProperty("Type", type);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.connect();

        return checkDataSizeinResponse(con, ExpectdResponseSize);

    }

    /**
     * @param con
     * @param ExpectdResponseSize
     * @return
     * @throws Exception
     */
    private int checkDataSizeinResponse(HttpURLConnection con, int ExpectdResponseSize) throws Exception {

        LOG.info("Start reading the response.  Expected response size : " + ExpectdResponseSize);
//        String line = "";
//        java.io.InputStream data = con.getInputStream();
//
//        BufferedReader rd = new BufferedReader(new InputStreamReader(data));
//        StringBuilder sb = new StringBuilder();
//        while ((line = rd.readLine()) != null) {
//            sb.append(line);
//        }
//        int total = sb.toString().length();
//
//        LOG.info(total + " bytes read for the resposne.");
//        if (total != ExpectdResponseSize) {
//            LOG.info("Response data : " + sb.toString());
//        }
//        con.disconnect();
//
//        //assertEquals(ExpectdResponseSize, total);
//
//        LOG.info("End reading the response.  Expected response size : " + ExpectdResponseSize);
//        return total;

        Objects.requireNonNull(con, "HttpURLConnection cannot be null");
        boolean testPassed = Boolean.FALSE;
        int bytesRead = 0;

        try {
            // Ensure we have a successful response
            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOG.info("HTTP Response Code: " + responseCode);
                //TODO: fail
            }
            LOG.info("HTTP Response Code: " + responseCode);

            // Get the input stream to read the response

            try (InputStream inputStream = con.getInputStream()) {
                byte[] buffer = new byte[1024];

                int bytesReadThisChunk;;
                //Read and discard the response data while couting bytes
                while ((bytesReadThisChunk = inputStream.read(buffer)) != -1) {
                    LOG.info("Reading chunk: " + bytesReadThisChunk);
                    for (int i = 0; i < bytesReadThisChunk; i++) {
                        byte currentByte = buffer[i];
                        if (currentByte != '\r' && currentByte != '\n') {
                            bytesRead++;
                        }
                    }
                }

                // Compare total bytes read with expected size

                LOG.info("Total bytes read: " + bytesRead);

                testPassed = (bytesRead == ExpectdResponseSize);
                if (testPassed) {
                    LOG.info("Received " + bytesRead + " bytes as expected.");
                } else {
                    LOG.info("Received " + bytesRead + " bytes, but expected " + ExpectdResponseSize + " bytes.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            con.disconnect();
        }

        return bytesRead;

    }
}
