/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.sharedclasses.fat;

import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_LIB_TEST_SERVER;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_SERVER_LIB;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_SERVER_LIB_PATH;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR_NAME;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.classloading.sharedclasses.fat.SharedClassTestRule.ServerMode;
import io.openliberty.classloading.sharedclasses.war.TestSharedClassesServerLib;

/**
 *
 */
@RunWith(FATRunner.class)
@OnlyIfSysProp("com.ibm.oti.shared.enabled")
public class SharedClassesServerLibTest extends FATServletClient {


    @Server(SHARED_CLASSES_LIB_TEST_SERVER)
    @TestServlet(servlet = TestSharedClassesServerLib.class, contextRoot = SHARED_CLASSES_WAR_NAME)
    public static LibertyServer server;

    @ClassRule
    public static SharedClassTestRule sharedClassTestRule = new SharedClassTestRule()
                        .setConsoleLogName(SharedClassesServerLibTest.class.getSimpleName())
                        .setServerSetup(SharedClassesServerLibTest::setupTestApp);

    public static LibertyServer setupTestApp(ServerMode mode) throws Exception {
        if (mode == ServerMode.storeInCache) {
            ShrinkHelper.exportAppToServer(server, SHARED_CLASSES_WAR, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportToServer(server, "libs", SHARED_CLASSES_SERVER_LIB, DeployOptions.OVERWRITE);
        }
        if (mode == ServerMode.modifyAppClasses) {
            Thread.sleep(5000);
            ShrinkHelper.exportToServer(server, "libs", SHARED_CLASSES_SERVER_LIB, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);
        }
        return server;
    }

    private void runTest() throws Exception {
        sharedClassTestRule.runSharedClassTest(SHARED_CLASSES_SERVER_LIB_PATH, getTestMethodSimpleName());
    }

    @Test
    public void testServerLibClassesA() throws Exception {
        runTest();
    }

    @Test
    public void testServerLibClassesB() throws Exception {
        runTest();
    }
}
