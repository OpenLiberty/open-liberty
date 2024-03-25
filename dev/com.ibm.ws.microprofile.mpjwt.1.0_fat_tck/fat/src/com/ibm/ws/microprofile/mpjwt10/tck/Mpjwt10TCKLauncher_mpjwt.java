/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.ws.microprofile.mpjwt10.tck;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test. *
 */
//@Mode(TestMode.QUARANTINE)
@RunWith(FATRunner.class)
public class Mpjwt10TCKLauncher_mpjwt {

    @Server("mpjwt")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
        server.waitForStringInLog("CWWKS4105I", 30000); // wait for ltpa keys to be created and service ready, which can happen after startup.
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    @AllowedFFDC("org.jose4j.jwt.consumer.InvalidJwtSignatureException")
    public void launchMpjwt10TCK_mpjwt() throws Exception {
        String suiteName = "tck_suite_mpjwt.xml";
        String bucketName = "com.ibm.ws.microprofile.mpjwt.1.0_fat_tck";
        String testName = this.getClass() + ":launchMpjwt10TCK_mpjwt";
        Type type = Type.MICROPROFILE;
        String specName = "JWT Auth";
        TCKRunner.runTCK(server, bucketName, testName, type, specName, suiteName);
    }
}
