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
package com.ibm.ws.concurrent.persistent.fat.compat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import web.PersistentExecCompatibilityTestServlet;

/**
 * Tests for persistent scheduled executor with task execution disabled
 */
@RunWith(FATRunner.class)
public class PersistentExecutorCompatibilityTest {
    public static final String APP_NAME = "persistentcompattest";

    @Server("com.ibm.ws.concurrent.persistent.fat.compat")
    @TestServlet(servlet = PersistentExecCompatibilityTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    /**
     * Before running any tests, start the server
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Delete the Derby-only database that is used by the persistent executor
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/persistcompat");

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web", "ejb");
        server.startServer();
    }

    /**
     * After completing all tests, stop the server.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted())
            server.stopServer("CNTR0333W","CWWKC1511W");
    }
}