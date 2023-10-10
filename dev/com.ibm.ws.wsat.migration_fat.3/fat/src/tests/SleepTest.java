/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class SleepTest extends DBTestBase {

	@Server("WSATSleep")
	public static LibertyServer server;

	@Server("MigrationServer2")
	public static LibertyServer server2;

	private static String BASE_URL;
	private static String BASE_URL2;
	
	private static final Duration normalStartupTime = Duration.ofSeconds(30); // Normal test machine startup time
	private static Duration meanStartupTime;

	@BeforeClass
	public static void beforeTests() throws Exception {
		System.getProperties().entrySet().stream().forEach(e -> Log.info(SleepTest.class, "beforeTests", e.getKey() + " -> " + e.getValue()));

		BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		DBTestBase.initWSATTest(server);
		DBTestBase.initWSATTest(server2);

		ShrinkHelper.defaultDropinApp(server, "simpleClient", "web.simpleclient");
		ShrinkHelper.defaultDropinApp(server2, "simpleService", "web.simpleservice");

		server.setServerStartTimeout(START_TIMEOUT);
		server2.setServerStartTimeout(START_TIMEOUT);
		
		// None of these tests take into account client inactivity timeout so we'll set it infinite
		ServerConfiguration config = server2.getServerConfiguration();
		
		config.getTransaction().setClientInactivityTimeout("0");
		
		server2.updateServerConfiguration(config);

		meanStartupTime = FATUtils.startServers(server, server2);

		Log.info(SleepTest.class, "beforeTests", "Mean server start time: " + meanStartupTime);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server, server2);

		DBTestBase.cleanupWSATTest(server);
		DBTestBase.cleanupWSATTest(server2);
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE094FVT() {
		callServlet("WSATRE094FVT");
	}

	@Test
	public void testWSATRE095FVT() {
		callServlet("WSATRE095FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void testWSATRE096FVT() {
		callServlet("WSATRE096FVT");
	}

	@Test
	public void testWSATRE097FVT() {
		callServlet("WSATRE097FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void testWSATRE098FVT() {
		callServlet("WSATRE098FVT");
	}

	@Test
	public void testWSATRE099FVT() {
		callServlet("WSATRE099FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void testWSATRE100FVT() {
		callServlet("WSATRE100FVT");
	}

	@Test
	public void testWSATRE101FVT() {
		callServlet("WSATRE101FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE102FVT() {
		callServlet("WSATRE102FVT");
	}

	@Test
	public void testWSATRE103FVT() {
		callServlet("WSATRE103FVT");
	}

	@Test
	public void testWSATRE104FVT() {
		callServlet("WSATRE104FVT");
	}

	@Test
	public void testWSATRE105FVT() {
		callServlet("WSATRE105FVT");
	}

	private void callServlet(String testMethod){
		try {
			float perf = (float)normalStartupTime.getSeconds() / (float)meanStartupTime.getSeconds();
			perf = (float) (perf > 1.0 ? 1.0 : perf);
			Log.info(SleepTest.class, "callServlet", "Perf: " + Float.toString(perf));
			
			// The perf param is an indication of how slow the test machine is
			String urlStr = BASE_URL + "/simpleClient/SimpleClientServlet"
					+ "?method=" + Integer.parseInt(testMethod.substring(6, 9))
					+ "&baseurl=" + BASE_URL2
					+ "&perf=" + perf;
			Log.info(SleepTest.class, "callServlet", "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			System.out.println(testMethod + " Result : " + result);
			assertTrue("Cannot get expected reply from server, result = '" + result + "'",
					result.contains("Test passed"));
		} catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Exception happens: " + e.toString());
		}
	}
}
