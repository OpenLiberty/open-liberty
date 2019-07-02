/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.LoadTestServlet;

@RunWith(FATRunner.class)
@SkipIfSysProp(DB_Informix) // persistent executor is not support on Informix
public class LoadTest extends FATServletClient {

    private static final String APP_NAME = "schedtest";

    @Server(FATSuite.SERVER_NAME)
    @TestServlet(servlet = LoadTestServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
    	ShrinkHelper.defaultDropinApp(server, APP_NAME, "web", "web.task");
        server.configureForAnyDatabase();
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKC1501W"); // Ignore failing task warning message
        }
    }
}