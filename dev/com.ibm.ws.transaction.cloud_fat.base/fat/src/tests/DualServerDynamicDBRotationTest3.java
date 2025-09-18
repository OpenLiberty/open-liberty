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
package tests;

import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@AllowedFFDC(value = { "java.sql.SQLRecoverableException", "javax.resource.spi.ResourceAllocationException", "java.sql.SQLNonTransientConnectionException",
                       "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
public class DualServerDynamicDBRotationTest3 extends DualServerDynamicCoreTest3 {

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001")
    public static LibertyServer s1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002")
    public static LibertyServer s2;

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
        setup(s1, s2, "Simple2PCCloudServlet", "cloud001");
    }

    @Before
    public void tearDown() throws Exception {
        serversToCleanup = Arrays.asList(s1, s2);
    }
}
