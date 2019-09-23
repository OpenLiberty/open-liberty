/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.feature;

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
@SuiteClasses( {ConcurrentPersistentFeatureFATTest.class })
public class FATSuite {
	
	/**
	 * This bucket's Liberty server instance.
	 */
	 public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.feature.fat");
	
     /**
      * liberty helper feature installation info.
      */
	 public static final String LIBERTY_FEATURE_PATH = "lib/features/";
	 public static final String LIBERTY_FEATURE_MF_TEST_PATH = "features/persistentExecutorTestFeature-1.0.mf";
	 public static final String LIBERTY_FEATURE_NAME = "persistentExecutorTestFeature-1.0.mf";
	 
     /**
      * User feature installation info.
      */
	 public static final String USER_FEATURE_PATH = "usr/extension/lib/features/";
	 public static final String USER_BUNDLE_PATH = "usr/extension/lib/";
	 public static final String USER_FEATURE_MF_FAT_PATH = "features/testFeature-1.0.mf";
	 public static final String USER_FEATURE_NAME = "testFeature-1.0.mf";
	 public static final String USER_BUNDLE_JAR_FAT_PATH = "bundles/test.concurrent.persistent.feature.jar";
	 public static final String USER_BUNDLE_JAR_NAME = "test.concurrent.persistent.feature.jar";
	    
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
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/cpefeature");
        
        // Install user feature 
        server.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, USER_FEATURE_MF_FAT_PATH);
        assertTrue("Product feature: " + USER_FEATURE_MF_FAT_PATH + " should have been copied to: " + USER_FEATURE_PATH,
                   server.fileExistsInLibertyInstallRoot(USER_FEATURE_PATH + USER_FEATURE_NAME));
        server.copyFileToLibertyInstallRoot(USER_BUNDLE_PATH, USER_BUNDLE_JAR_FAT_PATH);
        assertTrue("Product bundle: " + USER_BUNDLE_JAR_FAT_PATH + " should have been copied to: " + USER_BUNDLE_PATH,
                   server.fileExistsInLibertyInstallRoot(USER_BUNDLE_PATH + USER_BUNDLE_JAR_NAME));
        
        // Install liberty helper feature.
        server.copyFileToLibertyInstallRoot(LIBERTY_FEATURE_PATH, LIBERTY_FEATURE_MF_TEST_PATH);
        assertTrue("Product feature: " + LIBERTY_FEATURE_MF_TEST_PATH + " should have been copied to: " + LIBERTY_FEATURE_PATH,
                   server.fileExistsInLibertyInstallRoot(LIBERTY_FEATURE_PATH + LIBERTY_FEATURE_NAME));
    }

	/**
	 * Post-bucket execution setup.
	 * 
	 * @throws Exception
	 */
    @AfterClass
    public static void afterSuite() throws Exception {
    	// Remove the user extension added during the build process.
    	server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
    	
    	// Remove the feature installed under lib/features during the build process.
    	server.deleteFileFromLibertyInstallRoot("lib/features/persistentExecutorTestFeature-1.0.mf");
    }
}
