/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics20.tck.launcher;

import static org.junit.Assume.assumeTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class MetricsTCKLauncher {

    @Server("MetricsTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Ignore CWWKZ0131W - In windows, some jars are being locked during the test. Issue #2768
        server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E", "CWWKZ0131W");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchTck() throws Exception {
        //disable tests for Java versions 11.0.0 - 11.0.3 since there's a bug in TLS 1.3 implementation
        JavaInfo javaInfo = JavaInfo.forServer(server);
        assumeTrue(!(javaInfo.majorVersion() == 11 && javaInfo.minorVersion() == 0
                     && javaInfo.microVersion() <= 3));
        String protocol = "https";
        String host = server.getHostname();
        String port = Integer.toString(server.getHttpDefaultSecurePort());

        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("test.url", protocol + "://" + host + ":" + port);
        additionalProps.put("test.user", "theUser");
        additionalProps.put("test.pwd", "thePassword");

        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.microprofile.metrics.2.0_fat_tck", "launchTck", additionalProps);
    }

}
