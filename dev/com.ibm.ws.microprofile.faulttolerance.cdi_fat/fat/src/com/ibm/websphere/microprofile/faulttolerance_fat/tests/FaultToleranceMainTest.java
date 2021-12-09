/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance_fat.suite.AnnotationFilter;
import com.ibm.websphere.microprofile.faulttolerance_fat.suite.BasicTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.suite.FATSuite;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableServlet;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.AsyncBulkheadServlet;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.AsyncServlet;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.CircuitBreakerServlet;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.FallbackServlet;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.RetryServlet;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.SyncBulkheadServlet;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TimeoutServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class FaultToleranceMainTest extends FATServletClient {

    private static final Logger LOGGER = Logger.getLogger(FaultToleranceMainTest.class.getName());

    private static final String SERVER_NAME = "CDIFaultTolerance";

    @Server(SERVER_NAME)
    @TestServlets({ @TestServlet(contextRoot = "CDIFaultTolerance", servlet = AsyncServlet.class),
                    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = AsyncBulkheadServlet.class),
                    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = SyncBulkheadServlet.class),
                    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = CircuitBreakerServlet.class),
                    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = FallbackServlet.class),
                    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = RetryServlet.class),
                    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = TimeoutServlet.class),
                    @TestServlet(contextRoot = "DisableEnable", servlet = DisableEnableServlet.class)
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeatAll(SERVER_NAME);

    @Rule
    public AnnotationFilter filter = AnnotationFilter
                    .requireAnnotations(BasicTest.class)
                    .forAllRepeatsExcept(MicroProfileActions.MP40_ID, MicroProfileActions.MP41_ID)
                    .inModes(TestMode.LITE);

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.exportCDIFaultToleranceAppToServer(server);
        FATSuite.exportDisableEnableAppToServer(server);

        server.addEnvVar("FAULT_TOLERANCE_VERSION", getFaultToleranceVersion());
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        /*
         * Ignore following exception as those are expected:
         * CWWKC1101E: The task com.ibm.ws.microprofile.faulttolerance.cdi.FutureTimeoutMonitor@3f76c259, which was submitted to executor service
         * managedScheduledExecutorService[DefaultManagedScheduledExecutorService], failed with the following error:
         * org.eclipse.microprofile.faulttolerance.exceptions.FTTimeoutException: java.util.concurrent.TimeoutException
         */
        server.stopServer("CWWKC1101E");
    }

    /**
     * Get the fault tolerance feature version from the server.xml
     *
     * @return
     * @throws Exception
     */
    private static String getFaultToleranceVersion() throws Exception {
        Set<String> feature = server.getServerConfiguration().getFeatureManager().getFeatures();

        LOGGER.info("Features: " + String.join(", ", feature));

        Optional<String> ftFeature = feature.stream().filter((s) -> s.toLowerCase().startsWith("mpfaulttolerance")).findFirst();
        if (!ftFeature.isPresent()) {
            throw new Exception("No mpFaultTolerance feature in server config");
        }

        String featureName = ftFeature.get();
        String featureVersion = featureName.substring(featureName.indexOf("-") + 1);
        LOGGER.info("Feature version: " + featureVersion);
        return featureVersion;
    }

    /**
     * Test a synchronous bulkhead by firing multiple requests at it
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMultiRequestBulkhead() throws Exception {

        final long TEST_TWEAK_TIME_UNIT = 100;
        final long TIMEOUT = 5000;
        final long FUTURE_THRESHOLD = 6000;

        // Make an initial request so that everything is initialized
        HttpUtils.findStringInReadyUrl(server, "/CDIFaultTolerance/multi-request-bulkhead", "Success");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Connect C has a pool size of 2
        // Fire three requests in parallel
        Future<String> future1 = executor.submit(() -> HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(TEST_TWEAK_TIME_UNIT);

        Future<String> future2 = executor.submit(() -> HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(TEST_TWEAK_TIME_UNIT);

        Future<String> future3 = executor.submit(() -> HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(TEST_TWEAK_TIME_UNIT);

        executor.shutdown();

        // Two of the tasks should succeed, one should fail with a bulkhead exception
        List<Result> results = new ArrayList<>();
        results.add(new Result("Task One", future1.get(TIMEOUT + FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)));
        results.add(new Result("Task Two", future2.get(FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)));
        results.add(new Result("Task Three", future3.get(TIMEOUT + FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)));

        List<Result> successes = new ArrayList<>();
        List<Result> failures = new ArrayList<>();
        for (Result result : results) {
            if (result.value.contains("Success")) {
                successes.add(result);
            } else if (result.value.contains("BulkheadException")) {
                failures.add(result);
            } else {
                fail("Unexpected failure result: " + result);
            }
        }

        assertThat("Number of successes", successes, hasSize(2));
        assertThat("Number of failures", failures, hasSize(1));

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP20_ID)) {
            // Check for the correct message for FT 1.x
            assertThat("Failure message should have correct code", failures.get(0).value, containsString("CWMFT0001E"));
            // Ensure that the message substitution has happened
            assertThat("Failure message should be substituted", failures.get(0).value, not(containsString("bulkhead.no.threads.CWMFT0001E")));
        }
    }

    private static class Result {
        String name;
        String value;

        public Result(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name + ": " + value;
        }
    }

    @SkipForRepeat({ MicroProfileActions.MP40_ID, MicroProfileActions.MP41_ID, MicroProfileActions.MP50_ID }) // FT 3.0+ does not close executors until the application shuts down
    @Test
    public void testExecutorsClose() throws Exception {

        RemoteFile traceLog = server.getMostRecentTraceFile();
        server.setMarkToEndOfLog(traceLog);

        // This calls a RequestScoped bean which only has fault tolerance annotations on the method
        // This should cause executors to get cleaned up
        runTest(server, "CDIFaultTolerance/retry", "testRetryAbortOn");

        assertNotNull("Did not find executor cleanup message in trace file", server.waitForStringInLog("Cleaning up executors", traceLog));
    }
}
