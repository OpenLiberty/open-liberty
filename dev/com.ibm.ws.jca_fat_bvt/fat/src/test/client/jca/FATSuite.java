/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package test.client.jca;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                JCABVTTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction())
                    .andWith(new JakartaEE9Action().withID("EE9").conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11));

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.bvt");

    /**
     * Pre-bucket execution setup.
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Install user feature
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/jcabvtbundle-1.0.mf");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.jca.fat.bvt.bundle.jar");

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
