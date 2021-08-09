/*******************************************************************************
 * Copyright (c) 2014, 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.delayexec;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.Machine;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({ DelayExecutionFATTest.class })
public class FATSuite {

    /**
     * This bucket's Liberty server instance.
     */
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.delayexec.fat");

    /**
     * Feature installation info.
     */
    public static final String FEATURE_PATH = "lib/features/";
    public static final String BUNDLE_PATH = "lib/";
    public static final String FEATURE_MF_FAT_PATH = "features/testFeature-1.0.mf";
    public static final String FEATURE_NAME = "testFeature-1.0.mf";
    public static final String BUNDLE_JAR_FAT_PATH = "bundles/test.concurrent.persistent.delayexec.jar";
    public static final String BUNDLE_JAR_NAME = "test.concurrent.persistent.delayexec.jar";

    /**
     * Pre-bucket execution setup.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Delete the Derby database that might be used by the persistent scheduled executor and the Derby-only test database
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/delayexec");

        // Install user feature
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, FEATURE_MF_FAT_PATH);
        assertTrue("Product feature: " + FEATURE_MF_FAT_PATH + " should have been copied to: " + FEATURE_PATH,
                   server.fileExistsInLibertyInstallRoot(FEATURE_PATH + FEATURE_NAME));
        server.copyFileToLibertyInstallRoot(BUNDLE_PATH, BUNDLE_JAR_FAT_PATH);
        assertTrue("Product bundle: " + BUNDLE_JAR_FAT_PATH + " should have been copied to: " + BUNDLE_PATH,
                   server.fileExistsInLibertyInstallRoot(BUNDLE_PATH + BUNDLE_JAR_NAME));

    }

    /**
     * Post-bucket execution setup.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void afterSuite() throws Exception {
        // Remove test feature
        server.deleteFileFromLibertyInstallRoot("lib/" + FEATURE_MF_FAT_PATH);
        server.deleteFileFromLibertyInstallRoot("lib/" + BUNDLE_JAR_NAME);
    }
}
