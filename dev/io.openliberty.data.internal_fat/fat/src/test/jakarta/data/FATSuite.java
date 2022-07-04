/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data;

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
                DataTest.class,
                TemplateTest.class
})
public class FATSuite {
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("io.openliberty.data.internal.fat");

    /**
     * Pre-bucket execution setup.
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        server.copyFileToLibertyInstallRoot("lib", "bundles/test.jakarta.data.jar");
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatures/data-1.0.mf");
    }

    /**
     * Post-bucket execution setup.
     */
    @AfterClass
    public static void cleanUpSuite() throws Exception {
        server.deleteFileFromLibertyInstallRoot("lib/test.jakarta.data.jar");
        server.deleteFileFromLibertyInstallRoot("lib/features/data-1.0.mf");
    }
}
