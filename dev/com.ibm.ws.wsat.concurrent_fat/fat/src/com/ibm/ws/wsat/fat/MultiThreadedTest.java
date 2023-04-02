/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import com.ibm.ws.wsat.fat.util.WSATTest;
import com.ibm.ws.transaction.fat.util.FATUtils;

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

		server.setServerStartTimeout(START_TIMEOUT);
		server2.setServerStartTimeout(START_TIMEOUT);
		
		FATUtils.startServers(server, server2);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(new String[] {"WTRN0046E", "WTRN0048W", "WTRN0049W"}, server, server2);

		ShrinkHelper.cleanAllExportedArchives();
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.InvalidTransactionException", "javax.transaction.RollbackException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "com.ibm.ws.wsat.service.WSATException", "java.lang.IllegalStateException", "com.ibm.ws.wsat.service.WSATFaultException", "org.osgi.framework.BundleException"})
	public void testWSATMT001FVT() {
		String method = "testWSATMT001FVT";
		String result;
		String urlStr;
		HttpURLConnection con;
		BufferedReader br;
		try {
			OperatingSystem os = server.getMachine().getOperatingSystem();
			int count = os == OperatingSystem.ZOS ? 10 : 50;
			Log.info(getClass(), method, "Thread count set to " + count + " because operating system is " + os);

			final int originalCount = count;
			do {
				if (count != originalCount) {
					// This is a retry. Wait a minute
					Thread.sleep(60000);
				}
				urlStr = BASE_URL + "/threadedClient/ThreadedClientServlet"
						+ "?baseurl=" + BASE_URL2 + "&count=" + count;
				Log.info(getClass(), method, "URL: " + urlStr);
				con = getHttpConnection(new URL(urlStr), 
						HttpURLConnection.HTTP_OK, 1200); // 20 minutes
				br = HttpUtils.getConnectionStream(con);
				result = br.readLine();
				assertNotNull(result);

				Log.info(getClass(), method, "Result: " + result);

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
			
            Log.info(getClass(), method, "Client commits: " + clientCommits + ", Participant commits: " + participantCommits);
            assertTrue("Coordinator commit count differs from participant", Integer.parseInt(clientCommits) == Integer.parseInt(participantCommits));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}
}