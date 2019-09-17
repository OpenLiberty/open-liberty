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
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.DerbyEmbeddedUtilities;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

import web.SchedulerFATServlet;

@RunWith(FATRunner.class)
public class SchedulerTest extends FATServletClient {

    private static final String APP_NAME = "schedtest";
    private static final String DB_NAME = "${shared.resource.dir}/data/scheddb";

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

        //Create Derby DB if using derby
        if (dbContainerType == DatabaseContainerType.Derby)
            DerbyEmbeddedUtilities.createDB(server, "TaskStoreDS", "userx", "passx");

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
            if (server != null && server.isStarted())
                server.stopServer("CWWKC1500W", //Persistent Executor Rollback
                                  "CWWKC1510W", //Persistent Executor Rollback and Failed
                                  "DSRA0174W"); //Generic Datasource Helper
        }
    }
}