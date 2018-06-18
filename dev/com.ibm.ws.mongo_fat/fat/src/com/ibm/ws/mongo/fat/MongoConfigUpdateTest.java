/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.MongoDBElement;
import com.ibm.websphere.simplicity.config.MongoElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MongoConfigUpdateTest extends FATServletClient {

    @Server("mongo.fat.server.config.update")
    public static LibertyServer server;

    private static ServerConfiguration originalConfig;
    private static final String[] EXPECTED_ERRS = {
                                                    "CWKKD0017E:.*", "CWKKD0013E:.*", "SRVE0777E:.*",
                                                    "SRVE0315E:.*com.mongodb.CommandFailureException.*client_not_known.*",
                                                    "CWWKE0701E" // TODO: Circular reference detected
                    // trying to get service
                    // {org.osgi.service.cm.ManagedServiceFactory,
                    // com.ibm.wsspi.logging.Introspector,
                    // com.ibm.ws.runtime.update.RuntimeUpdateListener,
                    // com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator}
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        MongoServerSelector.assignMongoServers(server);
        originalConfig = server.getServerConfiguration().clone();
        FATSuite.createApp(server);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer(EXPECTED_ERRS);
    }

    @Before
    public void beforeEach() throws Exception {
        // Restore original config after each test if it has changed
        if (!server.getServerConfiguration().equals(originalConfig))
            updateConfig(originalConfig.clone());
    }

    private void updateConfig(ServerConfiguration config) throws Exception {
        server.updateServerConfiguration(config);
        if (server.isStarted()) {
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(FATSuite.APP_NAME));
            server.setMarkToEndOfLog();
        } else {
            server.startServer();
        }
    }

    @Test
    public void testChangeDbInsertFind() throws Exception {
        runInsertFind();

        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<MongoDBElement> mdbs = config.getMongoDBs();
        for (MongoDBElement mdb : mdbs) {
            // Change all MongoDB elemets to point at new db
            mdb.setDatabaseName("default2");
        }

        updateConfig(config);
        runInsertFind();

        Map<String, String> conf = runConfigDump();
        assertEquals("default2", conf.get("databaseName"));
    }

    @Test
    public void testUpdateLibRef() throws Exception {
        runInsertFind();

        updateLib("mongo-lib-updated");

        runInsertFind();
    }

    @Test
    public void testSetAllPropertiesOnRunningServer() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();

        runInsertFind();

        for (MongoElement element : config.getMongos()) {
            element.setAutoConnectRetry(true);
            element.setConnectionsPerHost(11);
            element.setConnectTimeout(1100);
            element.setCursorFinalizerEnabled(true);
            element.setDescription("descccccc");
            element.setMaxAutoConnectRetryTime(Long.valueOf(5666));
            element.setMaxWaitTime(1895);
            element.setReadPreference("nearest");
            element.setWriteConcern("ACKNOWLEDGED");
            element.setSocketKeepAlive(false);
            element.setSocketTimeout(1234);
            element.setThreadsAllowedToBlockForConnectionMultiplier(125);
        }
        updateConfig(config);

        //runInsertFind();
        Map<String, String> dbConfig = runConfigDump();
        assertEquals("true", dbConfig.get("autoConnectRetry"));
        assertEquals("11", dbConfig.get("connectionsPerHost"));
        assertEquals("1100", dbConfig.get("connectTimeout"));
        assertEquals("descccccc", dbConfig.get("description"));
        assertEquals("5666", dbConfig.get("maxAutoConnectRetryTime"));
        assertEquals("1895", dbConfig.get("maxWaitTime"));
        assertEquals("nearest", dbConfig.get("readPreference"));
        assertEquals("false", dbConfig.get("socketKeepAlive"));
        assertEquals("1234", dbConfig.get("socketTimeout"));
        assertEquals("125", dbConfig.get("threadsAllowedToBlockForConnectionMultiplier"));
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    public void testV1_0_MongoFailure() throws Exception {
        testOldMongoDriver("mongo-lib-10");
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    public void testV2_9_3MongoFailure() throws Exception {
        testOldMongoDriver("mongo-lib-293");
    }

    private void testOldMongoDriver(String libToUse) throws Exception {
        runInsertFind();

        updateLib(libToUse);

        boolean gotError = false;
        try {
            runInsertFind();
        } catch (AssertionError expected) {
            gotError = true;
        }
        assertTrue("Expected failure trying to access application with the " + libToUse + " Mongo driver", gotError);
        assertNotNull("Server exception for error CWKKD0013E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0013E"));
    }

    private void updateLib(String libName) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> appLibRefs = config.getApplications()
                        .getBy("name", FATSuite.APP_NAME)
                        .getClassloaders()
                        .get(0)
                        .getCommonLibraryRefs();
        appLibRefs.clear();
        appLibRefs.add(libName);

        for (MongoElement mongo : config.getMongos())
            mongo.setLibraryRef(libName);

        for (MongoDBElement mongoDB : config.getMongoDBs()) {
            MongoElement mongo = mongoDB.getMongo();
            if (mongo != null)
                mongo.setLibraryRef(libName);
        }

        updateConfig(config);
    }

    private void runInsertFind() throws Exception {
        FATServletClient.runTest(server, FATSuite.APP_NAME + "/MongoTestServlet", "basicInsertFind&forTest=" + testName.getMethodName());
    }

    private Map<String, String> runConfigDump() throws Exception {
        String response = runTestWithResponse(server, FATSuite.APP_NAME + "/MongoTestServlet", "configDump&forTest=" + testName.getMethodName()).toString();
        Map<String, String> res = new HashMap<>();
        String[] values = response.split("[;]");
        for (String value : values) {
            String[] kv = value.split("[=]");
            res.put(kv[0], kv[1]);
        }
        return res;
    }
}
