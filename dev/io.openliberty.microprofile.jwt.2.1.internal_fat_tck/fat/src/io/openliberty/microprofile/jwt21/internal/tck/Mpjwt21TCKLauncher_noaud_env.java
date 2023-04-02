/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.jwt21.internal.tck;

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
@RunWith(FATRunner.class)
public class Mpjwt21TCKLauncher_noaud_env {

    @Server("jwt21tckNoaudEnv")
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
        // CWWKS5508E - RequiredClaimsTest - verifyTokenWithoutName generates this error as expected for this test (mpJwt2.0)
        // CWWKS5506E - RequiredClaimsTest - verifyTokenWithoutName generates this error as expected for this test (mpJwt2.0) along with FFDC.
        // CWWKS5519E - RequiredClaimsTest - verifyTokenWithoutName generates this error as expected for this test (mpJwt2.0)
        server.stopServer("CWWKG0014E", "CWWKS5524E", "CWWKS6023E", "CWWKS5523E", "CWWKS6031E", "CWWKS5524E", "CWWKZ0014W", "CWWKE0921W", "CWWKE0912W", "CWWKS5508E", "CWWKS5506E",
                          "CWWKS5519E");
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException")
    public void launchMpjwt21TCK_noaud_env() throws Exception {
        String suiteName = "tck_suite_noaud_env.xml";
        String bucketName = "io.openliberty.microprofile.jwt.2.1.internal_fat_tck";
        String testName = this.getClass() + ":launchMpjwt21TCK_aud_noenv2";
        Type type = Type.MICROPROFILE;
        String specName = "JWT Auth";
        TCKRunner.runTCK(server, bucketName, testName, type, specName, suiteName);
    }
}
