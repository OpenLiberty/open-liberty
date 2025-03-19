/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.commons.logging.test.app.TestCommonsLogging;

@RunWith(FATRunner.class)
public class CommonsLoggingTCCLTest extends FATServletClient {

    public static final String APP_NAME = "testCommonsLoggingServlet";
    private static final String SYSTEM_FEATURE_PATH = "lib/features/";
    private static final String SYSTEM_BUNDLE_PATH = "lib/";
    private static final String TEST_MF = "test.commons.logging-1.0.mf";
    private static final String TEST_JAR = "io.openliberty.commons.logging.test.bundle.jar";
    private static final String SYSTEM_FEATURE_TEST_MF = "features/" + TEST_MF;
    private static final String SYSTEM_FEATURE_TEST_JAR = "bundles/" + TEST_JAR;

    @Server("commonsLoggingServer")
    @TestServlet(servlet = TestCommonsLogging.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void addTestFeature() throws Exception {
        server.copyFileToLibertyInstallRoot(SYSTEM_FEATURE_PATH, SYSTEM_FEATURE_TEST_MF);
        server.copyFileToLibertyInstallRoot(SYSTEM_BUNDLE_PATH, SYSTEM_FEATURE_TEST_JAR);
        ShrinkHelper.defaultDropinApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, //
                                      TestCommonsLogging.class.getPackage().getName(), //
                                      "org.apache.commons.logging", //
                                      "org.apache.commons.logging.impl");
        server.startServer();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.deleteFileFromLibertyInstallRoot(SYSTEM_FEATURE_PATH + TEST_MF);
            server.deleteFileFromLibertyInstallRoot(SYSTEM_BUNDLE_PATH + TEST_JAR);
        }
    }

}
