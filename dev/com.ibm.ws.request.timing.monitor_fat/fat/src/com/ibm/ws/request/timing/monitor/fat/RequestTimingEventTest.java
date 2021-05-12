/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.monitor.fat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
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

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

/**
 * This FAT bucket tests the websphere.session.*, websphere.datasource.* events
 * that can possibly disrupt the request counts. Same tests from
 * RequestTimingMbeanTest are added to include these event feature and invokes
 * the servlet for specific methods that drive them.
 *
 * Background: The request timing support is intrinsically dependent on the
 * probe framework. The probe framework is a generic mechanism that allows
 * components to register specific methods/events (along the execution path)
 * with it such that those components are called by the probe framework prior
 * and post (registered) method execution. The probe framework calls components
 * through events. Therefore, a registered method is associated to an event and
 * event type. The probe framework is synchronous in nature and treats events
 * (method calls) along the request execution path as parent(root)/children
 * events. The request timing support uses the root/parent event notion to count
 * active, slow, hung, etc requests only once. Each request is counted/tracked
 * per root event type. This means that there can only be one root event type
 * per request type if the counts are to be accurate. For example, the
 * expectation is that request timing tracks all servlet requests under a single
 * root event type called websphere.servlet.service. In this particular case,
 * there is is a method in the servlet request execution path called *service()
 * that was registered with the probe framework and was associated event type
 * websphere.servlet.service. It is this event type that is supposed to
 * represent all servlet requests. Given that the probe support is a generic
 * event framework, the root event and associated event type can change. More
 * precisely, if there is another component that registered method
 * x()/event-type-x with the probe framework, and method x() is along the
 * servlet execution path, which is called before the method associated with
 * event websphere.servlet.service, the request timing counts would now be
 * tracked under method x()'s event type. This exposes a flaw in the way request
 * timing keeps track of request counts. For instance, If there were to be
 * dynamic configuration updates that enabled components that changed what is
 * considered to be the root event, the counts for a request type would be split
 * among different root events. One known case where this takes place is when
 * zosRequestLogging-1.0 is also enabled. The enablement of this feature causes
 * the root event type or servlet request to change from
 * websphere.servlet.service to websphere.http.wrapperHandleAndExecute. The
 * agreed temporary solution is to simply allow the notion that request types
 * can have more than one root event type. This means that when retrieving
 * statistic counts for servlet requests, callers must manually track and add
 * the counts kept under the two potential and currently know root events:
 * websphere.servlet.service and websphere.http.wrapperHandleAndExecute.
 *
 * Notes: - At each servlet request a global variable for total requests is
 * incremented. The total value is checked after each test for consistency.
 * Since the counts are checked from the servlet there will always be 1 active
 * request during the mbean call which happens inside the initial servlet call.
 */
@RunWith(FATRunner.class)
@SkipForRepeat({ "MPM23", "MPM22", "MPM20" })
public class RequestTimingEventTest {

    private static final Class<RequestTimingEventTest> c = RequestTimingEventTest.class;

    // private static JMXConnector jmxConnector;
    static RequestTimingStatsMXBean mbean;
    static MBeanServer mbeanConn;

    @Server("RequestTimingFeatureWithEventFeature")
    @TestServlet(servlet = RequestTimingServlet.class, path = "RequestTimingWebApp")
    public static LibertyServer server;
    static ObjectName ServletInstanceName;

    // Keeping a AtomicLong total count because of threads created
    public static AtomicLong totalRequestCount = new AtomicLong();
    public static long mbeanServletActiveCount = 1;

    public final String TestRequestHandlerUrl = getURLString("TestRequestHandler", 0);

