/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.lra.tck;

import java.util.Collections;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

//import com.ibm.websphere.microprofile.faulttolerance_fat.suite.RepeatFaultTolerance;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs the whole LRA TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 */
@RunWith(FATRunner.class)
public class LraTck10Launcher {

    private static final String SERVER_NAME = "FaultTolerance20TCKServer";
    private static final String SERVER_NAME = "LRA10TCKServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(RepeatFaultTolerance.ft20metrics11Features(SERVER_NAME).fullFATOnly())
                    .andWith(RepeatFaultTolerance.mp30Features(SERVER_NAME).fullFATOnly())
                    .andWith(RepeatFaultTolerance.mp20Features(SERVER_NAME).fullFATOnly())
                    .andWith(RepeatFaultTolerance.mp13Features(SERVER_NAME).fullFATOnly())
                    .andWith(RepeatFaultTolerance.mp32Features(SERVER_NAME));

    @BeforeClass
    public static void setUp() throws Exception {
        Vendor vendor = JavaInfo.forServer(server).vendor();
        // For J9 JVMs, add JIT trace for getConfig method to diagnose crashes
        if (vendor == Vendor.IBM || vendor == Vendor.OPENJ9) {
            Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
            jvmOptions.put("-Xjit:{org/eclipse/microprofile/config/ConfigProvider.getConfig(Ljava/lang/ClassLoader;)Lorg/eclipse/microprofile/config/Config;}(tracefull,traceInlining,traceCG,log=getConfig.trace)",
                           null);
            server.setJvmOptions(jvmOptions);
        }

        server.startServer();
    }

    /**
     * Various TCK tests test for Deployment, Definition and other Exceptions and
     * these will cause the test suite to be marked as FAILED if found in the logs
     * when the server is shut down. So we tell Simplicity to allow for the message
     * ID's below.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMFT5001E", // CWMFT0001E: No free capacity is available in the bulkhead
                          "CWMFT5003E", // CWMFT5003E: The fallback method fallbackForServiceB with parameter types
                          // matching method serviceB was not found in class class
                          // org.eclipse.microprofile.fault.tolerance.tck.illegalConfig.FallbackMethodWithArgsClient.
                          "CWMFT5020E", // CWMFT5020E: The {0} asynchronous method does not have a return type of either java.util.concurrent.Future or java.util.concurrent.CompletionStage.
                          "CWMFT0001E", // CWMFT5009E: The Fallback policy for the method
                          // org.eclipse.microprofile.fault.tolerance.tck.illegalConfig.FallbackClientWithBothFallbacks.serviceB()
                          // is not valid because both FallbackHandler class class
                          // org.eclipse.microprofile.fault.tolerance.tck.illegalConfig.IncompatibleFallbackHandler
                          // and fallbackMethod serviceBFallback were specified.
                          "CWMFT5010E", // CWMFT5010E: The Retry policy parameter jitter value -1 for
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.RetryClientForValidationJitter.serviceA
                          // is not valid
                          "CWMFT0013E", // CWMFT5013E: The CircuitBreaker policy parameter failureRatio value -1 for the
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.CircuitBreakerClientForValidationDelay.serviceA
                          // is not valid, because the value must be between 0 and 1,
                          "CWMFT0014E", // CWMFT5014E: The CircuitBreaker policy parameter requestVolumeThreshold value
                          // -1 for the
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.CircuitBreakerClientForValidationReqVolNeg.serviceA
                          // is not valid, because the parameter must not be a negative number.
                          "CWMFT0015E", // CWMFT5015E: The CircuitBreaker policy parameter successThreshold value 0 for
                          // the
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.CircuitBreakerClientForValidationSuccessNeg.serviceA
                          // is not valid, because the parameter must not be a negative number.
                          "CWMFT0016E", // CWMFT5016E: The Bulkhead policy parameter value value -1....
                          "CWMFT5017E", // CWMFT5017E: The Retry policy maximum duration of 500 Millis for the
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.RetryClientForValidationDelayDuration.serviceA
                          // target is not valid as it must be greater than the delay duration of 1,000
                          // Millis.
                          "CWMFT5019W", //CWMFT5019W: The Retry policy jitter delay of 200 Millis for the
                          //org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRapidRetry55ClassAsynchBean target
                          //is not valid because the jitter delay must be less than the delay duration of 1 Micros.
                          "CWMFT5021E", // CWMFT5021E: The fallback method {0} with parameter and return types matching method {1} was not found in the class hierarchy of {2}.
                          "CWWKZ0002E");// CWWKZ0002E: An exception occurred while starting the application
                                        // ftInvalidCB5. The exception message was:
                                        // com.ibm.ws.container.service.state.StateChangeException:
                                        // org.jboss.weld.exceptions.DefinitionException
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tcl/tck-suite.html)
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchFaultToleranceTCK() throws Exception {
        boolean isFullMode = TestModeFilter.shouldRun(TestMode.FULL);

        String suiteFileName;
        switch (RepeatTestFilter.CURRENT_REPEAT_ACTION) {
            case RepeatFaultTolerance.FT20_METRICS11_ID:
            case RepeatFaultTolerance.MP30_FEATURES_ID:
                // For test configurations which only differ my the version of mpMetrics, just run the metrics tests
                suiteFileName = "tck-suite-metrics-only.xml";
                break;
            case RepeatFaultTolerance.MP13_FEATURES_ID:
                // For FT 1.0 configurations, run a compatible subset of tests which have fixes in the FT 2.0 TCK
                suiteFileName = "tck-suite-10-subset.xml";
                break;
            case RepeatFaultTolerance.MP20_FEATURES_ID:
                // For FT 1.1 configurations, run a compatible subset of tests which have fixes in the FT 2.0 TCK
                suiteFileName = "tck-suite-11-subset.xml";
                break;
            default:
                // Otherwise, fun the full test suite
                suiteFileName = isFullMode ? "tck-suite.xml" : "tck-suite-lite.xml";
        }

        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.microprofile.faulttolerance.2.0_fat_tck", this.getClass() + ":launchFaultToleranceTCK", suiteFileName,
                              Collections.emptyMap(), Collections.emptySet());
    }
}
