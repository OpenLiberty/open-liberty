/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.faulttolerance41.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;
import componenttest.topology.utils.tck.TCKUtilities;

/**
 * This is a test class that runs the whole Fault Tolerance TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 */
@RunWith(FATRunner.class)
public class FaultToleranceTck41Launcher {

    private static final String SERVER_NAME = "FaultTolerance41TCKServer";

    private static final boolean FAT_TEST_LOCALRUN = Boolean.getBoolean("fat.test.localrun");

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeatIf(SERVER_NAME, TCKUtilities::areAllFeaturesPresent, MicroProfileActions.MP70_EE10, MicroProfileActions.MP70_EE11);

    @Server(SERVER_NAME)
    public static LibertyServer server;

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
        server.stopServer("CWMFT0001E", // CWMFT0001E: No free capacity is available in the bulkhead
                          // does not have a return type of Future
                          "CWMFT5001E", // CWMFT5001E: The asynchronous method
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.BulkheadClientForValidationAsynchQueue.serviceA()
                          // does not have a return type of Future.
                          "CWMFT5003E", // CWMFT5003E: The fallback method fallbackForServiceB with parameter types
                          // matching method serviceB was not found in class class
                          // org.eclipse.microprofile.fault.tolerance.tck.illegalConfig.FallbackMethodWithArgsClient.
                          "CWMFT5010E", // CWMFT5010E: The Retry policy parameter jitter value -1 for
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.RetryClientForValidationJitter.serviceA
                          // is not valid
                          "CWMFT5013E", // CWMFT5013E: The CircuitBreaker policy parameter failureRatio value -1 for the
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.CircuitBreakerClientForValidationDelay.serviceA
                          // is not valid, because the value must be between 0 and 1,
                          "CWMFT5014E", // CWMFT5014E: The CircuitBreaker policy parameter requestVolumeThreshold value -1 for the
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.CircuitBreakerClientForValidationReqVolNeg.serviceA
                          // is not valid, because the parameter must not be a negative number.
                          "CWMFT5015E", // CWMFT5015E: The CircuitBreaker policy parameter successThreshold value 0 for the
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.CircuitBreakerClientForValidationSuccessNeg.serviceA
                          // is not valid, because the parameter must not be a negative number.
                          "CWMFT5016E", // CWMFT5016E: The Bulkhead policy parameter value value -1....
                          "CWMFT5017E", // CWMFT5017E: The Retry policy maximum duration of 500 Millis for the
                          // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.RetryClientForValidationDelayDuration.serviceA
                          // target is not valid as it must be greater than the delay duration of 1,000 Millis.
                          "CWMFT5019W", //CWMFT5019W: The Retry policy jitter delay of 200 Millis for the
                          //org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRapidRetry55ClassAsynchBean target
                          //is not valid because the jitter delay must be less than the delay duration of 1 Micros.
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
    public void launchFaultTolerance41TCK() throws Exception {
        boolean isFullMode = TestModeFilter.shouldRun(TestMode.FULL);

        String suiteFileName = isFullMode ? "tck-suite.xml" : "tck-suite-lite.xml";

        Map<String, String> additionalProps = new HashMap<>();
        if (FAT_TEST_LOCALRUN) {
            // Reduce timeout multiplier when running locally
            additionalProps.put("timeoutMultiplier", "1.0");
        }

        TCKRunner.build(server, Type.MICROPROFILE, "Fault Tolerance")
                        .withSuiteFileName(suiteFileName)
                        .withAdditionalMvnProps(additionalProps)
                        .runTCK();
    }

}
