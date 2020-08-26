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
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class MultiThreadedTest extends WSATTest {

	@Server("MigrationServer1")
	public static LibertyServer server;
	private static String BASE_URL;

	@Server("MigrationServer2")
	public static LibertyServer server2;
	private static String BASE_URL2;

	@BeforeClass
	public static void beforeTests() throws Exception {

		BASE_URL = "http://" + server.getHostname() + ":"
				+ server.getHttpDefaultPort();

		server2.setHttpDefaultPort(9992);
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		ShrinkHelper.defaultDropinApp(server, "threadedClient", "com.ibm.ws.wsat.threadedclient.*");
		ShrinkHelper.defaultDropinApp(server2, "threadedServer", "com.ibm.ws.wsat.threadedserver.*");

		server.setServerStartTimeout(600000);
		server.startServer(true);

		server2.setServerStartTimeout(600000);
		server2.startServer(true);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		server.stopServer("WTRN0046E", "WTRN0048W", "WTRN0049W");
		server2.stopServer();
		
		ShrinkHelper.cleanAllExportedArchives();
  }
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.RollbackException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "com.ibm.ws.wsat.service.WSATException", "java.lang.IllegalStateException", "com.ibm.ws.wsat.service.WSATFaultException"})
	public void testWSATMT001FVT() {
		int count = 100;
		String result;
		String urlStr;
		HttpURLConnection con;
		BufferedReader br;
		try {
			final int originalCount = count;
			do {
				if (count != originalCount) {
					// This is a retry. Wait a minute
					Thread.sleep(60000);
				}
				urlStr = BASE_URL + "/threadedClient/ThreadedClientServlet"
						+ "?baseurl=" + BASE_URL2 + "&count=" + count;
				Log.info(this.getClass(), "testWSATMT001FVT", "URL: " + urlStr);
				con = getHttpConnection(new URL(urlStr), 
						HttpURLConnection.HTTP_OK, 1200); // 20 minutes
				br = HttpUtils.getConnectionStream(con);
				result = br.readLine();
				assertNotNull(result);

				Log.info(this.getClass(), "testWSATMT001FVT", "Result : " + result);

				assertTrue("Cannot get expected reply from server",
						result.contains("completedCount = "+originalCount));
				count = 0;
			} while (!result.contains("transactionCount = 0"));

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
			
            Log.info(this.getClass(), "testWSATMT001FVT", "Client commits: " + clientCommits + ", Participant commits: " + participantCommits);
            assertTrue("Coordinator commit count differs from participant", Integer.parseInt(clientCommits) == Integer.parseInt(participantCommits));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}
}
