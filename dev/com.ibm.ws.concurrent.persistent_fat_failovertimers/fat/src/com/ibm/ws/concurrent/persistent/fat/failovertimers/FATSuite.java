/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.database.DerbyNetworkUtilities;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //Needed because we skip when testing against oracle.
                FailoverTimersTest.class
})
public class FATSuite {

    // By default run on DerbyClient and not DerbyEmbedded
    static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create(DatabaseContainerType.DerbyClient);

    @BeforeClass
    public static void beforeSuite() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client.
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();

        // Remove databases that were created by previous executions of this test bucket when running with Derby.
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.failovertimers.serverA");
        server.deleteDirectoryFromLibertyInstallRoot("usr/shared/resources/data/failovertimersdb");
        server.deleteDirectoryFromLibertyInstallRoot("usr/shared/resources/data/failovertimers2db");

        testContainer.start();

        DerbyNetworkUtilities.startDerbyNetwork();
    }

    @AfterClass
    public static void afterSuite() throws Exception {
        DerbyNetworkUtilities.stopDerbyNetwork();

        testContainer.stop();
    }
}