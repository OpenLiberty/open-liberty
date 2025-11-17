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

import static io.openliberty.classloading.classpath.fat.FATSuite.PARENT_LAST_LIBRARY_FEATURE_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB6_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_PARENT_LAST_LIBRARY_FEATURE_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_PARENT_LAST_LIBRARY_FEATURE_WAR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.classloading.feature.message.test.app.ParentLastFeatureMessageTestServlet;

/**
 * Tests using parentLast delegation with a configured library.
 * Verifies we do not log unexpected messages about missing classes and possible features to enable.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel=11)
public class ParentLastLibraryFeatureTests extends FATServletClient {

    @Server(PARENT_LAST_LIBRARY_FEATURE_TEST_SERVER)
    @TestServlet(servlet = ParentLastFeatureMessageTestServlet.class, contextRoot = TEST_PARENT_LAST_LIBRARY_FEATURE_APP)
    public static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setupTestServer() throws Exception {
        ShrinkHelper.exportAppToServer(server, TEST_PARENT_LAST_LIBRARY_FEATURE_WAR, DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB6_JAR, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @After
    public void checkForCWWKL0084W() {
        String methodName = testName.getMethodName();
        if (methodName.contains("testNo")) {
            String result = server.waitForStringInLog("CWWKL0084W:.* javax.transaction.Transaction", 100);
            assertNull("Found CWWKL0084W in log: " + result, result);
        } else if (methodName.contains("testYes")) {
            String result = server.waitForStringInLog("CWWKL0084W:.* javax.xml.bind.TriggerNotFound.*jaxb-2.2", 100);
            assertNotNull("Did not find CWWKL0084W in log", result);
        } else {
            fail("unknown test method " + methodName);
        }
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer();
    }
}
