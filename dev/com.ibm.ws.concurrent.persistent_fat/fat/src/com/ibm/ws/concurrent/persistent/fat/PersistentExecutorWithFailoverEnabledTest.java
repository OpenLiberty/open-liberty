/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat;

import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.SchedulerFATServlet;

@AllowedFFDC({
    "jakarta.persistence.PersistenceException", // after transaction times out and rolls back
    "jakarta.resource.ResourceException", // due to transaction timeout from infra slowness
    "jakarta.transaction.RollbackException", // due to transaction timeout from infra slowness
    "javax.persistence.PersistenceException", // after transaction times out and rolls back
    "javax.resource.ResourceException", // due to transaction timeout from infra slowness
    "javax.transaction.RollbackException", // due to transaction timeout from infra slowness
    "javax.transaction.xa.XAException" // due to transaction timeout from infra slowness
})
@RunWith(FATRunner.class)
public class PersistentExecutorWithFailoverEnabledTest extends FATServletClient {

    private static final String APP_NAME = "schedtest";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()//
            .andWith(new JakartaEE9Action());

    @Server("com.ibm.ws.concurrent.persistent.fat.failover")
    @TestServlet(servlet = SchedulerFATServlet.class, path = APP_NAME)
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @BeforeClass
    public static void setUp() throws Exception {
        testContainer.start();

        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");

        // Use the Jakarta version of test features if Jakarta is being used.
        if (JakartaEE9Action.isActive()) {
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            features.remove("timerinterfacestestfeature-1.0");
            features.add("timerInterfacesTestFeature-2.0");
            server.updateServerConfiguration(config);
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            runTest(server, APP_NAME, "verifyNoTasksRunning");
        } finally {
            try {
                if (server.isStarted())
                    server.stopServer("CWWKC1500W", // Task rolled back
                            "CWWKC1501W", // Task rolled back due to failure ...
                            "CWWKC1502W", // Task rolled back, retry time unspecified
                            "CWWKC1503W", // Task rolled back due to failure ..., retry time unspecified
                            "CWWKC1510W", // Task rolled back and aborted
                            "CWWKC1511W", // Task rolled back and aborted. Failure is ...
                            "DSRA0174W",  // Generic Datasource Helper
                            "DSRA030*E"); // XA errors related to transaction timeout
            } finally {
                testContainer.stop();
            }
        }
    }

    @Test
    public void testBlockAfterCancelByIdFE() throws Exception {
        runTest(server, APP_NAME, "testBlockAfterCancelByIdFE");
    }

    @Test
    public void testBlockAfterCancelByNameFE() throws Exception {
        runTest(server, APP_NAME, "testBlockAfterCancelByNameFE");
    }

    @Test
    public void testBlockAfterFindByIdFE() throws Exception {
        runTest(server, APP_NAME, "testBlockAfterFindByIdFE");
    }

    @Test
    public void testBlockAfterFindByNameFE() throws Exception {
        runTest(server, APP_NAME, "testBlockAfterFindByNameFE");
    }

    @Test
    public void testBlockAfterRemoveByIdFE() throws Exception {
        runTest(server, APP_NAME, "testBlockAfterRemoveByIdFE");
    }

    @Test
    public void testBlockAfterRemoveByNameFE() throws Exception {
        runTest(server, APP_NAME, "testBlockAfterRemoveByNameFE");
    }

    @Test
    public void testBlockAfterScheduleFE() throws Exception {
        runTest(server, APP_NAME, "testBlockAfterScheduleFE");
    }

    @Test
    public void testBlockRunningTaskFE() throws Exception {
        runTest(server, APP_NAME, "testBlockRunningTaskFE");
    }

    @Test
    public void testBlockRunningTaskThatCancelsSelfFE() throws Exception {
        runTest(server, APP_NAME, "testBlockRunningTaskThatCancelsSelfFE");
    }

    @Test
    public void testBlockRunningTaskThatRemovesSelfFE() throws Exception {
        runTest(server, APP_NAME, "testBlockRunningTaskThatRemovesSelfFE");
    }

    @Test
    public void testCancelRunningTaskFE() throws Exception {
        runTest(server, APP_NAME, "testCancelRunningTaskFE");
    }

    @Test
    public void testRemoveRunningTaskAutoPurgeFE() throws Exception {
        runTest(server, APP_NAME, "testRemoveRunningTaskAutoPurgeFE");
    }

    @Test
    public void testRemoveRunningTaskFE() throws Exception {
        runTest(server, APP_NAME, "testRemoveRunningTaskFE");
    }
}