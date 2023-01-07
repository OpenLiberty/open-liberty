/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.concurrent.sim.zos.context;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                SimZOSContextProviderTest.class
})
public class FATSuite {
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.fat.zcontext");

    /**
     * Pre-bucket execution setup.
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Install user feature
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/simulatedZOSContextProviders-1.0.mf");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.concurrent.sim.zos.syncToOSThread.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.concurrent.sim.zos.wlm.jar");

        // Install internal feature that allows access to the thread context provider SPI
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatures/contextProviderSPI-1.0.mf");
    }

    /**
     * Post-bucket execution setup.
     */
    @AfterClass
    public static void cleanUpSuite() throws Exception {
        // Remove the user extension added during the build process.
        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");

        // Remove the internal feature
        server.deleteFileFromLibertyInstallRoot("lib/features/contextProviderSPI-1.0.mf");
    }
}
