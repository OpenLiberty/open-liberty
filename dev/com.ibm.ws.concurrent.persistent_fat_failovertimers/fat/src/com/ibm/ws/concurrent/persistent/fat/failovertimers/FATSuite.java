/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.failovertimers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.database.DerbyNetworkUtilities;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
    FailoverTimersTest.class
    })
public class FATSuite {
    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Remove databases that were created by previous executions of this test bucket when running with Derby.
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.failovertimers.serverA");
        server.deleteDirectoryFromLibertyInstallRoot("usr/shared/resources/data/failovertimersdb");
        server.deleteDirectoryFromLibertyInstallRoot("usr/shared/resources/data/failovertimers2db");

        DerbyNetworkUtilities.startDerbyNetwork();
    }

    @AfterClass
    public static void afterSuite() throws Exception {
        DerbyNetworkUtilities.stopDerbyNetwork();
    }
}