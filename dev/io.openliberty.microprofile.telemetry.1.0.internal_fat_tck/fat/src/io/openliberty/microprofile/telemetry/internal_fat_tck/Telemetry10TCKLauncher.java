/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat_tck;

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
import componenttest.topology.utils.tck.TCKResultsConstants;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class Telemetry10TCKLauncher {

    private static final String SERVER_NAME = "Telemetry10TCKServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.setDebuggingAllowed(true);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchTelemetry10Tck() throws Exception {
        TCKRunner.build(server, Type.MICROPROFILE, TCKResultsConstants.TELEMETRY)
                        .withDefaultSuiteFileName()
                        .withPlatformVersion(TCKResultsConstants.MICROPROFILE_VERSION_60) //Latest MicroProfile version
                        .runTCK();
    }
}
