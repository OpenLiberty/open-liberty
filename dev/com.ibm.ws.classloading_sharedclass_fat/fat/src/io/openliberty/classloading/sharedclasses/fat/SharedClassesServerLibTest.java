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
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.deleteSharedClassCache;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.runSharedClassTest;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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

    @BeforeClass
    public static void setupTestApp() throws Exception {
        ShrinkHelper.exportAppToServer(server, SHARED_CLASSES_WAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "libs", SHARED_CLASSES_SERVER_LIB, DeployOptions.OVERWRITE);
    }

    @Before
    public void startTestServer() throws Exception {
        deleteSharedClassCache(server);
        server.startServer(getTestMethodSimpleName() + ".log");
    }

    private void runTest() throws Exception {
        runSharedClassTest(server, SHARED_CLASSES_SERVER_LIB_PATH, getTestMethodSimpleName());
    }

    @Test
    public void testServerLibClassesA() throws Exception {
        runTest();
    }

    @Test
    public void testServerLibClassesB() throws Exception {
        runTest();
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }
}
