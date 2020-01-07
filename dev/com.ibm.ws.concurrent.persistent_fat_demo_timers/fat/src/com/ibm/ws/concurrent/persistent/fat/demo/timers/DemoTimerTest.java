/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.concurrent.persistent.fat.demo.timers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import ejb.timers.PersistentDemoTimersServlet;

/**
 * This test suite start's an application that has automated timers,
 * and scheduled timers that will perform some sort of in memory data manipulation.
 * This is to simulate the situation where customers use timers to do something
 * like unit conversions, data processing, etc.
 *
 * These timers will run every half second. That sort of frequency is the
 * maximum we would ever expect a customer to run a timer that is doing
 * in memory work.
 */
@RunWith(FATRunner.class)
public class DemoTimerTest extends FATServletClient {

    public static final String APP_NAME = "demotimer";

    @Server("com.ibm.ws.concurrent.persistent.fat.demo.timers")
    @TestServlet(servlet = PersistentDemoTimersServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @BeforeClass
    public static void setUp() throws Exception {
        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        if (DatabaseContainerType.valueOf(testContainer) == DatabaseContainerType.Oracle) {
            //Type=javax.sql.ConnectionPoolDataSource to avoid
            //  ORA-02089: COMMIT is not allowed in a subordinate session
            //  when application tries to create a new table using oracle.
            server.addEnvVar("DS_TYPE", "javax.sql.ConnectionPoolDataSource");
        } else {
            server.addEnvVar("DS_TYPE", "javax.sql.XADataSource");
        }

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        //Install App
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "ejb.timers");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKC1501W", "CWWKC1511W");
    }
}