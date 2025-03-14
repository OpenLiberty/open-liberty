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

import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR_NAME;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR_PATH;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR_TEST_SERVER;

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
import io.openliberty.classloading.sharedclasses.war.TestSharedClassesWar;

/**
 *
 */
@RunWith(FATRunner.class)
@OnlyIfSysProp("com.ibm.oti.shared.enabled")
public class SharedClassesWarTest extends FATServletClient {


    @Server(SHARED_CLASSES_WAR_TEST_SERVER)
    @TestServlet(servlet = TestSharedClassesWar.class, contextRoot = SHARED_CLASSES_WAR_NAME)
    public static LibertyServer server;

    @ClassRule
    public static SharedClassTestRule sharedClassTestRule = new SharedClassTestRule()
                        .setConsoleLogName(SharedClassesWarTest.class.getSimpleName())
                        .setServerSetup(SharedClassesWarTest::setupTestApp);

    public static LibertyServer setupTestApp(ServerMode mode) throws Exception {
        if (mode == ServerMode.storeInCache) {
            ShrinkHelper.exportAppToServer(server, SHARED_CLASSES_WAR, DeployOptions.SERVER_ONLY);
        }
        if (mode == ServerMode.modifyAppClasses) {
            Thread.sleep(5000);
            ShrinkHelper.exportAppToServer(server, SHARED_CLASSES_WAR, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);
        }
        return server;
    }

    private void runTest() throws Exception {
        sharedClassTestRule.runSharedClassTest(SHARED_CLASSES_WAR_PATH, getTestMethodSimpleName());
    }

    @Test
    public void testWarClassesA() throws Exception {
        runTest();
    }

    @Test
    public void testWarClassesB() throws Exception {
        runTest();
    }

    @Test
    public void testWarLibA() throws Exception {
        runTest();
    }

    @Test
    public void testWarLibB() throws Exception {
        runTest();
    }

}
