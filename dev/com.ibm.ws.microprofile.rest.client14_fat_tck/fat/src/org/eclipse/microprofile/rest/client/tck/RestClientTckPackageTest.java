/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
package org.eclipse.microprofile.rest.client.tck;

import java.util.Locale;

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
import componenttest.topology.utils.tck.TCKResultsConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import componenttest.topology.impl.LibertyServerFactory;
import com.ibm.websphere.simplicity.log.Log;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class RestClientTckPackageTest {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("FATServer");

    // Define fipsEnabled
    private static final boolean fipsEnabled;

    static {
        boolean isFipsEnabled = false;
        try {
            isFipsEnabled = server.isFIPS140_3EnabledAndSupported();
        } catch (Exception e) {
            e.printStackTrace();
        }
        fipsEnabled = isFipsEnabled;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(RestClientTckPackageTest.class, "setup", "fipsEnabled: " + fipsEnabled);
        if (fipsEnabled) {
            Path cwd = Paths.get(".");
            Log.info(RestClientTckPackageTest.class, "setup", "cwd = " + cwd.toAbsolutePath());
            Path fipsFile = Paths.get("publish/tckRunner/tck/tck-suite.xml-fips");
            Path tckSuiteFile = Paths.get("publish/tckRunner/tck/tck-suite.xml");
            Files.copy(fipsFile, tckSuiteFile, StandardCopyOption.REPLACE_EXISTING);
        }   
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E", "CWNEN0047W", "CWNEN0049W");
        }
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void testRestClient14Tck() throws Exception {
        // Skip running on the windows platform when not running locally.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            TCKRunner.build(server, Type.MICROPROFILE, TCKResultsConstants.REST_CLIENT)
                            .withDefaultSuiteFileName()
                            .runTCK();
        }
    }

}
