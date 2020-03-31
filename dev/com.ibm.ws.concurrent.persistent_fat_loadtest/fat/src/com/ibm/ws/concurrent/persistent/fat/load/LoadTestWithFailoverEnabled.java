/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.load;

import static componenttest.annotation.SkipIfSysProp.DB_Informix;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.LoadTestServlet;

@RunWith(FATRunner.class)
@SkipIfSysProp(DB_Informix) // persistent executor is not supported on Informix
public class LoadTestWithFailoverEnabled extends FATServletClient {

    private static final String APP_NAME = "schedtest";

    public static final String SERVER_NAME = "com.ibm.ws.concurrent.persistent.fat.loadtest";

    private static ServerConfiguration originalConfig;

    @Server(SERVER_NAME)
    @TestServlet(servlet = LoadTestServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Delete the Derby database that might be used by the persistent scheduled executor and the Derby-only test database
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/scheddb");

        originalConfig = server.getServerConfiguration();
        ServerConfiguration config = originalConfig.clone();
        PersistentExecutor myScheduler = config.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        myScheduler.setPollInterval("5s");
        myScheduler.setInitialPollDelay("0");
        myScheduler.setMissedTaskThreshold("4s");
        myScheduler.setRetryInterval(null);
        myScheduler.setExtraAttribute("ignore.minimum.for.test.use.only", "true"); // allows missedTaskThreshold value for test

        server.updateServerConfiguration(config);

    	ShrinkHelper.defaultDropinApp(server, APP_NAME, "web", "web.task");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (server.isStarted())
                server.stopServer("CWWKC1502W", "CWWKC1503W"); // Ignore failing task warning messages. These are fine as long as tasks retry and are eventually successful.
        } finally {
            if (originalConfig != null)
                server.updateServerConfiguration(originalConfig);
        }
    }
}