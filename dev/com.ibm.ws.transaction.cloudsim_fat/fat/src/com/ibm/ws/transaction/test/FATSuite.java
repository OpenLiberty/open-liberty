/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                SimpleFS2PCCloudTest.class,
                Simple2PCCloudTest.class,
                DualServerDynamicFSTest.class,
                DualServerDynamicDBTest.class,
                DualServerPeerLockingTest.class
})
public class FATSuite {

    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("com.ibm.ws.transaction_FSCLOUD001");
    private static LibertyServer server2 = LibertyServerFactory.getLibertyServer("com.ibm.ws.transaction_FSCLOUD002");
    // We don't repeat these tests, when they run in full mode doubling the number of tests can mean that the whole
    // suite can take longer than the 3 hour threshold for a FAT suite.
    //
    // So run the suite with the EE8 Feature set only.

    @BeforeClass
    public static void beforeSuite() throws Exception {

        // Install user feature
        server1.copyFileToLibertyInstallRoot("lib/features/", "features/txfat-1.0.mf");
        server2.copyFileToLibertyInstallRoot("lib/features/", "features/txfat-1.0.mf");

        // Install bundle for txfat feature
        server1.copyFileToLibertyInstallRoot("lib/", "bundles/com.ibm.ws.transactions.fat.utils.jar");
        server2.copyFileToLibertyInstallRoot("lib/", "bundles/com.ibm.ws.transactions.fat.utils.jar");
    }
}
