/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.Data;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
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

    /**
     * Error messages to ignore. This is populated by tests when they introduce an
     * expected error and used and cleared by the next test to stop the server.
     */
    private static final List<String> ERRORS_TO_IGNORE = new ArrayList<String>();

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
        String[] expectedErrors = new String[ERRORS_TO_IGNORE.size()];
        expectedErrors = ERRORS_TO_IGNORE.toArray(expectedErrors);
        ERRORS_TO_IGNORE.clear();
        server.stopServer(expectedErrors);
    }

    /**
     * Adds the Jakarta Data feature while the server is running, after the
     * application has already been used. This needs to force a restart of the
     * application for Jakarta Data repositories to be processed and injected.
     */
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    @Test
    public void testAddJakartaDataFeature() throws Exception {
        // remove the data-1.0 feature
        ServerConfiguration config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().remove("data-1.0");

        String[] expectedErrors = new String[ERRORS_TO_IGNORE.size()];
        expectedErrors = ERRORS_TO_IGNORE.toArray(expectedErrors);
        ERRORS_TO_IGNORE.clear();
        server.stopServer(expectedErrors);

        server.updateServerConfiguration(config);

        server.startServer();
        ERRORS_TO_IGNORE.add("CWNEN0047W.*NoClassDefFoundError");

        runTest(server, APP_NAME,
                "testDoNotUseJakartaData&invokedBy=testAddJakartaDataFeature_1");

        // add the data-1.0 feature
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(savedConfig);
        server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));

        runTest(server, APP_NAME,
                "testEntitiesNotFound&invokedBy=testAddJakartaDataFeature_2");
    }

    /**
     * Tests configuration of databaseStore for dropping and creating tables.
     */
    @Test
    public void testDatabaseStoreDropAndCreateTables() throws Exception {
        // start with createTables=true dropTables=false
        runTest(server, APP_NAME, "testEntitiesCanBeAdded&invokedBy=testDatabaseStoreDropAndCreateTables_1");

        // switch to createTables=false dropTables=false
        ServerConfiguration config = server.getServerConfiguration();
        DatabaseStore myDataStore = config.getDatabaseStores().getById("MyDataStore");
        myDataStore.setCreateTables("false");
        // TODO can application restart be triggered automatically?
        Application app = config.getApplications().getBy("location", "DataConfigTestApp.war");
        app.setExtraAttribute("forceRestart", "1");
        // save
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));
        try {
            // Data must remain because tables were not dropped
            runTest(server, APP_NAME, "testEntitiesFound&invokedBy=testDatabaseStoreDropAndCreateTables_2");

            // switch to createTables=true dropTables=true
            myDataStore.setCreateTables("true");
            myDataStore.setDropTables("true");
            app.setExtraAttribute("forceRestart", "2"); // TODO can application restart be triggered automatically?
            // save
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));

            // tables must be empty because they were dropped and recreated
            runTest(server, APP_NAME, "testEntitiesCanBeAdded&invokedBy=testDatabaseStoreDropAndCreateTables_3");

            // switch to createTables=false dropTables=true
            myDataStore.setCreateTables("false");
            app.setExtraAttribute("forceRestart", "3"); // TODO can application restart be triggered automatically?
            // save
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));

            // tables must not exist
            runTest(server, APP_NAME, "testEntitiesDoNotHaveTables&invokedBy=testDatabaseStoreDropAndCreateTables_4");
        } finally {
            // restore to original
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(savedConfig);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));
        }

        // tables must be empty after being recreated
        runTest(server, APP_NAME, "testEntitiesNotFound&invokedBy=testDatabaseStoreDropAndCreateTables_5");
    }

    /**
     * Tests configuration of the data element for dropping and creating tables.
     */
    @Test
    public void testDataDropAndCreateTables() throws Exception {
        // use the entities to ensure async processing is complete before changing configuration
        runTest(server, APP_NAME, "testEntitiesNotFound&invokedBy=testDataDropAndCreateTables_0");

        // start with createTables=true dropTables=false,
        // but switch MyDataStore from databaseStore to dataSource
        ServerConfiguration config = server.getServerConfiguration();
        config.getDatabaseStores().removeById("MyDataStore");
        DataSource MyDataStore = (DataSource) config.getDataSources().getById("DefaultDataSource").clone();
        MyDataStore.setId("MyDataStore");
        config.getDataSources().add(MyDataStore);
        Data data;
        ConfigElementList<Data> datas = config.getData();
        if (datas.isEmpty()) {
            data = new Data();
            datas.add(data);
        } else {
            data = datas.get(0);
        }
        data.setCreateTables("true");
        data.setDropTables("false");
        // TODO can application restart be triggered automatically?
        Application app = config.getApplications().getBy("location", "DataConfigTestApp.war");
        app.setExtraAttribute("forceRestart", "1");
        // save
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));
        try {
            runTest(server, APP_NAME, "testEntitiesCanBeAdded&invokedBy=testDataDropAndCreateTables_1");

            // switch to createTables=false dropTables=false
            data.setCreateTables("false");
            data.setDropTables("false");
            // TODO can application restart be triggered automatically?
            config.getApplications().getBy("location", "DataConfigTestApp.war");
            app.setExtraAttribute("forceRestart", "2");
            // save
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));

            // Data must remain because tables were not dropped
            runTest(server, APP_NAME, "testEntitiesFound&invokedBy=testDataDropAndCreateTables_2");

            // switch to createTables=true dropTables=true
            data.setCreateTables("true");
            data.setDropTables("true");
            app.setExtraAttribute("forceRestart", "3"); // TODO can application restart be triggered automatically?
            // save
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));

            // tables must be empty because they were dropped and recreated
            runTest(server, APP_NAME, "testEntitiesCanBeAdded&invokedBy=testDataDropAndCreateTables_3");

            // switch to createTables=false dropTables=true
            data.setCreateTables("false");
            app.setExtraAttribute("forceRestart", "4"); // TODO can application restart be triggered automatically?
            // save
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));

            // tables must not exist
            runTest(server, APP_NAME, "testEntitiesDoNotHaveTables&invokedBy=testDataDropAndCreateTables_4");
        } finally {
            // restore to original
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(savedConfig);
            server.waitForConfigUpdateInLogUsingMark(Set.of(APP_NAME));
        }

        // tables must be empty after being recreated
        runTest(server, APP_NAME, "testEntitiesNotFound&invokedBy=testDataDropAndCreateTables_5");
    }
}
