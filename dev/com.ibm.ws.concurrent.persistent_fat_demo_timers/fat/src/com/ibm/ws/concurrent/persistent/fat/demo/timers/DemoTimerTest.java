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

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
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
    private static final Class<DemoTimerTest> c = DemoTimerTest.class;

    public static final String APP_NAME = "demotimer";

    @Server("com.ibm.ws.concurrent.persistent.fat.demo.timers")
    @TestServlet(servlet = PersistentDemoTimersServlet.class, path = APP_NAME)
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {

        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        //Install App
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "ejb.timers");

        //Start server
        server.startServer();

        //Application uses an XA datasource to perform database access.
        //Oracle restrictions creation/dropping of database tables using transactions with error:
        //  ORA-02089: COMMIT is not allowed in a subordinate session
        //Therefore, we will create the table prior to running tests when running against oracle.
        if (DatabaseContainerType.valueOf(testContainer) == DatabaseContainerType.Oracle) {
            final String createTable = "CREATE TABLE AUTOMATICDATABASE (name VARCHAR(64) NOT NULL PRIMARY KEY, count INT)";

            try (Connection conn = testContainer.createConnection("")) {
                try (PreparedStatement pstmt = conn.prepareStatement(createTable)) {
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                fail(c.getName() + " caught exception when initializing table: " + e.getMessage());
            }

        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKC1501W", "CWWKC1511W");
    }
}