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

import static io.openliberty.classloading.classpath.fat.AppParentDelegationPlatformNullPackagesTest.getCheckTracePlatformNullPackages;
import static io.openliberty.classloading.classpath.fat.FATSuite.APP_PARENT_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_PLATFORM_DELEGATION_APP;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.classloading.platform.delegation.test.app.PlatformDelegationTestServlet;

/**
 *
 */
@RunWith(FATRunner.class)
public class AppParentDelegationPlatformEmptyPackagesTest extends AppParentDelegationAbstractTest {

    @Server(APP_PARENT_TEST_SERVER)
    @TestServlet(servlet = PlatformDelegationTestServlet.class, contextRoot = TEST_PLATFORM_DELEGATION_APP)
    public static LibertyServer server;

    @BeforeClass
    public static void setupTestServer() throws Exception {
        setAppParent(server, CONFIG_APP_PARENT_PLATFORM, "");
        setupTestServer(server);
    }

    @After
    public void checkTestTrace() throws Exception {
        String testMethodName = testName.getMethodName();
        testMethodName = testMethodName.substring(PlatformDelegationTestServlet.class.getSimpleName().length() + 1);
        getCheckTracePlatformNullPackages(testMethodName).test(server);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer();
    }
}
