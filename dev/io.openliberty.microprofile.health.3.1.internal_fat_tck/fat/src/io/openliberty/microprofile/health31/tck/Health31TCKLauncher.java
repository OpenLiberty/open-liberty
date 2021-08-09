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
package io.openliberty.microprofile.health31.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PortType;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class Health31TCKLauncher {

    @Server("Health31TCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer("console.log", true, true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMMH0052W", "CWWKZ0002E", "SRVE0190E", "CWWKZ0014W", "CWNEN0047W", "CWNEN0048W", "CWNEN0049W");
    }

    @Test
    @AllowedFFDC() // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchHealth31Tck() throws Exception {
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getPort(PortType.WC_defaulthost));

        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("test.url", protocol + "://" + host + ":" + port);

        MvnUtils.runTCKMvnCmd(server, "io.openliberty.microprofile.health.3.1.internal_fat_tck", this.getClass() + ":launchHealth31Tck", additionalProps);
    }

}
