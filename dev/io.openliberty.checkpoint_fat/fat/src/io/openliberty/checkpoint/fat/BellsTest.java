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
import static io.openliberty.checkpoint.fat.FATSuite.updateVariableConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class BellsTest extends FATServletClient {

    public static final String APP_NAME = "bells";

    public static final String SERVER_NAME = "checkpointBells";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public TestMethod testMethod;

    public static final String USER_BUNDLE_NAME = "test.checkpoint.bells.bundle";

    public static final String USER_FEATURE_NAME = "user.feature.checkpoint.bells-1.0";

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat(SERVER_NAME);

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        buildAndExportBellLibrary(server, "bells", "AppInitializer", "TestInterfaceImpl");
        server.installUserBundle(USER_BUNDLE_NAME);
        server.installUserFeature(USER_FEATURE_NAME);
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        switch (testMethod) {
            case testBellsCheckpointBeforeAppStart:
                server.setCheckpoint(CheckpointPhase.BEFORE_APP_START, false, null);
                break;
            case testBellsCheckpointAfterAppStart:
                server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
                break;
            case testUpdatedBellPropertiesBeforeRestore:
                server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
                break;
            default:
                break;
        }
        server.startServer(getTestMethod(TestMethod.class, testName) + ".log");
    }

    @Test
    public void testBellsCheckpointBeforeAppStart() throws Exception {
        server.checkpointRestore();
        assertEquals("Bell service registration for ServletContainerInitializer and TestInterface expected", 2, server.findStringsInLogs("CWWKL0050I").size());

        assertTrue("Bell service for TestInterface should have been consumed and injected with original properties during restore",
                   !server.findStringsInLogs("Updated bell properties: \\{bProp=orig_val\\}").isEmpty());
        assertTrue("Bell service for ServletContainerInitializer should have been consumed during restore",
                   !server.findStringsInLogs("Inside Servlet Container Initializer...").isEmpty());
    }

    @Test
    public void testBellsCheckpointAfterAppStart() throws Exception {
        assertEquals("Bell service registration for ServletContainerInitializer and TestInterface expected", 2, server.findStringsInLogs("CWWKL0050I").size());

        assertTrue("Bell service for TestInterface should have been consumed and injected with original properties during checkpoint",
                   !server.findStringsInLogs("Updated bell properties: \\{bProp=orig_val\\}").isEmpty());
        assertTrue("Bell service for ServletContainerInitializer should have been consumed during checkpoint",
                   !server.findStringsInLogs("Inside Servlet Container Initializer...").isEmpty());
        server.checkpointRestore();
    }

    @Test
    public void testUpdatedBellPropertiesBeforeRestore() throws Exception {
        assertTrue("Bell service for TestInterface should have been injected with original properties",
                   !server.findStringsInLogs("Updated bell properties: \\{bProp=orig_val\\}").isEmpty());
        updateVariableConfig(server, "bellProp", "updated_val");
        server.checkpointRestore();
        assertTrue("Bell service for TestInterface should have been injected with updated properties",
                   !server.findStringsInLogs("Updated bell properties: \\{bProp=updated_val\\}").isEmpty());
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
                                                                "bells");
        ShrinkHelper.exportToServer(targetServer, "sharedLib", bellArchive, DeployOptions.OVERWRITE);
    }

    @After
    public void afterTest() throws Exception {
        stopServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer();
        try {
            server.uninstallUserFeature(USER_FEATURE_NAME);
            server.uninstallUserBundle(USER_BUNDLE_NAME);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    static void stopServer() {
        if (server.isStarted()) {
            try {
                server.stopServer();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    static enum TestMethod {
        testBellsCheckpointBeforeAppStart,
        testBellsCheckpointAfterAppStart,
        testUpdatedBellPropertiesBeforeRestore,
        unknown
    }

}
