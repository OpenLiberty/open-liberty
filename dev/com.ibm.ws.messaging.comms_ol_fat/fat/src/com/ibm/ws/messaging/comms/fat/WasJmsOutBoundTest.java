/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.comms.fat;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class WasJmsOutBoundTest extends AbstractSuite {

	private static LibertyServer servletServer = LibertyServerFactory.getLibertyServer("WasJmsOutBoundTest_com.ibm.ws.messaging.comms.WJO.Client");
	private static LibertyServer jmsServer = LibertyServerFactory.getLibertyServer("WasJmsOutBoundTest_com.ibm.ws.messaging.comms.WJO.Server");


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		setupShrinkWrap(servletServer, jmsServer);

		copyFiles(jmsServer, "resources/security",
                "serverLTPAKeys/ltpa.keys", "serverLTPAKeys/serverKeynew.jks");
		copyFiles(servletServer, "resources/security", "clientLTPAKeys/clientKey.jks");

        jmsServer.startServer();
        servletServer.startServer();
		waitForServerStart(servletServer, false);
		waitForServerStart(jmsServer, true);
	}


	@AfterClass
	public static void tearDown() throws Exception {
		servletServer.stopServer();
		jmsServer.stopServer();
	}

	@Test
	public void testWJOSendReceive2LP() throws Exception {
		runInServlet(servletServer,"testQueueSendMessage");
		runInServlet(servletServer,"testQueueReceiveMessages");

		String msg = servletServer.waitForStringInLog("Queue Message", servletServer.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the queue message in the trace file", msg);
	}
}
