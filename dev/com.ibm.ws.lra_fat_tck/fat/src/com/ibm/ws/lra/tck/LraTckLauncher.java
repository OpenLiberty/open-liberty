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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

//import com.ibm.websphere.microprofile.faulttolerance_fat.suite.RepeatFaultTolerance;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
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
public class LraTckLauncher {

    private static final String SERVER_NAME = "LRATCKServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    // TODO not sure what the repeats are for in fault tolerance of if we need them in LRA
    /*
     * @ClassRule
     * public static RepeatTests repeat = RepeatTests.with(RepeatFaultTolerance.ft20metrics11Features(SERVER_NAME).fullFATOnly())
     * .andWith(RepeatFaultTolerance.mp30Features(SERVER_NAME).fullFATOnly())
     * .andWith(RepeatFaultTolerance.mp20Features(SERVER_NAME).fullFATOnly())
     * .andWith(RepeatFaultTolerance.mp13Features(SERVER_NAME).fullFATOnly())
     * .andWith(RepeatFaultTolerance.mp32Features(SERVER_NAME));
     */

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
        // server.stopServer("CWMFT5001E"); // CWMFT0001E: No free capacity is available in the bulkhead
        // CWMFT0001E: No free capacity is available in the bulkhead
        server.stopServer();
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tcl/tck-suite.html)
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchLRATCK() throws Exception {
        //boolean isFullMode = TestModeFilter.shouldRun(TestMode.FULL);
        //String suiteFileName = isFullMode ? "tck-suite.xml" : "tck-suite-lite.xml";

        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.lra_fat_tck", this.getClass() + ":launchLRATCK", Collections.emptyMap());
        //MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.microprofile.faulttolerance.2.0_fat_tck", this.getClass() + ":launchFaultToleranceTCK", suiteFileName,
        //                      Collections.emptyMap(), Collections.emptySet());
    }
}
