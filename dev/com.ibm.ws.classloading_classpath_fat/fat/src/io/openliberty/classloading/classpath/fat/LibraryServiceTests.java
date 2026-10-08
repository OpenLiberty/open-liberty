/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.LIBRARY_USER_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB1_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB2_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB3_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB4_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIBRARY_USER_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIBRARY_USER_WAR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.classloading.library.test.app.LibraryUserTestServlet;

@RunWith(FATRunner.class)
public class LibraryServiceTests extends FATServletClient {

    @Server(LIBRARY_USER_TEST_SERVER)
    @TestServlet(servlet = LibraryUserTestServlet.class, contextRoot = TEST_LIBRARY_USER_APP)
    public static LibertyServer server;

    @BeforeClass
    public static void setupTestServer() throws Exception {
        server.installSystemFeature("testLibraryUser-1.0");
        assertTrue("testLibraryUser-1.0 should have been lib/features",
                   server.fileExistsInLibertyInstallRoot("lib/features/testLibraryUser-1.0.mf"));
        server.installSystemBundle("test.library.user");
        assertTrue("test.library.user.jar should have been copied to lib",
                   server.fileExistsInLibertyInstallRoot("lib/test.library.user.jar"));

        ShrinkHelper.exportAppToServer(server, TEST_LIBRARY_USER_WAR, DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB1_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB2_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB3_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB4_JAR, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @Test
    public void testSynchGetShareClassLoader() {
        doTestGetShareClassLoader("TEST_SYNC");
    }

    @Test
    public void testAsynchGetShareClassLoader() {
        doTestGetShareClassLoader("TEST_ASYNC");
    }

    void doTestGetShareClassLoader(String test) {
        String result = server.waitForStringInLog(test + " getSharedLibraryClassLoader -");
        assertNotNull(test + " not found", result);
        assertTrue(result, result.contains("SUCCESS"));
    }

    @AfterClass
    public static void stopServer() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.uninstallSystemFeature("testLibraryUser-1.0");
            assertFalse("Failed to clean up installed file: lib/features/testLibraryUser-1.0",
                        server.fileExistsInLibertyInstallRoot("lib/features/testLibraryUser-1.0.mf"));
            server.uninstallSystemBundle("test.library.user");
            assertFalse("Failed to clean up installed file: lib/test.library.user.jar", server.fileExistsInLibertyInstallRoot("lib/test.library.user.jar"));
        }
    }
}
