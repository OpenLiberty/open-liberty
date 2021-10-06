/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.mpjwt20.tck;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 *
 */
//@Mode(TestMode.QUARANTINE)
@RunWith(FATRunner.class)
public class Mpjwt20TCKLauncher_aud_noenv2 {

    @Server("jwt20tckAudNoenv")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // PrivHelper looked promising for fine grained java2sec exception management but did not work.
        //PrivHelper.generateCustomPolicy(server, "permission java.net.SocketPermission \"127.0.0.1\", \"resolve\"");
        server.startServer();
        server.waitForStringInLog("CWWKS4105I", 30000); // wait for ltpa keys to be created and service ready, which can happen after startup.
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // CWWKZ0014W  - we need app listed in server.xml even when it might not there, so allow this "missing app" error.
        // CWWKE0921W, 12w - the harness generates a java2sec socketpermission error, there's no way to suppress it  by itself in server.xml, so suppress this way
        // CWWKG0014E - intermittently caused by server.xml being momentarily missing during server reconfig
        server.stopServer("CWWKG0014E", "CWWKS5524E", "CWWKS6023E", "CWWKS5523E", "CWWKS6031E", "CWWKS5524E", "CWWKZ0014W", "CWWKS5604E", "CWWKE0921W", "CWWKE0912W");
    }

    @Test
    // @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchMpjwt20TCKLauncher_aud_noenv2() throws Exception {
        String port = String.valueOf(server.getBvtPort());
        String bucketAndTestName = this.getClass().getCanonicalName();
        Map<String, String> additionalProps = new HashMap<>();
        // need to pass the correct url for PublicKeyAsJWKLocationURLTest
        additionalProps.put("mp.jwt.tck.jwks.baseURL", "http://localhost:" + port + "/PublicKeyAsJWKLocationURLTest/");
        MvnUtils.runTCKMvnCmd(server, bucketAndTestName, bucketAndTestName, "tck_suite_aud_noenv2.xml", additionalProps, Collections.emptySet());

    }
}
