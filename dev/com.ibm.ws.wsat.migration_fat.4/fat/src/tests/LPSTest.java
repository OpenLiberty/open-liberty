/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class LPSTest extends DBTestBase {

	@Server("MigrationServer1")
	public static LibertyServer server;

	@Server("MigrationServer2")
	public static LibertyServer server2;

	public static String[] serverNames = new String[] {"MigrationServer1", "MigrationServer2"};

	private static String BASE_URL;
	private static String BASE_URL2;

	@BeforeClass
	public static void beforeTests() throws Exception {

		System.getProperties().entrySet().stream().forEach(e -> Log.info(SimpleTest.class, "beforeTests", e.getKey() + " -> " + e.getValue()));
		BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		DBTestBase.initWSATTest(server);
		DBTestBase.initWSATTest(server2);

		ShrinkHelper.defaultDropinApp(server, "LPSClient", "web.lpsclient.*");
		ShrinkHelper.defaultDropinApp(server, "LPSService", "web.lpsservice.*");
		ShrinkHelper.defaultDropinApp(server2, "LPSService", "web.lpsservice.*");

		server.setServerStartTimeout(START_TIMEOUT);
		server2.setServerStartTimeout(START_TIMEOUT);

		final Duration meanStartTime = FATUtils.startServers(server, server2);
		final float perfFactor = (float)normalStartTime.getSeconds() / (float)meanStartTime.getSeconds();
		Log.info(LPSTest.class, "beforeTests", "Mean startup time: "+meanStartTime+", Perf factor="+perfFactor);
		setTestQuerySuffix("perfFactor="+perfFactor);
	}

	@AfterClass
    public static void tearDown() throws Exception {
		FATUtils.stopServers(server, server2);

		DBTestBase.cleanupWSATTest(server);
		DBTestBase.cleanupWSATTest(server2);
    }
	
	// LPS Enabled Test
	@Test
	public void testWSTXLPS101FVT() {
		callServlet("WSTXLPS101FVT");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	public void testWSTXLPS102FVT() {
		callServlet("WSTXLPS102FVT");
	}
	
	@Test
	@ExpectedFFDC(value = { "java.lang.IllegalStateException" })
	public void testWSTXLPS103FVT() {
		callServlet("WSTXLPS103FVT");
	}

	@Test
	public void testWSTXLPS104FVT() {
		callServlet("WSTXLPS104FVT");
	}

	@Test
	public void testWSTXLPS105FVT() {
		callServlet("WSTXLPS105FVT");
	}

	@Test
	public void testWSTXLPS106FVT() {
		callServlet("WSTXLPS106FVT");
	}

	@Test
	public void testWSTXLPS107FVT() {
		callServlet("WSTXLPS107FVT");
	}

	@Test
	public void testWSTXLPS108FVT() {
		callServlet("WSTXLPS108FVT");
	}

	@Test
	public void testWSTXLPS109FVT() {
		callServlet("WSTXLPS109FVT");
	}

	@Test
	public void testWSTXLPS110FVT() {
		callServlet("WSTXLPS110FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS111FVT() {
		callServlet("WSTXLPS111FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS112FVT() {
		callServlet("WSTXLPS112FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS113FVT() {
		callServlet("WSTXLPS113FVT");
	}

	@Test
	@ExpectedFFDC(value = { "java.lang.IllegalStateException" })
	public void testWSTXLPS114FVT() {
		callServlet("WSTXLPS114FVT");
	}
	
	@Test
	public void testWSTXLPS201FVT() {
		callServlet("WSTXLPS201FVT");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS202FVT() {
		callServlet("WSTXLPS202FVT");
		assertNotNull("Expected to see Error WTRN0064E", server2.waitForStringInLog("WTRN0064E"));
		List<String> errors = new ArrayList<String>();
		errors.add("WTRN0064E");
		server2.addIgnoredErrors(errors);
	}
	
	@Test
	public void testWSTXLPS203FVT() {
		callServlet("WSTXLPS203FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS204FVT() {
		callServlet("WSTXLPS204FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS205FVT() {
		callServlet("WSTXLPS205FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException"})
	public void testWSTXLPS206FVT() {
		callServlet("WSTXLPS206FVT");
	}

	@Test
	public void testWSTXLPS207FVT() {
		callServlet("WSTXLPS207FVT");
	}

	private void callServlet(String testMethod){
		try {
			int testNumber = Integer.parseInt(testMethod.substring(7, 10));
			String providerURL = BASE_URL;
			if (testNumber>200)
				providerURL = BASE_URL2;
			String urlStr = BASE_URL + "/LPSClient/LPSClientServlet"
					+ "?method=" + testNumber + "&baseurl=" + providerURL;
			System.out.println(testMethod + " URL: " + urlStr);
            HttpURLConnection con = getHttpConnection(new URL(urlStr), 
            		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String result = br.readLine();
            assertNotNull(result);
			System.out.println(testMethod + " Result : " + result);
			assertTrue("Cannot get expected reply from server, result = '" + result + "'",
					result.contains("Test passed"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}
}
