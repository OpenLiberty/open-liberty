/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
@SuiteClasses({ ThreadingExtensionFAT.class, MemLeakTest.class, ThreadingTest.class })
public class FATSuite {
    static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.threading_fat_server");

    /**
     * Installs any custom features necessary for this test.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void installTestFeatures() throws Exception {
        server.installUserFeature("threadingTestFeature-1.0");
        assertTrue("threadingTestFeature-1.0.mf should have been copied to usr/extension/lib/features",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/threadingTestFeature-1.0.mf"));
        server.installUserBundle("test.bundle.threading");
        assertTrue("test.bundle.threading.jar should have been copied to usr/extension/lib",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/test.bundle.threading.jar"));

        server.installUserFeature("threadingMemLeakTestFeature-1.0");
        assertTrue("threadingMemLeakTestFeature-1.0.mf should have been copied to usr/extension/lib/features",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/threadingMemLeakTestFeature-1.0.mf"));
        server.installUserBundle("test.bundle.threading.memleak");
        assertTrue("test.bundle.threading.memleak.jar should have been copied to usr/extension/lib",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/test.bundle.threading.memleak.jar"));

        server.installUserFeature("threadingExecutorHang-1.0");
        assertTrue("threadingExecutorHang-1.0.mf should have been copied to usr/extension/lib/features",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/threadingExecutorHang-1.0.mf"));
        server.installUserBundle("test.bundle.threading.hangtest");
        assertTrue("test.bundle.threading.hangtest.jar should have been copied to usr/extension/lib",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/test.bundle.threading.hangtest.jar"));
    }

    @AfterClass
    public static void removeTestFeatures() throws Exception {
        server.uninstallUserFeature("threadingTestFeature-1.0");
        assertFalse("Failed to clean up installed file: usr/extension/lib/features/threadingTestFeature-1.0.mf",
                    server.fileExistsInLibertyInstallRoot("lib/features/threadingTestFeature-1.0.mf"));
        server.uninstallUserBundle("test.bundle.threading");
        assertFalse("Failed to clean up installed file: usr/extension/lib/test.bundle.threading.jar", server.fileExistsInLibertyInstallRoot("lib/test.bundle.threading.jar"));

        server.uninstallUserFeature("threadingMemLeakTestFeature-1.0");
        assertFalse("Failed to clean up installed file: usr/extension/lib/features/threadingMemLeakTestFeature-1.0.mf",
                    server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/threadingMemLeakTestFeature-1.0.mf"));
        server.uninstallUserBundle("test.bundle.threading.memleak");
        assertFalse("Failed to clean up installed file: usr/extension/lib/test.bundle.threading.memleak.jar",
                    server.fileExistsInLibertyInstallRoot("usr/extension/lib/test.bundle.threading.memleak.jar"));

        server.uninstallUserFeature("threadingExecutorHang-1.0");
        assertFalse("Failed to clean up installed file: usr/extension/lib/features/threadingExecutorHang-1.0.mf",
                    server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/threadingExecutorHang-1.0.mf"));
        server.uninstallUserBundle("test.bundle.threading.hangtest");
        assertFalse("Failed to clean up installed file: usr/extension/lib/test.bundle.threading.hangtest.jar",
                    server.fileExistsInLibertyInstallRoot("usr/extension/lib/test.bundle.threading.hangtest.jar"));

    }
}
