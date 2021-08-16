/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.dbrotationtests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.ws.transaction.test.tests.DualServerDynamicCoreTest1;
import com.ibm.ws.transaction.web.Simple2PCCloudServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;

@Mode
@RunWith(FATRunner.class)
public class DualServerDynamicDBRotationTest1 extends DualServerDynamicCoreTest1 {

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer firstServer;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer secondServer;

    public static JdbcDatabaseContainer<?> testContainer;

    @Override
    public void setUp(LibertyServer server) throws Exception {
        setupDriver(server);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        setup(firstServer, secondServer, "Simple2PCCloudServlet", "cloud001");
    }

    @After
    public void tearDown() throws Exception {
        tidyServersAfterTest(server1, server2);
    }
}
