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

import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testGetCommonResource_NoFilter8_NoFilter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testGetCommonResourcesOrder_NoFilter8_NoFilter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testGetPlatformResourceDoesExist;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testGetPlatformResourceDoesNotExist_NoFilter8_Filter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testGetPlatformResourcesDoesExist_NoFilter8_Filter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testGetPlatformResourcesDoesNotExist_NoFilter8_Filter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testLoadKernelClass_Found_NoFilter8_NoFilter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testLoadLibrary6Class_NoFilter8_Filter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testLoadLibrary7Class_NoFilter8_NoFilter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testLoadLibrary8Class_NoFilter8_Filter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testLoadLibrary9Class_NoFilter8_NoFilter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testLoadPlatformClassDoesExist;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testLoadPlatformClassDoesNotExist_NoFilter8_Filter9;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testLoadPlatformXAException;
import static io.openliberty.classloading.classpath.fat.AppParentDelegationAbstractTest.CheckTrace.testPlatformService;
import static io.openliberty.classloading.classpath.fat.FATSuite.APP_PARENT_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_PLATFORM_DELEGATION_APP;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.classloading.platform.delegation.test.app.PlatformDelegationTestServlet;
import junit.framework.AssertionFailedError;

/**
 *
 */
@RunWith(FATRunner.class)
public class AppParentDelegationSystemConfigPackagesTest extends AppParentDelegationAbstractTest {

    @Server(APP_PARENT_TEST_SERVER)
    @TestServlet(servlet = PlatformDelegationTestServlet.class, contextRoot = TEST_PLATFORM_DELEGATION_APP)
    public static LibertyServer server;

    @BeforeClass
    public static void setupTestServer() throws Exception {
        setAppParent(server, CONFIG_APP_PARENT_SYSTEM, "io.openliberty.classloading.test.resources, io.openliberty.classloading.classpath.test.lib7, io.openliberty.classloading.classpath.test.lib9");
        setupTestServer(server);
    }

    @After
    public void checkTestTrace() throws Exception {
        getCheckTrace().test(server);
    }

    private CheckTrace getCheckTrace() {
        String testMethod = testName.getMethodName();
        testMethod = testMethod.substring(PlatformDelegationTestServlet.class.getSimpleName().length() + 1);
        switch (testMethod) {
            case "testLoadLibrary6Class":
                return testLoadLibrary6Class_NoFilter8_Filter9;
            case "testLoadLibrary7Class":
                return testLoadLibrary7Class_NoFilter8_NoFilter9;
            case "testLoadLibrary8Class":
                return testLoadLibrary8Class_NoFilter8_Filter9;
            case "testLoadLibrary9Class":
                return testLoadLibrary9Class_NoFilter8_NoFilter9;
            case "testGetCommonResource":
                return testGetCommonResource_NoFilter8_NoFilter9;
            case "testGetCommonResourcesOrder":
                return testGetCommonResourcesOrder_NoFilter8_NoFilter9;
            case "testGetPlatformResourceDoesExist" :
                return testGetPlatformResourceDoesExist;
            case "testGetPlatformResourceDoesNotExist":
                return testGetPlatformResourceDoesNotExist_NoFilter8_Filter9;
            case "testGetPlatformResourcesDoesExist":
                return testGetPlatformResourcesDoesExist_NoFilter8_Filter9;
            case "testGetPlatformResourcesDoesNotExist":
                return testGetPlatformResourcesDoesNotExist_NoFilter8_Filter9;
            case "testLoadPlatformClassDoesExist":
                return testLoadPlatformClassDoesExist;
            case "testLoadPlatformClassDoesNotExist":
                return testLoadPlatformClassDoesNotExist_NoFilter8_Filter9;
            case "testLoadKernelClass":
                return testLoadKernelClass_Found_NoFilter8_NoFilter9;
            case "testPlatformService":
                return testPlatformService;
            case "testLoadPlatformXAException":
                return testLoadPlatformXAException;
            default:
               fail("Unknown test method: " + testMethod);
        }
        // should not get here
        throw new AssertionFailedError("Illegal state: " + testMethod);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer();
    }
}
