/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({ ThreadingExtensionFAT.class, MemLeakTest.class })
public class FATSuite {
    static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.threading_fat_server");

    /**
     * Installs any custom features necessary for this test.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void installTestFeatures() throws Exception {
        server.copyFileToLibertyInstallRoot("lib/features/", "features/threadingTestFeature-1.0.mf");
        assertTrue("threadingTestFeature-1.0.mf should have been copied to lib/features",
                   server.fileExistsInLibertyInstallRoot("lib/features/threadingTestFeature-1.0.mf"));
        server.copyFileToLibertyInstallRoot("lib/", "bundles/test.bundle.threading_1.0.0.jar");
        assertTrue("test.bundle.threading_1.0.0.jar should have been copied to lib",
                   server.fileExistsInLibertyInstallRoot("lib/test.bundle.threading_1.0.0.jar"));

        server.copyFileToLibertyInstallRoot("lib/features/", "features/threadingMemLeakTestFeature-1.0.mf");
        assertTrue("threadingMemLeakTestFeature-1.0.mf should have been copied to lib/features",
                   server.fileExistsInLibertyInstallRoot("lib/features/threadingMemLeakTestFeature-1.0.mf"));
        server.copyFileToLibertyInstallRoot("lib/", "bundles/test.bundle.threading.memleak_1.0.0.jar");
        assertTrue("test.bundle.threading.memleak_1.0.0.jar should have been copied to lib",
                   server.fileExistsInLibertyInstallRoot("lib/test.bundle.threading.memleak_1.0.0.jar"));
    }

    @AfterClass
    public static void removeTestFeatures() throws Exception {
        server.deleteFileFromLibertyInstallRoot("lib/features/threadingTestFeature-1.0.mf");
        assertFalse("Failed to clean up installed file: lib/features/threadingTestFeature-1.0.mf",
                    server.fileExistsInLibertyInstallRoot("lib/features/threadingTestFeature-1.0.mf"));
        server.deleteFileFromLibertyInstallRoot("lib/test.bundle.threading_1.0.0.jar");
        assertFalse("Failed to clean up installed file: lib/test.bundle.threading_1.0.0.jar", server.fileExistsInLibertyInstallRoot("lib/test.bundle.threading_1.0.0.jar"));

        server.deleteFileFromLibertyInstallRoot("lib/features/threadingMemLeakTestFeature-1.0.mf");
        assertFalse("Failed to clean up installed file: lib/features/threadingMemLeakTestFeature-1.0.mf",
                    server.fileExistsInLibertyInstallRoot("lib/features/threadingMemLeakTestFeature-1.0.mf"));
        server.deleteFileFromLibertyInstallRoot("lib/test.bundle.threading.memleak_1.0.0.jar");
        assertFalse("Failed to clean up installed file: lib/test.bundle.threading.memleak_1.0.0.jar",
                    server.fileExistsInLibertyInstallRoot("lib/test.bundle.threading.memleak_1.0.0.jar"));
    }
}
