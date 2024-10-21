/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
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
@Mode(FULL)
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ConcurrentTckLauncherFull {

    final static Map<String, String> additionalProps = new HashMap<>();

    @Server("ConcurrentTCKFullServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        //Comment out to use snapshot version
        additionalProps.put("jakarta.concurrent.tck.groupid", "jakarta.enterprise.concurrent");
        additionalProps.put("jakarta.concurrent.tck.version", "3.1.1");

        //Jakarta TCK platform
        additionalProps.put("jakarta.tck.platform", "platform");
        additionalProps.put("jimage.dir", server.getServerSharedPath() + "jimage/output/");

        if (!FATSuite.shouldRunSignatureTests(ConcurrentTckLauncherFull.class)) {
            additionalProps.put("jakarta.tck.platform", "platform & !signature");
        }

        //Finally start the server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "WLTC0032W", //Transaction rollback warning.
                          "WLTC0033W", //Transaction rollback warning.
                          "J2CA0046E", //See: https://github.com/jakartaee/concurrency/issues/317
                          "DSRA9400E", //See: https://github.com/jakartaee/concurrency/issues/317
                          "CWWKC1101E", //See: https://github.com/jakartaee/concurrency/issues/317
                          "CWWKE0955E", //websphere.java.security java 18+
                          "CWWKS9582E", //Quickstart security
                          "CWWKS0901E" //Quickstart security
        );
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchConcurrent31TCKFull() throws Exception {
        TCKRunner.build(server, Type.JAKARTA, "Concurrency")
                        .withPlatfromVersion("11")
                        .withQualifiers("full")
                        .withAdditionalMvnProps(additionalProps)
                        .runTCK();
    }
}