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
package com.ibm.ws.microprofile.reactive.streams.operators.tck;

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
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class ReactiveStreamsTCKLauncher {

    @Server("ReactiveStreamsTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Map<String,String> props = new HashMap<String,String>();
        //Set timeout for the tests.
        props.put("DEFAULT_TIMEOUT_MILLIS","10000");//Increase timeout before tests fail.
        props.put("DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS","100"); //By default NO_SIGNALS_TIMEOUT == DEFAULT_TIMEOUT_MILLIS. Every test will sleep for NO_SIGNALS_TIMEOUT so set this back to the original default to prevent the tests taking hours. 
        server.setAdditionalSystemProperties(props);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    @Mode(TestMode.EXPERIMENTAL)
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchReactiveStreamsTck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.microprofile.reactive.streams_fat_tck", this.getClass() + ":launchReactiveStreamsTck");
    }

}
