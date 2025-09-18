/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.concurrent.no.vt;

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
                ConcurrentVirtualThreadsDisabledTest.class
})
public class FATSuite {
    public static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("com.ibm.ws.concurrent.fat.no.vt");

    /**
     * Pre-bucket execution setup.
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Copy internal feature bundle to Liberty
        server.copyFileToLibertyInstallRoot("lib/",
                                            "bundles/test.concurrent.no.vt.disabler.jar");

        // Add internal feature mf file to Liberty
        server.copyFileToLibertyInstallRoot("lib/features",
                                            "internalFeatures/virtualThreadDisabler-1.0.mf");
    }

    /**
     * Post-bucket execution setup.
     */
    @AfterClass
    public static void cleanUpSuite() throws Exception {
        // Remove the internal feature bundle
        server.deleteFileFromLibertyInstallRoot("lib/test.concurrent.no.vt.disabler.jar");

        // Remove the internal feature mf file
        server.deleteFileFromLibertyInstallRoot("lib/features/virtualThreadDisabler-1.0.mf");
    }
}
