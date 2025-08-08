/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.LIB_PRECEDENCE_BEFORE_APP_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DUMMY_RAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB1_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB2_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB3_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB4_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB_PRECECENCE_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB_PRECENCENC_WAR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.classloading.library.precedence.test.app.LibPrecedenceBeforeAppTestServlet;

@RunWith(FATRunner.class)
public class LibraryPrecedenceBeforeAppTests extends FATServletClient {

    @Server(LIB_PRECEDENCE_BEFORE_APP_SERVER)
    @TestServlet(servlet = LibPrecedenceBeforeAppTestServlet.class, contextRoot = TEST_LIB_PRECECENCE_APP)
    public static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setupTestServer() throws Exception {
        server.installSystemFeature("apiTestFeature-1.0");
        assertTrue("apiTestFeature-1.0 should have been lib/features",
                   server.fileExistsInLibertyInstallRoot("lib/features/apiTestFeature-1.0.mf"));
        server.installSystemBundle("test.bundle.api");
        assertTrue("test.bundle.threading.jar should have been copied to lib",
                   server.fileExistsInLibertyInstallRoot("lib/test.bundle.api.jar"));

        ShrinkHelper.exportAppToServer(server, TEST_LIB_PRECENCENC_WAR, DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB1_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB2_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB3_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB4_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/ras", TEST_DUMMY_RAR, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.uninstallSystemFeature("apiTestFeature-1.0");
            assertFalse("Failed to clean up installed file: lib/features/apiTestFeature-1.0",
                        server.fileExistsInLibertyInstallRoot("lib/features/apiTestFeature-1.0.mf"));
            server.uninstallSystemBundle("test.bundle.api");
            assertFalse("Failed to clean up installed file: lib/test.bundle.api.jar", server.fileExistsInLibertyInstallRoot("lib/test.bundle.api.jar"));
        }
    }

    @AfterClass
    public static void removeTestFeatures() throws Exception {
    }
}
