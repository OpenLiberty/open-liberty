/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;
import componenttest.topology.utils.tck.TCKUtilities;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class RestClientTckPackageTest {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    public static final String SERVER_NAME = "FATServer";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeatIf(SERVER_NAME,
                                                               TCKUtilities::areAllFeaturesPresent,
                                                               MicroProfileActions.MP70_EE10, // 4.0+EE10
                                                               MicroProfileActions.MP70_EE11); // 4.0+EE11

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        String javaVersion = System.getProperty("java.version");
        Log.info(RestClientTckPackageTest.class, "setup", "javaVersion: " + javaVersion);
        System.out.println("java.version = " + javaVersion);
//        if (javaVersion.startsWith("1.8")) {
//            useTCKSuite("java8");
//        } else if (TestModeFilter.shouldRun(TestMode.FULL)) {
//            useTCKSuite("FULL");
//        }
        server.startServer();
    }

//    private static void useTCKSuite(String id) throws Exception {
//        Path cwd = Paths.get(".");
//        Log.info(RestClientTckPackageTest.class, "setup", "cwd = " + cwd.toAbsolutePath());
//        Path java8File = Paths.get("publish/tckRunner/tck/tck-suite.xml-" + id);
//        Path tckSuiteFile = Paths.get("publish/tckRunner/tck/tck-suite.xml");
//        Files.copy(java8File, tckSuiteFile, StandardCopyOption.REPLACE_EXISTING);
//    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
//            server.postStopServerArchive(); // must explicitly collect since arquillian is starting/stopping the server
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
        }
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void testRestClient40Tck() throws Exception {
        // Skip running on the windows platform when not running locally.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            TCKRunner.build(server, Type.MICROPROFILE, "Rest Client")
                            .withDefaultSuiteFileName()
                            .runTCK();
        }
    }

}
