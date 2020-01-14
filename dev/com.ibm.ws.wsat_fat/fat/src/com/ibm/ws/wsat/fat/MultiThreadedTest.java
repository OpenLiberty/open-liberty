/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class MultiThreadedTest extends WSATTest {

	private static LibertyServer server;
	private static String BASE_URL;
	private static LibertyServer server2;
	private static String BASE_URL2;

	@BeforeClass
	public static void beforeTests() throws Exception {
		
		server = LibertyServerFactory
				.getLibertyServer("MigrationServer1");
		BASE_URL = "http://" + server.getHostname() + ":"
				+ server.getHttpDefaultPort();
		server2 = LibertyServerFactory
				.getLibertyServer("MigrationServer2");
		BASE_URL2 = "http://" + server2.getHostname() + ":9992";

		if (server != null && server.isStarted()){
			server.stopServer();
		}
		
		if (server2 != null && server2.isStarted()){
			server2.stopServer();
		}

 		DBTestBase.initWSATTest(server);

    ShrinkHelper.defaultDropinApp(server, "threadedClient", "com.ibm.ws.wsat.threadedclient.*");
    ShrinkHelper.defaultDropinApp(server2, "threadedServer", "com.ibm.ws.wsat.threadedserver.*");

     if (server != null && !server.isStarted()){
         server.setServerStartTimeout(600000);
         server.startServer(true);
		}
		
		if (server2 != null && !server2.isStarted()){
			 server2.setServerStartTimeout(600000);
		     server2.startServer(true);
		}
	}

	@AfterClass
    public static void tearDown() throws Exception {
		ServerUtils.stopServer(server);
		ServerUtils.stopServer(server2);

		DBTestBase.cleanupWSATTest(server);
    }
	
	@Test
	@Mode(TestMode.FULL)
	@AllowedFFDC(value = {"javax.transaction.RollbackException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "com.ibm.ws.wsat.service.WSATException", "java.lang.IllegalStateException", "com.ibm.ws.wsat.service.WSATFaultException"})
	public void testWSATMT001FVT() {
		final int count = 100;
		try {
			String urlStr = BASE_URL + "/threadedClient/ThreadedClientServlet"
					+ "?baseurl=" + BASE_URL2 + "&count=" + count;
			System.out.println("URL: " + urlStr);
            HttpURLConnection con = getHttpConnection(new URL(urlStr), 
            		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT + count); // Use a bigger timeout here
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String result = br.readLine();
            assertNotNull(result);
			System.out.println("Result : " + result);
			
			assertTrue("Cannot get expected reply from server",
					result.contains("completedCount = "+count));
			assertTrue("Transactions are still running",
					result.contains("transactionCount = 0"));
			
			Thread.sleep(count * 1000);

			// get number of resources committed in client
			urlStr = BASE_URL + "/threadedClient/CoordinatorCheckServlet";
            con = HttpUtils.getHttpConnection(new URL(urlStr), 
            		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            String clientCommits = br.readLine();
		

			// get number of resources committed in server
			urlStr = BASE_URL2 + "/threadedServer/ParticipantCheckServlet";
            con = getHttpConnection(new URL(urlStr), 
            		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            String participantCommits = br.readLine();
			
            System.out.println("Client commits: " + clientCommits + ", Participant commits: " + participantCommits);
            assertTrue("Coordinator commit count differs from participant", Integer.parseInt(clientCommits) == Integer.parseInt(participantCommits));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}
}
