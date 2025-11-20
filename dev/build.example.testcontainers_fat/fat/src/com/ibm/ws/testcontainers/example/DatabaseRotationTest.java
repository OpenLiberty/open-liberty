/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package com.ibm.ws.testcontainers.example;

import static componenttest.annotation.SkipIfSysProp.DB_Not_Default;
import static componenttest.annotation.SkipIfSysProp.DB_Postgres;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
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

    public static final String APP_NAME = "app";

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
         * This builder method will edit each <dataSource> element in your server.xml
         * that has the attribute fat.modify="true".
         *
         * It will remove the old properties and replace them with the connection
         * properties for the specific database we are running against.
         *
         * If you want to replace with a generic <properties .. /> element use:
         * DatabaseContainerUtil.build(server, jdbcContainer)
         * .withGenericProperties()
         * .modify
         */
        DatabaseContainerUtil.build(server, jdbcContainer)
                        .withDatabaseProperties()
                        .withDriverVariable() //Add DB_DRIVER variable to the server to tell the server where to look for the jdbc driver.
                        .modify();

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * <pre>
     * If a test is not valid for a certain Database driver, then
     * you can skip this test for that database by using the
     * &#64;SkipIfSysProp annotation.
     * </pre>
     */
    @Test
    @SkipIfSysProp(DB_Postgres)
    public void testSkipForPostgreSQL() throws Exception {
        assertTrue(System.getProperty("fat.bucket.db.type") != "Postgres");
    }

    /**
     * <pre>
     * Exclude test from DB rotation.
     * This test won't run when a different database is configured.
     * Use: &#64;SkipIfSysProp(DB_Default)
     * </pre>
     */
    @Test
    @SkipIfSysProp(DB_Not_Default)
    public void testExcludeFromRotation() throws Exception {
        assertNull(System.getProperty("fat.bucket.db.type"));
    }

    /**
     * <pre>
     * If a test is only valid for a certain Database driver, then
     * you can skip all other databases for for this test by using the
     * &#64;OnlyIfSysProp annotation.
     * </pre>
     */
    @Test
    @OnlyIfSysProp(DB_Postgres)
    public void testSkipUnlessPostgreSQL() throws Exception {
        assertEquals("Postgres", System.getProperty("fat.bucket.db.type"));
    }

    /**
     * <pre>
     * Run test only on enterprise databases.
     * This test won't run unless a different database is configured.
     * Use: &#64;OnlyIfSysProp(DB_Not_Default)
     * </pre>
     */
    @Test
    @OnlyIfSysProp(DB_Not_Default)
    public void testOnlyOnRotation() throws Exception {
        assertNotNull(System.getProperty("fat.bucket.db.type"));
    }

}
