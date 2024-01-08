/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package tests;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;

@Mode
@RunWith(FATRunner.class)
@AllowedFFDC(value = { "java.sql.SQLRecoverableException", "javax.resource.spi.ResourceAllocationException", "java.sql.SQLNonTransientConnectionException" })
public class DualServerDynamicDBRotationTest2 extends DualServerDynamicCoreTest2 {

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001")
    public static LibertyServer firstServer;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002")
    public static LibertyServer secondServer;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD002",
    };

    public static JdbcDatabaseContainer<?> testContainer;

    @Override
    public void setUp(LibertyServer server) throws Exception {
        setupDriver(server);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        setup(firstServer, secondServer, "Simple2PCCloudServlet", "cloud001");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        dropTables();
    }

    @After
    public void tearDown() throws Exception {
        tidyServersAfterTest(server1, server2);
    }
}
