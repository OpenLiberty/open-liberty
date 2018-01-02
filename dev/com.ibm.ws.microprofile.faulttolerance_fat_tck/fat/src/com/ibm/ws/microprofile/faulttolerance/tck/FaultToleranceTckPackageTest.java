/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.topology.utils.MvnUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a test class that runs the whole Fault Tolerance TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 */
@RunWith(FATRunner.class)
public class FaultToleranceTckPackageTest {

    @Server("FATServer")
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
        server.stopServer("CWMFT5001E", // CWMFT0001E: No free capacity is available in the bulkhead
                "CWMFT5003E", // CWMFT5003E: The fallback method fallbackForServiceB with parameter types
                              // matching method serviceB was not found in class class
                              // org.eclipse.microprofile.fault.tolerance.tck.illegalConfig.FallbackMethodWithArgsClient.
                "CWMFT0001E", // CWMFT5001E: The asynchronous method
                              // org.eclipse.microprofile.fault.tolerance.tck.invalidParameters.BulkheadClientForValidationAsynchQueue.serviceA()
                              // does not have a return type of Future
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
    public void testTck() throws Exception {
        if (!MvnUtils.init) {
            MvnUtils.init(server);
        }
        // Everything under autoFVT/results is collected from the child build machine
        // so we place the TCKs results there.
        File mvnOutput = new File(MvnUtils.home, "results/mvnOutput_TCK");
        int rc = MvnUtils.runCmd(MvnUtils.mvnCliTckRoot, MvnUtils.tckRunnerDir, mvnOutput);
        File src = new File(MvnUtils.home, "results/tck/surefire-reports/junitreports");
        File tgt = new File(MvnUtils.home, "results/junit");
        try {
            Files.walkFileTree(src.toPath(), new MvnUtils.CopyFileVisitor(src.toPath(), tgt.toPath()));
        } catch (java.nio.file.NoSuchFileException nsfe) {
            Assert.assertNull(
                    "The TCK tests' results directory does not exist which suggests the TCK tests did not run - check build logs."
                            + src.getAbsolutePath(),
                    nsfe);
        }

        // mvn returns 0 if all surefire tests pass and -1 otherwise - this Assert is
        // enough to mark the build as having failed
        // the TCK regression
        Assert.assertTrue(
                "com.ibm.ws.microprofile.faulttolerance_fat_tck:org.eclipse.microprofile.faulttolerance.tck.FaultToleranceTckPackageTest:testTck:TCK has returned non-zero return code of: "
                        + rc
                        + " This indicates test failure, see...autoFVT/results/junit.html/index.html. Raw TCK testng output is at: ...autoFVT/results/mvn* "
                        + "and ...autoFVT/results/tck/surefire-reports/index.html",
                rc == 0);
    }

}
