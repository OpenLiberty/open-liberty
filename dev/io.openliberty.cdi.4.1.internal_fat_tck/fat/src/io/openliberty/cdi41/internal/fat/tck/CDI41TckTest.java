/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi41.internal.fat.tck;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

@RunWith(FATRunner.class)
public class CDI41TckTest {
    /**  */
    private static final String PORTING_PACKAGE_BUNDLE = "io.openliberty.cdi.tck.porting.package";

    /**  */
    private static final String PORTING_PACKAGE_FEATURE = "io.openliberty.cdi.tck.porting-package-4.1";

    private static final String SERVER_NAME = "FATServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.installSystemFeature(PORTING_PACKAGE_FEATURE);
        server.installSystemBundle(PORTING_PACKAGE_BUNDLE);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("CWWKZ0002E", // Failed to start app
                              "CWNEN0057W" // injection target must not be static (InitializerUnallowedDefinitionTest)
            );
        } finally {
            server.uninstallSystemBundle(PORTING_PACKAGE_BUNDLE);
            server.uninstallSystemFeature(PORTING_PACKAGE_FEATURE);
        }
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDCs so we have to allow for this.
    public void testCDI41Tck() throws Exception {

        String suiteFileName = TestModeFilter.shouldRun(TestMode.FULL) ? "tck-suite.xml" : "tck-suite-lite.xml";

        TCKRunner.build(server, Type.JAKARTA, "CDI Core")
                 .withSuiteFileName(suiteFileName)
                 .withProfiles("tckRunner")
                 .runTCK();
    }

}
