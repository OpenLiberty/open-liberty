/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.SchedulerFATServlet;

@AllowedFFDC({
    "javax.resource.ResourceException", // due to transaction timeout from infra slowness
    "javax.transaction.RollbackException", // due to transaction timeout from infra slowness
    "javax.transaction.xa.XAException" // due to transaction timeout from infra slowness
    })
@RunWith(FATRunner.class)
public class PersistentExecutorTest extends FATServletClient {

    private static final String APP_NAME = "schedtest";

    @Server("com.ibm.ws.concurrent.persistent.fat")
    @TestServlet(servlet = SchedulerFATServlet.class, path = APP_NAME)
    public static LibertyServer server;
    
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @BeforeClass
    public static void setUp() throws Exception {
    	testContainer.start();
    	
    	server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

    	DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");

        server.startServer();
    }

    /**
     * After completing all tests, stop the server.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        try {
            runTest(server, APP_NAME, "verifyNoTasksRunning");
        } finally {
        	try {
	            if (server.isStarted())
	                server.stopServer("CWWKC1500W", //Task rolled back
	                                  "CWWKC1501W", //Task rolled back due to failure ...
	                                  "CWWKC1510W", //Task rolled back and aborted
	                                  "CWWKC1511W", //Task rolled back and aborted. Failure is ...
	                                  "DSRA0174W"); //Generic Datasource Helper
        	} finally {
        		testContainer.stop();
        	}
        }
    }

    @Test
    public void testCancelRunningTask() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testRemoveRunningTaskAutoPurge() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testRemoveRunningTask() throws Exception {
        runTest(server, APP_NAME, testName);
    }
}