    /**
     * JUnit guarantees that this gets run after the static set up in the superclass
     * (as long as the names are different).
     */
    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, "RequestTimingWebApp", "com.ibm.ws.request.timing.app");

        JavaInfo java = JavaInfo.forCurrentVM();
        int javaMajorVersion = java.majorVersion();
        if (javaMajorVersion != 8) {
            Log.info(c, "setUp", " Java version = " + javaMajorVersion + " - It is higher than 8, adding --add-exports...");
            server.copyFileToLibertyServerRoot("add-exports/jvm.options");
        }
        server.startServer();
        setupTables();
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
     * Tests total servlet request metric by sending N requests to servlet, then
     * check if total requests is equal to N. Then, send 2 more requests and check
     * if expected total requests is N+2
     *
     * @throws Exception
     */
    @ExpectedFFDC("java.lang.ClassCastException")
    @Mode(TestMode.LITE)
    @Test
    public void testTotalServletRequestsWithEventTiming() throws Exception {

        final String METHOD_NAME = "testTotalServletRequestsWithEventTiming";
        Log.info(c, METHOD_NAME, "Entry");
        long receivedCount = 0;
        try {
            String response = null;

            // 1st Request
            response = getHttpResponse(TestRequestHandlerUrl);
            Log.info(c, METHOD_NAME, response);
            receivedCount = mbeanCall("RequestCount");
            assertTrue("Incorrect total requests to server found. Expected: " + totalRequestCount.get() + " Found: "
                       + receivedCount, receivedCount == totalRequestCount.get());

            // Normal Request
            response = getHttpResponse(TestRequestHandlerUrl);
            Log.info(c, METHOD_NAME, response);
            receivedCount = mbeanCall("RequestCount");
            assertTrue("Incorrect total requests to server found. Expected: " + totalRequestCount.get() + " Found: "
                       + receivedCount, receivedCount == totalRequestCount.get());

            // Test session
            testSessionMethod();
            // Check the total count
            receivedCount = mbeanCall("RequestCount");
            assertTrue("Incorrect total requests to server found. Expected: " + totalRequestCount.get() + " Found: "
                       + receivedCount, receivedCount == totalRequestCount.get());

            // Test JDBQuery test
            testJDBCQuery();
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
    @ExpectedFFDC("java.lang.ClassCastException")
    @Mode(TestMode.LITE)
    @Test
    public void testSlowServletRequestWithEventTiming() throws Exception {

        final String METHOD_NAME = "testSlowServletRequestWithEventTiming";
        Log.info(c, METHOD_NAME, "Entry");

        int NumofServletRequests = 5;
        int TotalNumofRequests = 2 * NumofServletRequests;

        try {

            // Set servlet countdownlatch for active request test
            setCountDownLatchInServlet(TotalNumofRequests);

            // Create sessionRequest and JDBCRequest threads
            Thread th_session[] = new Thread[NumofServletRequests];
            Thread th_jdbc[] = new Thread[NumofServletRequests];
            createRequestThreads(th_session, NumofServletRequests, "testSessionRequest");
            createRequestThreads(th_jdbc, NumofServletRequests, "testBasicQueryToSingle");

            // Starts N-1 threads that send request to servlet
            for (int i = 0; i < NumofServletRequests - 1; i++) {
                th_session[i].start();
                th_jdbc[i].start();
            }

            // Wait method that delays for up to a minute while threads start and send
            // requests
            waitInServletForCountDownLatch(2);

            // Issue mbean call and check for correct active request message
            long receivedActiveRequests = mbeanCall("ActiveRequestCount");
            assertTrue(
                       "Incorrect active requests to server found. Expected: "
                       + (TotalNumofRequests - 2 + mbeanServletActiveCount) + " Found: " + receivedActiveRequests,
                       receivedActiveRequests == (TotalNumofRequests - 2 + mbeanServletActiveCount));

            // Sleep 20 seconds so threads in servlet go from active to active and slow
            Thread.sleep(20000);

            // Issue mbean call and check for correct slow servlet request message
            long receivedSlowRequests = mbeanCall("SlowRequestCount");
            assertTrue("Incorrect slow requests to server found. Expected: " + (TotalNumofRequests - 2) + " Found: "
                       + receivedSlowRequests, receivedSlowRequests == TotalNumofRequests - 2);

            // Start last thread to release CountDownLatch
            th_session[NumofServletRequests - 1].start();
            th_jdbc[NumofServletRequests - 1].start();

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
     * First, it updates server.xml with config from requestTimingEventTest.xml.
     * Next, it sets a CountDownLatch in the servlet where the requests will hang.
     * Then, sends multiple requests to the servlet, sleep for longer than the
     * hungRequestThreshold, and invokes the mbean servlet call. It checks to make
     * sure that the number of requests sent both the hung and active totals. This
     * is because a request that is hung still shows up as active until it
     * completes.
     *
     * Finally, it will unblock any threads waiting in the servlet.
     *
     * @throws Exception
     */
    @ExpectedFFDC("java.lang.ClassCastException")
    @Mode(TestMode.LITE)
    @Test
    public void testHungServletRequestWithEventTiming() throws Exception {

        final String METHOD_NAME = "testHungServletRequest";
        Log.info(c, METHOD_NAME, "Entry");

        int NumofServletRequests = 5;
        int TotalNumofRequests = 2 * NumofServletRequests;

        try {

            // Set servlet countdownlatch for active request test
            setCountDownLatchInServlet(TotalNumofRequests);

            // Create sessionRequest and JDBCRequest threads
            Thread th_session[] = new Thread[NumofServletRequests];
            Thread th_jdbc[] = new Thread[NumofServletRequests];
            createRequestThreads(th_session, NumofServletRequests, "testSessionRequest");
            createRequestThreads(th_jdbc, NumofServletRequests, "testBasicQueryToSingle");

            // Starts N-1 threads that send request to servlet
            for (int i = 0; i < NumofServletRequests - 1; i++) {
                th_session[i].start();
                th_jdbc[i].start();
            }

            waitInServletForCountDownLatch(2);

            // Issue mbean call and check for correct active request message
            long receivedActiveRequests = mbeanCall("ActiveRequestCount");
            assertTrue(
                       "Incorrect active requests to server found. Expected: "
                       + (TotalNumofRequests - 2 + mbeanServletActiveCount)
                       + " Found: " + receivedActiveRequests,
                       receivedActiveRequests == (TotalNumofRequests - 2 + mbeanServletActiveCount));

            // Sleep 30 seconds to get requests into hung. This will print a Java Dump to
            // screen after the hungRequestThreshold is up
            Thread.sleep(30000);

            // Issue mbean call and check for correct slow servlet request message
            long receivedHungRequests = mbeanCall("HungRequestCount");
            assertTrue("Incorrect hung requests to server found. Expected: " + (TotalNumofRequests - 2) + " Found: "
                       + receivedHungRequests, receivedHungRequests == (TotalNumofRequests - 2));

            // Start last thread to release CountDownLatch
            th_session[NumofServletRequests - 1].start();
            th_jdbc[NumofServletRequests - 1].start();

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
        String waitForCountDownLatch = getURLString("waitForCountDownLatch", countToWaitFor);
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
    private static String getURLString(String testName, String variable) {
        return new String("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                          + "/RequestTimingWebApp/RequestTimingServlet?testName=" + testName + "&value=" + variable);
    }

    /**
     * Creates HTTP Client and invoke the application uri to get the response.
     * Increases the totalRequestCount to keep track
     *
     * @return String response of the application
     */

    private static String getHttpResponse(String url) throws IOException {
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
     *                    -- array of threads
     * @param numReqs
     *                    -- number of requests is the number of threads needed
     */
    private void createRequestThreads(Thread[] th, int numReqs, String method) {
        // Send N servlet requests to server, last request used to terminate
        // CountDownLatch
        final String METHOD_NAME = "createRequestThreads" + method;
        final String servletMethod = method;
        for (int i = 0; i < numReqs; i++) {
            th[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        // Sleep 2 seconds, then send request to servlet and print response to log
                        Thread.sleep(2000);
                        String response = getHttpResponse(getURLString(servletMethod, ""));
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
            Log.info(c, METHOD_NAME, methodName + ": " + count);
        } catch (Exception ex) {
            fail("mbeanCall(): " + "Not a valid response. Received: " + response);
        }
        return count;
    }

    /**
     * This helper method calls the testSessioRequest method in servlet to create a
     * session
     *
     */
    private void testSessionMethod() throws Exception {
        String response = getHttpResponse(getURLString("testSessionRequest", ""));
        Log.info(c, "testSessionMethod", response);
    }

    /**
     * This helper method calls the testBasicQueryToSingle method in servlet to run
     * a JDBC query
     *
     */
    private void testJDBCQuery() throws Exception {
        String response = getHttpResponse(getURLString("testBasicQueryToSingle", ""));
        Log.info(c, "testJDBCQuery", response);
    }

    /**
     * This helper method calls the setupTables method in servlet to setup the
     * require derby entries
     *
     */
    public static void setupTables() throws Exception {
        String response = getHttpResponse(getURLString("setUpTables", ""));
        Log.info(c, "setupTables", response);
    }

}
