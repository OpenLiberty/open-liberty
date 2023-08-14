/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.operators30.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class ReactiveStreams30TCKLauncher {

    @Server("ReactiveStreams30TCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Map<String, String> props = new HashMap<String, String>();
        //Set timeout for the tests.
        props.put("DEFAULT_TIMEOUT_MILLIS", "10000");//Increase timeout before tests fail.
        props.put("DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS", "100"); //By default NO_SIGNALS_TIMEOUT == DEFAULT_TIMEOUT_MILLIS. Every test will sleep for NO_SIGNALS_TIMEOUT so set this back to the original default to prevent the tests taking hours.
        server.setAdditionalSystemProperties(props);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    @Mode(TestMode.EXPERIMENTAL) //This version of the TCK is currently taking over an hour to run. This should be set to full mode after the performance issues have been resolved.
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchReactiveStreams30Tck() throws Exception {
        String bucketName = "io.openliberty.microprofile.reactive.streams.operators30.internal_fat_tck";
        String testName = this.getClass() + ":launchReactiveStreams30Tck";
        Type type = Type.MICROPROFILE;
        String specName = "Reactive Streams";
        TCKRunner.runTCK(server, bucketName, testName, type, specName);
    }

}
