/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.fat;

import static componenttest.topology.utils.FATServletClient.runTest;

import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class FixManagerTest {

    private static final Class<?> c = FixManagerTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fix.manager");

    private static final String BUNDLE_NAME = "ProvisioningTestBundle";
    private static final String FEATURE_MANAGER_CONTEXT_ROOT = "feature/fixManagerServlet";
    private static final String FEATURE_PATH = "lib/features/";
    private static final String FEATURE_MF = "test.featurefixmanager-1.0.mf";
    private static final String INTERIM_FIX_BUNDLE_NAME = "ProvisioningInterimFixesTestBundle";
    private static final String INTERIM_FIX_FEATURE = "test.InterimFixManagerTest-1.0.mf";
    private static final String INTERIM_FIXES_FEATURE = "test.InterimFixesManagerTest-1.0.mf";
    private static final String TEST_FIX_FEATURE = "test.TestFixManagerTest-1.0.mf";

    private static void deleteTestBundleJars() throws Exception {
        final String METHOD_NAME = "deleteTestBundleJars";

        List<String> filenames;
        filenames = server.listLibertyInstallRoot("lib", BUNDLE_NAME);
        Log.info(c, METHOD_NAME, "found " + filenames.size() + " bundle jars to delete.");
        for (String filename : filenames) {
            server.deleteFileFromLibertyInstallRoot("lib/" + filename);
        }
        filenames = server.listLibertyInstallRoot("lib", BUNDLE_NAME);
        Log.info(c, METHOD_NAME, "found " + filenames.size() + " bundle jars after delete.");
        filenames = server.listLibertyInstallRoot("lib", INTERIM_FIX_BUNDLE_NAME);
        Log.info(c, METHOD_NAME, "found " + filenames.size() + " bundle jars to delete.");
        for (String filename : filenames) {
            server.deleteFileFromLibertyInstallRoot("lib/" + filename);
        }
        filenames = server.listLibertyInstallRoot("lib", INTERIM_FIX_BUNDLE_NAME);
        Log.info(c, METHOD_NAME, "found " + filenames.size() + " bundle jars after delete.");
    }

    @BeforeClass
    public static void setup() throws Exception {

        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/lib", "publish/bundles/test.feature.fix.manager.jar");
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, FEATURE_MF);
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, INTERIM_FIX_FEATURE);
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, INTERIM_FIXES_FEATURE);
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, TEST_FIX_FEATURE);
        server.saveServerConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        final String METHOD_NAME = "tearDown";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted()) {
            server.stopServer();
        }

        server.restoreServerConfiguration();

        deleteTestBundleJars();

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testEmptyString() throws Exception {
        server.startServer();
        runTest(server, FEATURE_MANAGER_CONTEXT_ROOT, "emptyFixList");
    }

    @Test
    public void testSingleIFixOutput() throws Exception {
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle4_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle4_1.0.0.20130101.jar");
        server.setServerConfigurationFile("singleInterimFixServer.xml");
        server.startServer();
        runTest(server, FEATURE_MANAGER_CONTEXT_ROOT, "singleIFix");

    }

    @Test
    public void testMultiIFixOutput() throws Exception {
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle1_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle1_1.0.0.20130101.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle2_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle2_1.0.0.20130101.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle3_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle3_1.0.0.20130101.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle4_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle4_1.0.0.20130101.jar");
        server.setServerConfigurationFile("interimFixesServer.xml");
        server.startServer();
        runTest(server, FEATURE_MANAGER_CONTEXT_ROOT, "multiIFixes");

    }

    @Test
    public void testSingleTFixOutput() throws Exception {
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningTestBundle_1.0.1.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningTestBundle_1.0.1.201204040001-TestAPAR0001.jar");
        server.setServerConfigurationFile("testFixesServer.xml");
        server.startServer();
        runTest(server, FEATURE_MANAGER_CONTEXT_ROOT, "singleTFix");

    }

    @Test
    public void testMultipleTFixOutput() throws Exception {
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningTestBundle_1.0.1.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningTestBundle_1.0.1.201204040001-TestAPAR0001-TestAPAR0002.jar");
        server.setServerConfigurationFile("testFixesServer.xml");
        server.startServer();
        runTest(server, FEATURE_MANAGER_CONTEXT_ROOT, "multiTFixes");

    }

}
