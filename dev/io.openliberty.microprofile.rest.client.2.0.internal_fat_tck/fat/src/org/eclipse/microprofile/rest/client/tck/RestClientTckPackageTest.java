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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;
import componenttest.topology.utils.tck.TCKResultsConstants;

import componenttest.topology.impl.LibertyServerFactory;

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
    public static void setup() throws Exception {
        String javaVersion = System.getProperty("java.version");
        Log.info(RestClientTckPackageTest.class, "setup", "javaVersion: " + javaVersion);
        Log.info(RestClientTckPackageTest.class, "setup", "fipsEnabled: " + fipsEnabled);
        System.out.println("java.version = " + javaVersion);
        if (javaVersion.startsWith("1.8")) {
            useTCKSuite("java8");
        } else if (fipsEnabled) {
            useTCKSuite("fips");  
        }
        server.startServer();
    }

    private static void useTCKSuite(String id) throws Exception {
        Path cwd = Paths.get(".");
        Log.info(RestClientTckPackageTest.class, "setup", "cwd = " + cwd.toAbsolutePath());
        Path java8File = Paths.get("publish/tckRunner/tck/tck-suite.xml-" + id);
        Path tckSuiteFile = Paths.get("publish/tckRunner/tck/tck-suite.xml");
        Files.copy(java8File, tckSuiteFile, StandardCopyOption.REPLACE_EXISTING);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
//            server.postStopServerArchive(); // must explicitly collect since arquillian is starting/stopping the server
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
        }
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void testRestClient20Tck() throws Exception {
        // Skip running on the windows platform when not running locally.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            TCKRunner.build(server, Type.MICROPROFILE, TCKResultsConstants.REST_CLIENT)
                            .withDefaultSuiteFileName()
                            .withPlatformVersion(TCKResultsConstants.MICROPROFILE_VERSION_41) //Latest MicroProfile version
                            .runTCK();
        }
    }

}
