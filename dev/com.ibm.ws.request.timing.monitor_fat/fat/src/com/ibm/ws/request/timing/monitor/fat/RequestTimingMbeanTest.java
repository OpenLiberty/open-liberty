/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.monitor.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.request.timing.RequestTimingStatsMXBean;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.request.timing.app.RequestTimingServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

/**
 * This FAT bucket tests the Requesttiming Stats mxbean for requests sent to a
 * servlet. Requests are checked to ensure that metrics such as active request
 * count, slow request count, hung request count, and total request count are
 * counted correctly
 *
 *
 * Notes: - At each servlet request a global variable for total requests is
 * incremented. The total value is checked after each test for consistency.
 * Since the counts are checked from the servlet there will always be 1 active
 * request during the mbean call which happens inside the initial servlet call.
 */
@RunWith(FATRunner.class)
@SkipForRepeat({ "MPM23", "MPM22", "MPM20" })
public class RequestTimingMbeanTest {

    private static final Class<RequestTimingMbeanTest> c = RequestTimingMbeanTest.class;

    // private static JMXConnector jmxConnector;
    static RequestTimingStatsMXBean mbean;
    static MBeanServer mbeanConn;

    @Server("RequestTimingFeatureServer")
    @TestServlet(servlet = RequestTimingServlet.class, path = "RequestTimingWebApp")
    public static LibertyServer server;
    static ObjectName ServletInstanceName;

    // Keeping a AtomicLong total count because of threads created
    public static AtomicLong totalRequestCount = new AtomicLong();
    public static long mbeanServletActiveCount = 1;

    public final String TestRequestHandlerUrl = getURLString("TestRequestHandler", 0);

    private static long timeout = 30 * 1000;

