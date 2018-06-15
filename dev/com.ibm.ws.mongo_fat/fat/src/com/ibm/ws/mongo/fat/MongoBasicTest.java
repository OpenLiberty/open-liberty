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
		MongoServerSelector.assignMongoServers(server);
		FATSuite.createApp(server);
		server.startServer();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		server.stopServer("CWKKD0013E:.*mongo-lib-10", "CWKKD0013E:.*mongo-lib-293", "SRVE0777E:.*MongoServlet\\.getDB",
				"CWWKE0701E" // TODO: Circular reference detected trying to get service
								// {org.osgi.service.cm.ManagedServiceFactory,
								// com.ibm.wsspi.logging.Introspector,
								// com.ibm.ws.runtime.update.RuntimeUpdateListener,
								// com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator}
		);
	}
}
