/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.work.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses(ConcurrentWorkFATTest.class)
public class FATSuite {
    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.fat.work");

    @BeforeClass
    public static void beforeSuite() throws Exception {        
        // Copy user feature bundle to Liberty
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.concurrent.work.jar");
        
        // Add user feature mf file to Liberty
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/workManager-1.0.mf");

        // Add internal feature mf file to Liberty
        server.copyFileToLibertyInstallRoot("lib/features/", "features/concurrencyExtension-1.0.mf");
    }

    @AfterClass
    public static void afterSuite() throws Exception {
        // Remove the user feature
        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
        
        // Remove the internal feature that we previously installed
        server.deleteFileFromLibertyInstallRoot("lib/features/concurrencyExtension-1.0.mf");
    }
}
