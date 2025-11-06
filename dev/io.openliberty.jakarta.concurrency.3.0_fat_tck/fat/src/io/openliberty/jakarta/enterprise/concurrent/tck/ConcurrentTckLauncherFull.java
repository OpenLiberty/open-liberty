/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
package io.openliberty.jakarta.enterprise.concurrent.tck;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * This is a test class that runs the entire Jakarta Concurrency TCK against Full Profile.
 *
 * The TCK results are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard location.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ConcurrentTckLauncherFull {

    final static Map<String, String> additionalProps = new HashMap<>();

    private static String suiteXmlFile = "tck-suite-full.xml"; //Default value

    @Server("ConcurrentTCKFullServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        //UNCOMMENT - To test against a local snapshot of TCK
//        additionalProps.put("jakarta.concurrent.tck.groupid", "jakarta.enterprise.concurrent");
//        additionalProps.put("jakarta.concurrent.tck.version", "3.0.4-SNAPSHOT");

        Map<String, String> opts = server.getJvmOptionsAsMap();
        //Path that jimage will output modules for signature testing
        opts.put("-Djimage.dir", server.getServerSharedPath() + "jimage/output/");
        server.setJvmOptions(opts);

        //Finally start the server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "WLTC0032W", //Transaction rollback warning.
                          "WLTC0033W", //Transaction rollback warning.
                          "CWWKS0901E" //Quickstart security
        );
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchConcurrent30TCKFull() throws Exception {
        suiteXmlFile = FATSuite.createSuiteXML(FATSuite.PROFILE.FULL);

        TCKRunner.build(server, Type.JAKARTA, "Concurrency")
                        .withPlatformVersion("10")
                        .withQualifiers("full")
                        .withSuiteFileName(suiteXmlFile)
                        .withAdditionalMvnProps(additionalProps)
                        .withLogging(Map.of("ee.jakarta.tck.concurrent", Level.ALL))
                        .runTCK();
    }
}