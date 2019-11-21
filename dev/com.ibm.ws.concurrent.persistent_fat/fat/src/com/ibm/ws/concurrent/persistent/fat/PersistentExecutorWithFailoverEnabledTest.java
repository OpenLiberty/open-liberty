/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.DerbyEmbeddedUtilities;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

import web.SchedulerFATServlet;

@RunWith(FATRunner.class)
public class PersistentExecutorWithFailoverEnabledTest extends FATServletClient {

    private static final String APP_NAME = "schedtest";
    private static final String DB_NAME = "${shared.resource.dir}/data/scheddb";

    private static ServerConfiguration originalConfig;

    @Server("com.ibm.ws.concurrent.persistent.fat")
    @TestServlet(servlet = SchedulerFATServlet.class, path = APP_NAME)
    public static LibertyServer server;
    
    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create(DB_NAME);

    /**
     * Before running any tests, start the server
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Delete the Derby database that might be used by the persistent scheduled executor and the Derby-only test database
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/scheddb");
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/testdb");

        originalConfig = server.getServerConfiguration();
        ServerConfiguration config = originalConfig.clone();
        PersistentExecutor myScheduler = config.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        myScheduler.setPollInterval("3h"); // the test case does not expect polling, so set a large value that will never be reached
        myScheduler.setExtraAttribute("missedTaskThreshold2", "1m"); // TODO rename to missedTaskThreshold and use normal setter if this approach is chosen
        server.updateServerConfiguration(config);

    	//Get type
		DatabaseContainerType dbContainerType = DatabaseContainerType.valueOf(testContainer);

    	//Get driver info
    	String driverName = dbContainerType.getDriverName();
    	server.addEnvVar("DB_DRIVER", driverName);

    	//Setup server DataSource properties
    	DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);
    	
		//Initialize database
		DatabaseContainerUtil.initDatabase(testContainer, DB_NAME);

		//Add application to server
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");

        server.startServer();
    }

    /**
     * After completing all tests, stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        try {
            runTest(server, APP_NAME, "verifyNoTasksRunning");
        } finally {
            if (server != null)
                try {
                    if (server.isStarted())
                    server.stopServer("CWWKC1500W", //Persistent Executor Rollback
                                      "CWWKC1510W", //Persistent Executor Rollback and Failed
                                      "DSRA0174W"); //Generic Datasource Helper
                } finally {
                    if (originalConfig != null)
                        server.updateServerConfiguration(originalConfig);
                }
        }
    }

    @Test
    public void testCancelRunningTaskFE() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testRemoveRunningTaskAutoPurgeFE() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testRemoveRunningTaskFE() throws Exception {
        runTest(server, APP_NAME, testName);
    }
}