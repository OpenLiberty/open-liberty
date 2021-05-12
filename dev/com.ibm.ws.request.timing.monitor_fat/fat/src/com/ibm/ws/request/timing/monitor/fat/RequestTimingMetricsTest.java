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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.request.timing.app.RequestTimingServlet;

import componenttest.annotation.Server;
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
public class RequestTimingMetricsTest {

    private static final Class<RequestTimingMetricsTest> c = RequestTimingMetricsTest.class;

    @Server("RequestTimingFeatureWithMetrics")
    @TestServlet(servlet = RequestTimingServlet.class, path = "RequestTimingWebApp")
    public static LibertyServer server;

    // Keeping a AtomicLong total count because of threads created
    public static AtomicInteger totalRequestCount;
    public static int activeServletRequest;

    public final String TestRequestHandlerUrl = getRequestTimingServletURLString("TestRequestHandler", 0);

    /**
     * JUnit guarantees that this gets run after the static set up in the superclass
     * (as long as the names are different).
     */
    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "RequestTimingWebApp", "com.ibm.ws.request.timing.app");
        totalRequestCount = new AtomicInteger();
        activeServletRequest = 1;

        JavaInfo java = JavaInfo.forCurrentVM();
        int javaMajorVersion = java.majorVersion();
        if (javaMajorVersion != 8) {
            Log.info(c, "setUp", " Java version = " + javaMajorVersion + " - It is higher than 8, adding --add-exports...");
            server.copyFileToLibertyServerRoot("add-exports/jvm.options");
        }

        server.startServer();

        //Wait until server has started
        String logMsg = server.waitForStringInLogUsingMark("CWWKF0011I");

        Log.info(c, "setUp", logMsg);
        Assert.assertNotNull("No CWWKF0011I was found.", logMsg);

        //Validate that we have registered the RequestTiming MBean
        Assert.assertNotNull("RequestTiming Mbean not registered",
                             server.waitForStringInTraceUsingMark("Monitoring MXBean WebSphere:type=RequestTimingStats"));
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
     * Basic Test.
     * Tests that the Monitoring MXBean for RequestTimingStats is registered.
     * Followed by validating that the Prometheus metric appears on /metrics/vendor
     * Followed by a quick count check for total requests.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testBasic() throws Exception {
        String testName = "testBasic";
        Log.info(c, testName, "Entry");

        /*
         * The call to /metrics counts as an active request. And the total is 1.
         */
        checkStrings(getHttpServlet("/metrics/vendor", server), new String[] {
                                                                               "vendor_requestTiming_activeRequestCount " + Long.toString(activeServletRequest),
                                                                               "vendor_requestTiming_requestCount_total " + Long.toString(totalRequestCount.get()),
                                                                               "vendor_requestTiming_hungRequestCount 0",
                                                                               "vendor_requestTiming_slowRequestCount 0" },
                     new String[] {});

        //Increment total requests by 3
        for (int i = 1; i <= 3; i++) {
            getHttpServlet("/metrics/vendor", server);
        }

        checkStrings(getHttpServlet("/metrics/vendor", server), new String[] {
                                                                               "vendor_requestTiming_requestCount_total " + Long.toString(totalRequestCount.get())
        },
                     new String[] {});

        Log.info(c, testName, "Exit");
    }

    /**
     * First, it updates server.xml with config from serverHungRequest.xml. Next, it
     * sets a CountDownLatch in the servlet where the requests will hang. Then,
     * sends multiple requests to the servlet, sleep for longer than the
     * hungRequestThreshold, and invokes the /metrics call. It checks to make
     * sure that the number of requests sent both the hung and active totals. This
     * is because a request that is hung still shows up as active until it
     * completes.
     *
     * Then, it will send one more request, which will break the CountDownLatch, and
     * wait 10s before invoking the /metrics call. There is a delay from when a
     * request completes and when the hung count updates in the mbean (and thusly vendor
     * metrics) for the
     * command handler so we need to wait 10s to get the correct hung value. Check
     * to see if the hung requests decremented correctly back to 0 after all the
     * requests complete.
     *
     * Finally, it will unblock any threads waiting in the servlet.
     *
     * This test mirrors the testHungServletRequest in RequestTimingMbeanTest.
     *
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testHungThreads() throws Exception {
        final String testName = "testHungThreads";
        Log.info(c, testName, "Entry");
        int numofHungRequests = 5;

        try {
            updateAndValidateConfigFile("serverHungRequestMetrics.xml");
            server.waitForStringInLogUsingMark("CWWKG0017I", 30000);

            // Set servlet countdown latch for active request test
            setCountDownLatchInServlet(numofHungRequests);

            // Holds threads that will be asynchronously doing requests on servlet
            // build request sets of threads
            Thread th[] = new Thread[numofHungRequests];
            createRequestThreads(th, numofHungRequests);

            // Starts N-1 threads that send request to servlet
            for (int i = 0; i < numofHungRequests - 1; i++) {
                th[i].start();
            }
            waitInServletForCountDownLatch(1);

            /*
             * Check metrics. The amount of active requests equals to the amount of
             * threads executed plus this current call to /metrics (i.e active request)
             *
             * There are no hung threads yet.
             * We will omit checking requestCount until the end due to timing issues.
             */
            int activeRequestCount = numofHungRequests - 1 + activeServletRequest;
            int hungRequestCount = 0;
            int slowRequestCount = 0;
            checkStrings(getHttpServlet("/metrics/vendor", server), new String[] {
                                                                                   "vendor_requestTiming_activeRequestCount " + Integer.toString(activeRequestCount),
                                                                                   "vendor_requestTiming_hungRequestCount " + Integer.toString(hungRequestCount),
                                                                                   "vendor_requestTiming_slowRequestCount " + Integer.toString(slowRequestCount) },
                         new String[] {});

            // Sleep 20 seconds to get requests into hung. This will print a Java Dump to
            // screen after the hungRequestThreshold is up
            Thread.sleep(20000);

            /*
             * Check metrics. The amount of active requests equals to the amount of
             * threads executed plus the call to /metrics. As they are hung. They
             * are still active.
             *
             * We will omit checking requestCount until the end due to timing issues.
             */
            activeRequestCount = numofHungRequests - 1 + activeServletRequest;
            hungRequestCount = numofHungRequests - 1;
            checkStrings(getHttpServlet("/metrics/vendor", server), new String[] {
                                                                                   "vendor_requestTiming_activeRequestCount " + Integer.toString(activeRequestCount),
                                                                                   "vendor_requestTiming_hungRequestCount " + Integer.toString(hungRequestCount),
                                                                                   "vendor_requestTiming_slowRequestCount " + Integer.toString(slowRequestCount) },
                         new String[] {});
            // Start last thread to release CountDownLatch
            th[numofHungRequests - 1].start();

            // Need to sleep so last thread can break CountDownLatch and active threads that
            // are waiting can complete
            waitInServletForCountDownLatch(0);

            // Sleep while counts update because hung count updates slower than active count
            Thread.sleep(10000);

            activeRequestCount = activeServletRequest;
            hungRequestCount = 0;
            checkStrings(getHttpServlet("/metrics/vendor", server), new String[] {
                                                                                   "vendor_requestTiming_activeRequestCount " + Long.toString(activeRequestCount),
                                                                                   "vendor_requestTiming_requestCount_total " + Long.toString(totalRequestCount.get()),
                                                                                   "vendor_requestTiming_hungRequestCount " + Long.toString(hungRequestCount),
                                                                                   "vendor_requestTiming_slowRequestCount " + Long.toString(slowRequestCount) },
                         new String[] {});

        } finally {
            waitInServletToUnblockThreads();
            Log.info(c, testName, "Exit");
        }
    }

    /**
     * This test updates server.xml with config from serverSlowRequestMetrics.xml. Next, it
     * sets a CountDownLatch in the servlet where the requests will hang. Then, it
     * sends multiple requests to the servlet, sleeps for 5s more than the
     * slowRequestThreshold specified in server.xml, and calls the /metrics endpoint.
     * It checks to make sure that the number of requests sent both the slow
     * and active totals. This is because a request that is slow still shows up as
     * active until it completes.
     *
     * Then, it will send one more request, which will break the CountDownLatch, and
     * wait 10s before invoking the /metrics endpoint call. There is a delay from when a
     * request completes and when the slow count updates in the mbean for the
     * command handler so we need to wait 10s to get the correct slow value. Check
     * to see if the slow requests decremented correctly back to 0 after all the
     * requests complete.
     *
     * Finally, it will unblock any threads waiting in the servlet.
     *
     * This test mirrors the testSlowServletRequest in RequestTimingMbeanTest.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSlowServletRequest() throws Exception {

        final String METHOD_NAME = "testSlowServletRequest";
        Log.info(c, METHOD_NAME, "Entry");
        int numofSlowRequests = 5;
        try {
            updateAndValidateConfigFile("serverSlowRequestMetrics.xml");
            //CWWKG0017I: The server configuration was successfully updated in __ seconds.
            server.waitForStringInLogUsingMark("CWWKG0017I", 30000);

            // Set servlet countdownlatch for active request test
            setCountDownLatchInServlet(numofSlowRequests);

            // Holds threads that will be asynchronously doing requests on servlet
            // build request sets of threads
            Thread th[] = new Thread[numofSlowRequests];
            createRequestThreads(th, numofSlowRequests);

            // Starts N-1 threads that send request to servlet
            for (int i = 0; i < numofSlowRequests - 1; i++) {
                th[i].start();
            }

            // Wait method that delays for up to a minute while threads start and send
            // requests
            waitInServletForCountDownLatch(1);

            /*
             * Check metrics. The amount of active requests equals to the amount of
             * threads executed plus this current call to /metrics (i.e active request)
             *
             * There are no slow threads yet.
             * We will omit checking requestCount until the end due to timing issues.
             */
            int activeRequestCount = numofSlowRequests - 1 + activeServletRequest;
            int hungRequestCount = 0;
            int slowRequestCount = 0;
            checkStrings(getHttpServlet("/metrics/vendor", server), new String[] {
                                                                                   "vendor_requestTiming_activeRequestCount " + Integer.toString(activeRequestCount),
                                                                                   "vendor_requestTiming_hungRequestCount " + Integer.toString(hungRequestCount),
                                                                                   "vendor_requestTiming_slowRequestCount " + Integer.toString(slowRequestCount) },
                         new String[] {});

            // Sleep 15 seconds so threads in servlet go from active to active and slow
            Thread.sleep(15000);

            /*
             * Check metrics. The amount of active requests equals to the amount of
             * threads executed plus the call to /metrics. As they are slow. They
             * are still active.
             *
             * We will omit checking requestCount until the end due to timing issues.
             */
            activeRequestCount = numofSlowRequests - 1 + activeServletRequest;
            slowRequestCount = numofSlowRequests - 1;
            checkStrings(getHttpServlet("/metrics/vendor", server), new String[] {
                                                                                   "vendor_requestTiming_activeRequestCount " + Integer.toString(activeRequestCount),
                                                                                   "vendor_requestTiming_hungRequestCount " + Integer.toString(hungRequestCount),
                                                                                   "vendor_requestTiming_slowRequestCount " + Integer.toString(slowRequestCount) },
                         new String[] {});

            // Start last thread to release CountDownLatch
            th[numofSlowRequests - 1].start();

            // Need to sleep so last thread can break CountDownLatch and active threads that
            // are waiting can complete
            waitInServletForCountDownLatch(0);

            // Slow request count does not decrement as fast as active so wait for a 10 more
            // seconds while slow count updates
            Thread.sleep(10000);

            activeRequestCount = activeServletRequest;
            slowRequestCount = 0;
            checkStrings(getHttpServlet("/metrics/vendor", server), new String[] {
                                                                                   "vendor_requestTiming_activeRequestCount " + Long.toString(activeRequestCount),
                                                                                   "vendor_requestTiming_requestCount_total " + Long.toString(totalRequestCount.get()),
                                                                                   "vendor_requestTiming_hungRequestCount " + Long.toString(hungRequestCount),
                                                                                   "vendor_requestTiming_slowRequestCount " + Long.toString(slowRequestCount) },
                         new String[] {});

        } finally {
            waitInServletToUnblockThreads();
            Log.info(c, METHOD_NAME, "Exit");

        }
    }

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
        String setCountDownLatchUrl = getRequestTimingServletURLString("setCountDownLatch", numbrequests);
        String response = getHttpServlet(setCountDownLatchUrl, server);
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
        waitForCountDownLatch = getRequestTimingServletURLString("waitForCountDownLatch", countToWaitFor);
        String response = getHttpServlet(waitForCountDownLatch, server);
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
        String unblockThreadsUrl = getRequestTimingServletURLString("unblockThreads", 0);
        String response = getHttpServlet(unblockThreadsUrl, server);
        Log.info(c, METHOD_NAME, response);
    }

    /** URL helper */
    private String getRequestTimingServletURLString(String testName, int intValue) {
        return getRequestTimingServletURLString(testName, Integer.toString(intValue));
    }

    private String getRequestTimingServletURLString(String testName, String value) {
        return new String("/RequestTimingWebApp/RequestTimingServlet?testName=" + testName + "&value=" + value);
    }

    private String getHttpServlet(String servletPath, LibertyServer server) throws Exception {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + servletPath;
            URL checkerServletURL = new URL(sURL);
            con = (HttpURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            Log.info(c, "getHttpServlet", sURL);
            totalRequestCount.incrementAndGet();
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    private void checkStrings(String metricsText, String[] expectedString, String[] unexpectedString) {
        for (String m : expectedString) {
            if (!metricsText.contains(m)) {
                Log.info(c, "checkStrings", "Failed:\n" + metricsText);
                Assert.fail("Did not contain string: " + m);
            }
        }
        for (String m : unexpectedString) {
            if (metricsText.contains(m)) {
                Log.info(c, "checkStrings", "Failed:\n" + metricsText);
                Assert.fail("Contained string: " + m);
            }
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
                        String response = getHttpServlet(TestRequestHandlerUrl, server);
                        Log.info(c, METHOD_NAME, response);
                    } catch (Exception ex) {
                        Log.info(c, METHOD_NAME, "Exception found: " + ex.getMessage());
                    }
                }
            };
        }
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
}