    /**
     * JUnit guarantees that this gets run after the static set up in the superclass
     * (as long as the names are different).
     */
    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "RequestTimingWebApp", "com.ibm.ws.request.timing.app");

        JavaInfo java = JavaInfo.forCurrentVM();
        int javaMajorVersion = java.majorVersion();
        if (javaMajorVersion != 8) {
            Log.info(c, "setUp", " Java version = " + javaMajorVersion + " - It is higher than 8, adding --add-exports...");
            server.copyFileToLibertyServerRoot("add-exports/jvm.options");
        }

        server.startServer();
    }

    /**
     * Closing the server after all tests have been executed. Adding the expected
     * TRAS011 message for the hung and slow javacore/warning messages in the logs
     *
     * @throws Exception
     */
    @AfterClass
    public static void classTearDown() throws Exception {
        server.stopServer(true, "TRAS011.*");
    }

    /**
     * Check the total count before and after each test to verify counts are
     * consistent through out the tests
     */
    @After
    public void checkTotalCount() throws Exception {
        Log.info(c, "checkTotalCount", "Entering");
        long newReceivedCount = mbeanCall("RequestCount");
        assertTrue("Incorrect total requests to server found. Expected: " + totalRequestCount.get() + " Found: "
                   + newReceivedCount, newReceivedCount == totalRequestCount.get());
        Log.info(c, "checkTotalCount", "Exiting");
    }

    /**
     * Tests if mbean is getting removed when both features are disabled and
     * registered correctly when both features are enabled.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMbeanRegistration() throws Exception {

        final String METHOD_NAME = "testMbeanRegistration";
        Log.info(c, METHOD_NAME, "Entry");
        try {
            updateAndValidateConfigFile("disableBothFeatures.xml");
            assertNotNull("requestTiming-1.0 not removed from the featureList",
                          server.waitForStringInLogUsingMark("CWWKF0013I.*requestTiming-1.0", timeout));
            assertNotNull("monitor-1.0 not removed from the featureList",
                          server.waitForStringInLogUsingMark("CWWKF0013I.*monitor-1.0", timeout));
            assertFalse("Mbean is still registered after the requestTiming features are removed", isMbeanRegistered());

            updateAndValidateConfigFile("enableBothFeatures.xml");
            totalRequestCount.set(0);
            assertNotNull("requestTiming-1.0 not added on the featureList",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*requestTiming-1.0", timeout));
            assertNotNull("monitor-1.0 not added on the featureList",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*monitor-1.0", timeout));
            assertTrue("Mbean is not registered after the requestTiming features are installed", isMbeanRegistered());

            // Expect all counts to be zero except total and active due to the servlet calls
            long receivedCount = mbeanCall("RequestCount");
            assertTrue("Incorrect total requests to server found. Expected: " + totalRequestCount.get() + " Found: "
                       + receivedCount, receivedCount == totalRequestCount.get());
            receivedCount = mbeanCall("ActiveRequestCount");
            assertTrue("Incorrect total requests to server found. Expected: " + mbeanServletActiveCount + " Found: "
                       + receivedCount, receivedCount == mbeanServletActiveCount);
            receivedCount = mbeanCall("SlowRequestCount");
            assertTrue("Incorrect total requests to server found. Expected: " + 0 + " Found: " + receivedCount,
                       receivedCount == 0);
            receivedCount = mbeanCall("HungRequestCount");
            assertTrue("Incorrect total requests to server found. Expected: " + 0 + " Found: " + receivedCount,
                       receivedCount == 0);
        } finally {
            updateAndValidateConfigFile("enableBothFeatures.xml");
            Log.info(c, METHOD_NAME, "Exit");
        }

    }

    /**
     * Tests if mbean is getting removed when one of the features are disabled. If
     * the mbean is not registered, the total count has to reset to 0. Finally this
     * test sets back the default server.xml for other tests.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMbeanRegistrationWithMonitorOnlyEnabled() throws Exception {

        final String METHOD_NAME = "testMbeanRegistrationWithMonitorOnlyEnabled";
        Log.info(c, METHOD_NAME, "Entry");
        try {
            updateAndValidateConfigFile("enableMonitor-1.0.xml");
            assertNotNull("requestTiming-1.0 not removed from the featureList",
                          server.waitForStringInLogUsingMark("CWWKF0013I.*requestTiming-1.0", timeout));
            assertFalse("Mbean is still registered after the requestTiming-1.0 feature is removed", isMbeanRegistered());
            totalRequestCount.set(0);

        } finally {
            updateAndValidateConfigFile("enableBothFeatures.xml");
            Log.info(c, METHOD_NAME, "Exit");
        }

    }

    /**
     * Tests if mbean is getting removed when one of the features are disabled.
     * Finally this test sets back the default server.xml for other tests.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMbeanRegistrationWithRequestTimingOnlyEnabled() throws Exception {

        final String METHOD_NAME = "testMbeanRegistrationWithRequestTimingOnlyEnabled";
        Log.info(c, METHOD_NAME, "Entry");
        try {
            updateAndValidateConfigFile("enableRequestTiming-1.0.xml");
            assertNotNull("monitor-1.0 not removed from the featureList",
                          server.waitForStringInLogUsingMark("CWWKF0013I.*monitor-1.0", timeout));
            assertFalse("Mbean is still registered after the monitor-1.0 feature is removed", isMbeanRegistered());
        } finally {
            updateAndValidateConfigFile("enableBothFeatures.xml");
            Log.info(c, METHOD_NAME, "Exit");
        }

    }

    /**
     * Tests total servlet request metric by sending N requests to servlet, then
     * check if total requests is equal to N. Then, send 2 more requests and check
     * if expected total requests is N+2
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testTotalServletRequests() throws Exception {

        final String METHOD_NAME = "testTotalServletRequests";
        Log.info(c, METHOD_NAME, "Entry");
        long receivedCount = 0;
        int totalNumberofRequests = 5;
        try {
            String response = null;

            for (int i = 0; i < totalNumberofRequests; i++) {
                response = getHttpResponse(TestRequestHandlerUrl);
                Log.info(c, METHOD_NAME, response);
            }

            receivedCount = mbeanCall("RequestCount");
            assertTrue("Incorrect total requests to server found. Expected: " + totalRequestCount.get() + " Found: "
                       + receivedCount, receivedCount == totalRequestCount.get());

            // Send 2 more servlet requests to server
            for (int i = 0; i < 2; i++) {
                response = getHttpResponse(TestRequestHandlerUrl);
                Log.info(c, METHOD_NAME, response);
            }

            // Check the total count
            receivedCount = mbeanCall("RequestCount");
            assertTrue("Incorrect total requests to server found. Expected: " + totalRequestCount.get() + " Found: "
                       + receivedCount, receivedCount == totalRequestCount.get());

        } finally {
            waitInServletToUnblockThreads();
            Log.info(c, METHOD_NAME, "Exit");

        }
    }

    /**
     * This test sets a CountDownLatch in the servlet where the requests will hang.
     * Then, sends multiple requests to the servlet and invokes the mbean servlet
     * call. It will make sure that the number of requests sent matches the Active
     * Request count.
     *
     * Then, it will send one more request, which will break the CountDownLatch, and
     * invoke the 'display,work' command. Check to see if the active requests
     * decrements correctly to 0 after all the requests complete.
     *
     * Finally, it will unblock any threads waiting in the servlet.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testActiveRequestsToServer() throws Exception {

        final String METHOD_NAME = "testActiveRequestsToServer";
        Log.info(c, METHOD_NAME, "Entry");
        int NumofActiveRequests = 5;

        try {
            setCountDownLatchInServlet(NumofActiveRequests);

            // Holds threads that will be asynchronously doing requests on servlet
            // build request sets of threads
            Thread th[] = new Thread[NumofActiveRequests];
            createRequestThreads(th, NumofActiveRequests);

            // Starts N-1 threads that send request to servlet
            for (int i = 0; i < NumofActiveRequests - 1; i++) {
                th[i].start();
            }

            // Wait method that delays for up to a minute while threads start and send
            // requests
            waitInServletForCountDownLatch(1);

            // Checking for correct number of active servlet requests which is initRequest plus the active request from mbean
            long receivedActiveRequests = mbeanCall("ActiveRequestCount");
            assertTrue(
                       "Incorrect active requests to server found. Expected: "
                       + (NumofActiveRequests - 1 + mbeanServletActiveCount) + " Found: " + receivedActiveRequests,
                       receivedActiveRequests == (NumofActiveRequests - 1 + mbeanServletActiveCount));

            // Start last thread to break CountDownLatch in servlet
            th[NumofActiveRequests - 1].start();

            // Need to sleep so last thread can break CountDownLatch and active threads that
            // are waiting can complete
            waitInServletForCountDownLatch(0);

            // Need to wait for threads to complete and exit before checking for active requests
            for (Thread thread : th) {
                thread.join(180000);
            }

            // Should see 1 active requests for mbean otherwise assert fails
            receivedActiveRequests = mbeanCall("ActiveRequestCount");
            assertTrue("Incorrect active requests to server found. Expected: " + mbeanServletActiveCount + " Found: "
                       + receivedActiveRequests, receivedActiveRequests == mbeanServletActiveCount);

        } finally {
            waitInServletToUnblockThreads();
            Log.info(c, METHOD_NAME, "Exit");
        }
    }

    /**
     * This test updates server.xml with config from serverSlowRequest.xml. Next, it
     * sets a CountDownLatch in the servlet where the requests will hang. Then, it
     * sends multiple requests to the servlet, sleeps for 5s more than the
     * slowRequestThreshold specified in server.xml, and calls the mbean servlet
     * call. It checks to make sure that the number of requests sent both the slow
     * and active totals. This is because a request that is slow still shows up as
     * active until it completes.
     *
     * Then, it will send one more request, which will break the CountDownLatch, and
     * wait 10s before invoking the mbean servlet call. There is a delay from when a
     * request completes and when the slow count updates in the mbean for the
     * command handler so we need to wait 10s to get the correct slow value. Check
     * to see if the slow requests decremented correctly back to 0 after all the
     * requests complete.
     *
     * Finally, it will unblock any threads waiting in the servlet.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSlowServletRequest() throws Exception {

        final String METHOD_NAME = "testSlowServletRequest";
        Log.info(c, METHOD_NAME, "Entry");
        int NumofSlowRequests = 5;
        try {
            updateAndValidateConfigFile("serverSlowRequest.xml");
            //CWWKG0017I: The server configuration was successfully updated in __ seconds.
            server.waitForStringInLogUsingMark("CWWKG0017I", 30000);

            // Set servlet countdownlatch for active request test
            setCountDownLatchInServlet(NumofSlowRequests);

            // Holds threads that will be asynchronously doing requests on servlet
            // build request sets of threads
            Thread th[] = new Thread[NumofSlowRequests];
            createRequestThreads(th, NumofSlowRequests);

            // Starts N-1 threads that send request to servlet
            for (int i = 0; i < NumofSlowRequests - 1; i++) {
                th[i].start();
            }

            // Wait method that delays for up to a minute while threads start and send
            // requests
            waitInServletForCountDownLatch(1);

            // Issue mbean call and check for correct active request message
            long receivedActiveRequests = mbeanCall("ActiveRequestCount");
            assertTrue(
                       "Incorrect active requests to server found. Expected: "
                       + (NumofSlowRequests - 1 + mbeanServletActiveCount)
                       + " Found: " + receivedActiveRequests,
                       receivedActiveRequests == (NumofSlowRequests - 1 + mbeanServletActiveCount));

            // Sleep 15 seconds so threads in servlet go from active to active and slow
            Thread.sleep(15000);

            // Issue mbean call and check for correct slow servlet request message
            long receivedSlowRequests = mbeanCall("SlowRequestCount");
            assertTrue("Incorrect slow requests to server found. Expected: " + (NumofSlowRequests - 1) + " Found: "
                       + receivedSlowRequests, receivedSlowRequests == NumofSlowRequests - 1);

            // Start last thread to release CountDownLatch
            th[NumofSlowRequests - 1].start();

            // Need to sleep so last thread can break CountDownLatch and active threads that
            // are waiting can complete
            waitInServletForCountDownLatch(0);

            // Slow request count does not decrement as fast as active so wait for a 10 more
            // seconds while slow count updates
            Thread.sleep(10000);

            receivedSlowRequests = mbeanCall("SlowRequestCount");
            assertTrue("Incorrect slow requests to server found. Expected: " + 0 + " Found: " + receivedSlowRequests,
                       receivedSlowRequests == 0);

            // Should see 1 active requests for mbean otherwise assert fails
            receivedActiveRequests = mbeanCall("ActiveRequestCount");
            assertTrue("Incorrect active requests to server found. Expected: " + mbeanServletActiveCount + " Found: "
                       + receivedActiveRequests, receivedActiveRequests == mbeanServletActiveCount);

        } finally {
            waitInServletToUnblockThreads();
            Log.info(c, METHOD_NAME, "Exit");

        }
    }

    /**
     * First, it updates server.xml with config from serverHungRequest.xml. Next, it
     * sets a CountDownLatch in the servlet where the requests will hang. Then,
     * sends multiple requests to the servlet, sleep for longer than the
     * hungRequestThreshold, and invokes the mbean servlet call. It checks to make
     * sure that the number of requests sent both the hung and active totals. This
     * is because a request that is hung still shows up as active until it
     * completes.
     *
     * Then, it will send one more request, which will break the CountDownLatch, and
     * wait 10s before invoking the mbean servlet call. There is a delay from when a
     * request completes and when the hung count updates in the mbean for the
     * command handler so we need to wait 10s to get the correct hung value. Check
     * to see if the hung requests decremented correctly back to 0 after all the
     * requests complete.
     *
     * Finally, it will unblock any threads waiting in the servlet.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testHungServletRequest() throws Exception {

        final String METHOD_NAME = "testHungServletRequest";
        Log.info(c, METHOD_NAME, "Entry");
        int NumofHungRequests = 5;

        try {
            updateAndValidateConfigFile("serverHungRequest.xml");

            // Set servlet countdownlatch for active request test
            setCountDownLatchInServlet(NumofHungRequests);

            // Holds threads that will be asynchronously doing requests on servlet
            // build request sets of threads
            Thread th[] = new Thread[NumofHungRequests];
            createRequestThreads(th, NumofHungRequests);

            // Starts N-1 threads that send request to servlet
            for (int i = 0; i < NumofHungRequests - 1; i++) {
                th[i].start();
            }

            waitInServletForCountDownLatch(1);

            // Issue mbean call and check for correct active request message
            long receivedActiveRequests = mbeanCall("ActiveRequestCount");
            assertTrue(
                       "Incorrect active requests to server found. Expected: "
                       + (NumofHungRequests - 1 + mbeanServletActiveCount)
                       + " Found: " + receivedActiveRequests,
                       receivedActiveRequests == (NumofHungRequests - 1 + mbeanServletActiveCount));

            // Sleep 20 seconds to get requests into hung. This will print a Java Dump to
            // screen after the hungRequestThreshold is up
            Thread.sleep(20000);

            // Issue mbean call and check for correct slow servlet request message
            long receivedHungRequests = mbeanCall("HungRequestCount");
            assertTrue("Incorrect hung requests to server found. Expected: " + (NumofHungRequests - 1) + " Found: "
                       + receivedHungRequests, receivedHungRequests == (NumofHungRequests - 1));

            // Start last thread to release CountDownLatch
            th[NumofHungRequests - 1].start();

            // Need to sleep so last thread can break CountDownLatch and active threads that
            // are waiting can complete
            waitInServletForCountDownLatch(0);

            // Sleep while counts update because hung count updates slower than active count
            Thread.sleep(10000);
            receivedHungRequests = mbeanCall("HungRequestCount");
            assertTrue("Incorrect hung requests to server found. Expected: " + 0 + " Found: " + receivedHungRequests,
                       receivedHungRequests == 0);

            // Should see no active requests otherwise assert fails
            receivedActiveRequests = mbeanCall("ActiveRequestCount");
            assertTrue("Incorrect active requests to server found. Expected: " + mbeanServletActiveCount + " Found: "
                       + receivedActiveRequests, receivedActiveRequests == mbeanServletActiveCount);

        } finally {
            waitInServletToUnblockThreads();
            Log.info(c, METHOD_NAME, "Exit");
        }
    }

    /*************************************************************
     * HELPER METHODS
     ************************************************************/

    /**
     * Sends a request to the servlet to set the CountDownLatch. CountDownLatch is
     * used to wait requests in servlet to simulate behavior of active, slow, and
     * hung requests
     *
     * @param numRequests
     * @throws Exception
     */
    private void setCountDownLatchInServlet(int numbrequests) throws Exception {
        final String METHOD_NAME = "setCountDownLatchInServlet";
        String setCountDownLatchUrl = getURLString("setCountDownLatch", numbrequests);
        String response = getHttpResponse(setCountDownLatchUrl);
        Log.info(c, METHOD_NAME, response);
    }

    /**
     * Sends a request to waitForCountDownLatch function in servlet. It will wait in
     * servlet until correct value is found and then this request will complete and
     * allow a test to finish
     *
     * @param countToWaitFor
     *                           Value looked for in CountDownLatch
     * @throws Exception
     */
    private void waitInServletForCountDownLatch(int countToWaitFor) throws Exception {
        final String METHOD_NAME = "waitInServletForCountDownLatch";
        String waitForCountDownLatch = null;
        if (countToWaitFor == 0) {
            waitForCountDownLatch = getURLString("waitForCountDownLatch", 0);
        } else {
            waitForCountDownLatch = getURLString("waitForCountDownLatch", 1);
        }
        String response = getHttpResponse(waitForCountDownLatch);
        Log.info(c, METHOD_NAME, response);
    }

    /**
     * Sends a request to waitForCountDownLatch function in servlet. It will wait in
     * servlet until correct value is found and then this request will complete and
     * allow a test to finish
     *
     * @throws Exception
     */
    private void waitInServletToUnblockThreads() throws Exception {
        final String METHOD_NAME = "waitInServletToUnblockThreads";
        String unblockThreadsUrl = getURLString("unblockThreads", 0);
        String response = getHttpResponse(unblockThreadsUrl);
        Log.info(c, METHOD_NAME, response);
    }

    /** URL helper */
    private String getURLString(String testName, int variable) {
        return new String("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                          + "/RequestTimingWebApp/RequestTimingServlet?testName=" + testName + "&value=" + variable);
    }

    /** URL helper */
    private String getURLString(String testName, String variable) {
        return new String("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                          + "/RequestTimingWebApp/RequestTimingServlet?testName=" + testName + "&value=" + variable);
    }

    /**
     * Creates HTTP Client and invoke the application uri to get the response
     *
     * @return String response of the application
     */

    private String getHttpResponse(String url) throws IOException {
        final String METHOD_NAME = "getHttpResponse";
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            Log.info(c, METHOD_NAME, "Invoking HTTP request using url: " + url);
            response = httpclient.execute(httpget);
            String returned_response = EntityUtils.toString(response.getEntity()).trim();
            if (returned_response.contains("Context Root Not Found")) {
                throw new Exception("Context Root was not found");
            }
            totalRequestCount.incrementAndGet();
            return returned_response;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            response.close();
            httpclient.close();
        }
    }

    /**
     * Method creates threads that send request to servlet for specified servlet
     * test
     *
     * @param th
     *                            -- array of threads
     * @param numReqs
     *                            -- number of requests is the number of threads needed
     * @param servletTestName
     *                            -- Used for request to servlet
     * @param testMethodName
     *                            -- Used for printing to logs
     */
    private void createRequestThreads(Thread[] th, int numReqs) {
        // Send N servlet requests to server, last request used to terminate
        // CountDownLatch
        final String METHOD_NAME = "createRequestThreads";
        for (int i = 0; i < numReqs; i++) {
            th[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        // Sleep 2 seconds, then send request to servlet and print response to log
                        Thread.sleep(2000);
                        String response = getHttpResponse(TestRequestHandlerUrl);
                        Log.info(c, METHOD_NAME, response);
                    } catch (Exception ex) {
                        Log.info(c, METHOD_NAME, "Exception found: " + ex.getMessage());
                    }
                }
            };
        }
    }

    /**
     * Make a call to the mbean method and returns the value accordingly.
     *
     * @param methodName
     * @return
     */
    private long mbeanCall(String methodName) throws Exception {
        final String METHOD_NAME = "mbeanCall";
        Log.info(c, METHOD_NAME, "Checking the " + methodName + " from the mbean call");
        long count = 0;
        String response = null;
        try {
            response = getHttpResponse(getURLString("getCount", methodName));
            count = Long.parseLong(response);
        } catch (Exception ex) {
            fail("mbeanCall(): " + "Not a valid response. Received: " + response);
        }
        return count;
    }

    /**
     * This method sets the config file and waits for the update message from logs
     */
    private void updateAndValidateConfigFile(String File) throws Exception {
        final String METHOD_NAME = "updateAndValidateConfigFile";
        Log.info(c, METHOD_NAME, "Updating the server.xml to " + File);
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(File);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("RequestTimingWebApp"), "");
    }

    /**
     * This helper method returns if the mbean is registered or not
     *
     * @return true or false if mbean registered
     */
    private boolean isMbeanRegistered() throws Exception {
        boolean result = false;
        String response = getHttpResponse(getURLString("TestRegistration", 0));
        if (response.equalsIgnoreCase("true") || response.equalsIgnoreCase("false")) {
            result = Boolean.valueOf(response);
        } else {
            throw new Exception("Not a valid response from registration servlet call. Found: " + response);
        }
        return result;
    }
}
