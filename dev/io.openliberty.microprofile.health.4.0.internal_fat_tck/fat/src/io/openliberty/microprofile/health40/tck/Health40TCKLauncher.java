/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health40.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PortType;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class Health40TCKLauncher {

    private static final String SERVER_NAME = "Health40TCKServer";

    @ClassRule
    public static RepeatTests r = HealthActions.repeat(SERVER_NAME,
                                                       MicroProfileActions.MP70_EE11,
                                                       MicroProfileActions.MP70_EE10,
                                                       MicroProfileActions.MP61,
                                                       MicroProfileActions.MP50);

    @Server(SERVER_NAME)
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
    public void launchHealth40Tck() throws Exception {
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getPort(PortType.WC_defaulthost));

        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("test.url", protocol + "://" + host + ":" + port);

        TCKRunner.build(server, Type.MICROPROFILE, "Health")
                        .withDefaultSuiteFileName()
                        .withAdditionalMvnProps(additionalProps)
                        .runTCK();
    }

}
