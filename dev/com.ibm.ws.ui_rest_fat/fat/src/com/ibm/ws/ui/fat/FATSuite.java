/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.ui.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.rest.APITest;
import com.ibm.ws.ui.fat.rest.v1.APIv1Test;
import com.ibm.ws.ui.fat.rest.v1.CatalogAPIv1Test;
import com.ibm.ws.ui.fat.rest.v1.CatalogAPIv1_MultiThreadedTest;
import com.ibm.ws.ui.fat.rest.v1.CatalogPersistenceTest;
import com.ibm.ws.ui.fat.rest.v1.FeatureToolServiceTest;
import com.ibm.ws.ui.fat.rest.v1.IconRestHandlerTest;
import com.ibm.ws.ui.fat.rest.v1.ToolboxAPIv1Test;
import com.ibm.ws.ui.fat.rest.v1.ToolboxAPIv1_MultiThreadedTest;
import com.ibm.ws.ui.fat.rest.v1.ToolboxPersistenceTest;
import com.ibm.ws.ui.fat.rest.v1.TooldataAPIv1Test;
import com.ibm.ws.ui.fat.rest.v1.TooldataPersistenceTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.annotation.Server;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({
                TooldataAPIv1Test.class,
                TooldataPersistenceTest.class,
                APITest.class,
                APIv1Test.class,
                CatalogAPIv1Test.class,
                CatalogAPIv1_MultiThreadedTest.class,
                CatalogPersistenceTest.class,
                FeatureToolServiceTest.class,
                ToolboxAPIv1Test.class,
                ToolboxAPIv1_MultiThreadedTest.class,
                ToolboxPersistenceTest.class,
                IconRestHandlerTest.class
})

@Mode(TestMode.LITE)
public class FATSuite {
    @ClassRule
    public static RepeatTests r;
    static {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");
        if (!isWindows) {
            r = RepeatTests.with(new EmptyAction().fullFATOnly())
            		.andWith(new FeatureReplacementAction("restConnector-1.0", "restConnector-2.0").fullFATOnly())
            		.andWith(FeatureReplacementAction.EE9_FEATURES().alwaysAddFeature("servlet-5.0")
            				.conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
            		.andWith(FeatureReplacementAction.EE10_FEATURES().alwaysAddFeature("servlet-6.0"));
        } else {
            // On Windows only run each repeat scenario on lite or full mode so
            // that both do not run on Windows.
            r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new FeatureReplacementAction("restConnector-1.0", "restConnector-2.0").fullFATOnly()).andWith(FeatureReplacementAction.EE10_FEATURES().alwaysAddFeature("servlet-6.0").liteFATOnly());
        }
    }
    private static final Class<?> c = FATSuite.class;
    public static final String RESOURCES_ADMIN_CENTER_1_0 = "resources/adminCenter-1.0/";
    @Server("com.ibm.ws.ui.fat")
    public static LibertyServer server = null;

    @BeforeClass
    public static void suiteSetUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.ui.fat");

        Log.info(c, "suiteSetUp", "Removing persistence directory: " + RESOURCES_ADMIN_CENTER_1_0);
        server.deleteDirectoryFromLibertyServerRoot(RESOURCES_ADMIN_CENTER_1_0);

        assertNotNull("The test server failed to start. Aborting test setup",
                      server.startServer());
        assertNotNull("The server did not report that HTTPS has started. Aborting test setup",
                      server.waitForStringInLog("CWWKO0219I:.*ssl.*"));
        assertNotNull("The server did not report the UI app (adminCenter) has started. Aborting test setup",
                      server.waitForStringInLog("CWWKT0016I:.*/adminCenter"));
        assertNotNull("The server did not report the ibm/api application was ready. Aborting test setup",
                      server.waitForStringInLog("CWWKT0016I:.*ibm/api/.*"));
        assertNotNull("The server did not report 'The server uiDev is ready to run a smarter planet'. Aborting test setup",
                      server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void suiteTearDown() throws Exception {
        if (server != null) {
            if (server.isStarted()) {
                server.stopServer("CWWKF0002E: A bundle could not be found for com.ibm.ws.appserver.*",
                            "CWWKE0702E: Could not resolve module: com.ibm.ws.appserver.*",
                            "CWWKF0029E: Could not resolve module: com.ibm.ws.appserver.*",
                            "CWWKF0001E: A feature definition could not be found for scopedprodextn.*",
                            "CWWKX1009E:.*");
            }

            Log.info(c, "suiteTearDown", "Removing persistence directory: " + RESOURCES_ADMIN_CENTER_1_0);
            server.deleteDirectoryFromLibertyServerRoot(RESOURCES_ADMIN_CENTER_1_0);
        }
    }

}
