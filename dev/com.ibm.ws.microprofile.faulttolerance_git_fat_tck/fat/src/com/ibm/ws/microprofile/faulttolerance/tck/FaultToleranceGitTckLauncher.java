/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.tck;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs the whole Fault Tolerance TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 */
@RunWith(FATRunner.class)
public class FaultToleranceGitTckLauncher {

    /**  */
    private static String GIT_REPO_PARENT_DIR = "publish/gitRepos/";
    private static final String GIT_REPO_NAME = "microprofile-fault-tolerance";

    @Server("FaultToleranceTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
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
    @Mode(TestMode.EXPERIMENTAL)
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void runFreshMasterBranchTck() throws Exception {

        File repoParent = new File(GIT_REPO_PARENT_DIR);
        File repo = new File(repoParent, GIT_REPO_NAME);

        MvnUtils.mvnCleanInstall(repo);

        HashMap<String, String> addedProps = new HashMap<String, String>();

        String apiVersion = MvnUtils.getApiSpecVersionAfterClone(repo);
        System.out.println("Queried api.version is : " + apiVersion);
        addedProps.put(MvnUtils.API_VERSION, apiVersion);

        String tckVersion = MvnUtils.getTckVersionAfterClone(repo);
        System.out.println("Queried tck.version is : " + tckVersion);
        addedProps.put(MvnUtils.TCK_VERSION, tckVersion);

        // A command line -Dprop=value actually gets to here as a environment variable...
        String implVersion = System.getenv("impl.version");
        System.out.println("Passed in impl.version is : " + implVersion);
        addedProps.put(MvnUtils.IMPL_VERSION, implVersion);

        // We store a set of keys that we want the system to add "1.1" or "1.2" etc to
        // depending on the pom.xml contents.
        HashSet<String> versionedLibraries = new HashSet<>(Arrays.asList("com.ibm.websphere.org.eclipse.microprofile.faulttolerance"));
        String backStopImpl = "1.0"; // Used if there is no impl matching the spec/pom.xml <version> AND impl.version is not set
        addedProps.put(MvnUtils.BACKSTOP_VERSION, backStopImpl);

        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.microprofile.faulttolerance_fat_tck", this
                        .getClass() + ":launchFaultToleranceTCK", MvnUtils.DEFAULT_SUITE_FILENAME, addedProps, versionedLibraries);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testThatDoesNothingAndCanAlwaysRunAndPass() {
        if (TestModeFilter.FRAMEWORK_TEST_MODE != TestMode.EXPERIMENTAL) {
            System.out.println("\n\n\n");
            System.out.println("TCK MASTER BRANCH RUN NOT REQUESTED: fat.test.mode=" + TestModeFilter.FRAMEWORK_TEST_MODE
                               + ", run with '-Dfat.test.mode=experimental' to run the TCK");
            System.out.println("\n\n\n");
            // In FATs System.out is captured to a file, we try to be kind to any developer and give it to them straight
            if (Boolean.valueOf(System.getProperty("fat.test.localrun"))) {
                try (PrintStream screen = new PrintStream(new FileOutputStream(FileDescriptor.out))) {
                    screen.println("\n\n\n");
                    screen.println("TCK MASTER BRANCH RUN NOT REQUESTED: fat.test.mode=" + TestModeFilter.FRAMEWORK_TEST_MODE
                                   + ", run with '-Dfat.test.mode=experimental' to run the TCK");
                    screen.println("\n\n\n");
                }
            }
        }
    }
}
