/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.config;

import java.util.Set;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.config.web.DataConfigTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataConfigTest extends FATServletClient {

    private static final String APP_NAME = "DataConfigTestApp";

    private static ServerConfiguration savedConfig;

    @Server("io.openliberty.data.internal.fat.config")
    @TestServlet(servlet = DataConfigTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("DataConfigTestApp", "test.jakarta.data.config.web");
        ShrinkHelper.exportAppToServer(server, war);
        savedConfig = server.getServerConfiguration().clone();
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Tests configuration for dropping and creating tables.
     */
    @Test
    public void testDropAndCreateTables() throws Exception {
        // start with createTables=true dropTables=false
        runTest(server, APP_NAME, "testEntitiesCanBeAdded&invokedBy=testDropAndCreateTables_1");

        // switch to createTables=false dropTables=false
        ServerConfiguration config = server.getServerConfiguration();
        DatabaseStore defaultDatabaseStore = config.getDatabaseStores().getById("defaultDatabaseStore");
        defaultDatabaseStore.setCreateTables("false");
        // TODO can application restart be triggered automatically?
        Application app = config.getApplications().getBy("location", "DataConfigTestApp.war");
        app.setExtraAttribute("forceRestart", "1");
        // save
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));
        try {
            // Data must remain because tables were not dropped
            runTest(server, APP_NAME, "testEntitiesFound&invokedBy=testDropAndCreateTables_2");

            // switch to createTables=true dropTables=true
            defaultDatabaseStore.setCreateTables("true");
            defaultDatabaseStore.setExtraAttribute("dropTables", "true"); // TODO use .setDropTables if this config gets added permanently
            app.setExtraAttribute("forceRestart", "2"); // TODO can application restart be triggered automatically?
            // save
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));

            // tables must be empty because they were dropped and recreated
            runTest(server, APP_NAME, "testEntitiesCanBeAdded&invokedBy=testDropAndCreateTables_3");

            // switch to createTables=false dropTables=true
            defaultDatabaseStore.setCreateTables("false");
            app.setExtraAttribute("forceRestart", "3"); // TODO can application restart be triggered automatically?
            // save
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));

            // tables must not exist
            runTest(server, APP_NAME, "testEntitiesDoNotHaveTables&invokedBy=testDropAndCreateTables_4");
        } finally {
            // restore to original
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(savedConfig);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));
        }

        // tables must be empty after being recreated
        runTest(server, APP_NAME, "testEntitiesNotFound&invokedBy=testDropAndCreateTables_5");
    }
}
