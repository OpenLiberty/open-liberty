/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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
package com.ibm.ws.mongo.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import fat.mongo.web.MongoTestServlet;

@RunWith(FATRunner.class)
public class MongoBasicTest extends FATServletClient {

    @Server("mongo.fat.server")
    @TestServlet(servlet = MongoTestServlet.class, contextRoot = FATSuite.APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        FATSuite.skipTestOnFIPS140_3Enabled(server);
        MongoServerSelector.assignMongoServers(server);
        FATSuite.createApp(server);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // TODO: CWWKE0701E - Circular reference detected trying to get service
        // {org.osgi.service.cm.ManagedServiceFactory,
        // com.ibm.wsspi.logging.Introspector,
        // com.ibm.ws.runtime.update.RuntimeUpdateListener,
        // com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator}
        server.stopServer("CWWKE0701E");
    }
}
