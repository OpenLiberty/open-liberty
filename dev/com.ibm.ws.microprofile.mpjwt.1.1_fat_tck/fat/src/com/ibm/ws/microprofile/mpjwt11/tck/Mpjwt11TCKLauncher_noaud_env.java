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
package com.ibm.ws.microprofile.mpjwt11.tck;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test. *
 */
//@Mode(TestMode.QUARANTINE)
@RunWith(FATRunner.class)
public class Mpjwt11TCKLauncher_noaud_env {

    @Server("tckNoaudEnv")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
        server.waitForStringInLog("CWWKS4105I", 30000); // wait for ltpa keys to be created and service ready, which can happen after startup.
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // CWWKZ0014W  - we need app listed in server.xml even when it might not there, so allow this "missing app" error.
        // CWWKE0921W, 12w - the harness generates a java2sec socketpermission error, there's no way to suppress it by itself in server.xml, so allow this way
        // CWWKG0014E - intermittently caused by server.xml being momentarily missing during server reconfig
        server.stopServer("CWWKG0014E", "CWWKS5524E", "CWWKS6023E", "CWWKS5523E", "CWWKS6031E", "CWWKS5524E", "CWWKZ0014W", "CWWKE0921W", "CWWKE0912W");
    }

    @Test
    //@AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchMpjwt11TCKLauncher_noaud_env() throws Exception {
        String bucketAndTestName = this.getClass().getCanonicalName();
        MvnUtils.runTCKMvnCmd(server, bucketAndTestName, bucketAndTestName, "tck_suite_noaud_env.xml", Collections.emptyMap(), Collections.emptySet());

    }
}
