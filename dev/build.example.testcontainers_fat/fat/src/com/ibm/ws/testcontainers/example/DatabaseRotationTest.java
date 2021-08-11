/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testcontainers.example;

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
import web.dbrotation.DbRotationServlet;

/**
 * Example test class showing how to setup a test class that can
 * be run against any of our supported databases.
 *
 * This workload is commonly referred to as "Database Rotation", but
 * it should be noted that the rotation workflow happens as part of our
 * SOE testing and not within the test infrastructure itself.
 */
@RunWith(FATRunner.class)
public class DatabaseRotationTest {

    public static final String APP_NAME = "containerApp";

    @Server("build.example.testcontainers.dbrotation")
    @TestServlet(servlet = DbRotationServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    /**
     * Here we are using a child class of GenericContainer called JdbcDatabaseContainer.
     * This class has extra workflows specific to database containers.
     * <br>
     *
     * The DatabaseContainerFactory is from fattest.simplicity and will setup and return
     * a container based on the fat.bucket.db.type property. This can be set either on the
     * commandline when doing ./gradlew \<project\>:buildandrun or on a build system.
     * <br>
     *
     * This is how our SOE builds run against each database.
     */
    @ClassRule
    public static JdbcDatabaseContainer<?> jdbcContainer = DatabaseContainerFactory.create();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "web.dbrotation");

        /*
         * This static method will edit each <dataSource> element in your server.xml
         * that has the attribute fat.modify="true".
         *
         * It will remove the old properties and replace them with the connection
         * properties for the specific database we are running against.
         *
         * If you want to replace with a generic <properties .. /> element use:
         * DatabaseContainerUtil.setupDataSourceProperties(server, jdbcContainer);
         */
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, jdbcContainer);

        /*
         * Add DB_DRIVER variable to the server to tell the server where to look for the jdbc driver.
         */
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(jdbcContainer).getDriverName());

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
