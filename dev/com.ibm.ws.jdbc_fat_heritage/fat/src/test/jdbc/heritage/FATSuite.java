/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jdbc.heritage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses(HeritageJDBCTest.class)
public class FATSuite {
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jdbc.heritage");

    /**
     * Pre-bucket execution setup.
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Install user feature
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/jdbcHeritage-1.0.mf");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/jdbcHeritage.jar");

        // Install internal feature that allows access to DataStoreHelper as though it were API
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatures/dataStoreHelper-1.0.mf");
    }

    /**
     * Post-bucket execution setup.
     */
    @AfterClass
    public static void cleanUpSuite() throws Exception {
        // Remove the user extension added during the build process.
        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");

        // Remove the internal feature
        server.deleteFileFromLibertyInstallRoot("lib/features/dataStoreHelper-1.0.mf");
    }
}