/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import app1.TestServletA;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class BellsTest extends FATServletClient {

    public static final String APP_NAME = "app1";

    public static final String SERVER_NAME = "checkpointBells";

    @Server(SERVER_NAME)
    @TestServlet(servlet = TestServletA.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public TestMethod testMethod;

    @ClassRule
    public static RepeatTests repeatTest = MicroProfileActions.repeat(SERVER_NAME, TestMode.FULL,
                                                                      MicroProfileActions.MP41, // first test in LITE mode
                                                                      // rest are FULL mode
                                                                      MicroProfileActions.MP50, MicroProfileActions.MP60);

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        buildAndExportBellLibrary(server, "app1", "AppInitializer");
        ShrinkHelper.defaultApp(server, APP_NAME, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        switch (testMethod) {
            case testBellsCheckpointAtDeployment:
                server.setCheckpoint(CheckpointPhase.DEPLOYMENT, false, null);
                break;
            case testBellsCheckpointAtApplication:
                server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
                break;
            case testHttpServletRequest:
                server.setCheckpoint(CheckpointPhase.APPLICATIONS, true, null);
                break;
            default:
                break;
        }
        server.startServer(getTestMethod(TestMethod.class, testName) + ".log");
    }

    @Test
    public void testBellsCheckpointAtDeployment() throws Exception {
        assertTrue("Expected message for bell service registration not found", !server.findStringsInLogs("CWWKL0050I").isEmpty());
        server.checkpointRestore();
        assertTrue("Bell service should have been consumed during restore", !server.findStringsInLogs("Inside Servlet Container Initializer...").isEmpty());
    }

    @Test
    public void testBellsCheckpointAtApplication() throws Exception {
        assertTrue("Expected message for bell service registration not found", !server.findStringsInLogs("CWWKL0050I").isEmpty());
        assertTrue("Bell service should have been consumed during checkpoint", !server.findStringsInLogs("Inside Servlet Container Initializer...").isEmpty());
        server.checkpointRestore();

    }

    private static void buildAndExportBellLibrary(LibertyServer targetServer, String archiveName, String... classNames) throws Exception {
        JavaArchive bellArchive = ShrinkHelper.buildJavaArchive(
                                                                archiveName,
                                                                new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
                                                                    @Override
                                                                    public boolean include(ArchivePath ap) {
                                                                        for (String cn : classNames)
                                                                            if (ap.get().endsWith(cn + ".class"))
                                                                                return true;
                                                                        return false;
                                                                    }
                                                                },
                                                                "app1");
        ShrinkHelper.exportToServer(targetServer, "sharedLib", bellArchive);
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

    static enum TestMethod {
        testBellsCheckpointAtDeployment,
        testBellsCheckpointAtApplication,
        testHttpServletRequest,
        unknown
    }

}
