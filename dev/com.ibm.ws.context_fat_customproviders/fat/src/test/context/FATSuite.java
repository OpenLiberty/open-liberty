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
package test.context;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses(ContextServiceTest.class)
public class FATSuite {
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.context.fat.customproviders");

    /**
     * Pre-bucket execution setup.
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Install user feature
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/contexttest-1.0.mf");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/buffer.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/map.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/numeration.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/threadfactory.jar");

        // Install internal feature that allows access to the thread context provider
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatures/contextproviderinternals-1.0.mf");
    }

    /**
     * Post-bucket execution setup.
     */
    @AfterClass
    public static void cleanUpSuite() throws Exception {
        // Remove the user extension added during the build process.
        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");

        // Remove the internal feature
        server.deleteFileFromLibertyInstallRoot("lib/features/contextproviderinternals-1.0.mf");
    }
}