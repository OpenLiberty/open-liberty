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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.database.container.PostgreSQLContainer;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class DBRerouteTest extends DBTestBase {

    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";
    public static PostgreSQLContainer testContainer;
    
	@Server("Server1")
	public static LibertyServer server;

	@Server("Server2")
	public static LibertyServer server2;

	@Server("Server3")
	public static LibertyServer server3;

	public static String[] serverNames = new String[] {"Server1", "Server2", "Server3"};

	private static String BASE_URL;
	private static String BASE_URL2;

	@BeforeClass
	public static void beforeTests() throws Exception {

	    // The Dockerfile for 'jonhawkes/postgresql-ssl:1.0' can be found in the com.ibm.ws.jdbc_fat_postgresql project
	    testContainer = new PostgreSQLContainer("jonhawkes/postgresql-ssl:1.0")
	                    .withDatabaseName(POSTGRES_DB)
	                    .withUsername(POSTGRES_USER)
	                    .withPassword(POSTGRES_PASS)
	                    .withSSL()
	                    .withLogConsumer(new SimpleLogConsumer(DBRerouteTest.class, "postgre-ssl"));

        testContainer.setStartupAttempts(2);
        testContainer.start();

//		System.getProperties().entrySet().stream().forEach(e -> Log.info(RerouteTest.class, "Properties", e.getKey() + " -> " + e.getValue()));
//		System.getenv().entrySet().stream().forEach(e -> Log.info(RerouteTest.class, "Environment", e.getKey() + " -> " + e.getValue()));

		BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();

		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		server3.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_tertiary")));

		DBTestBase.initWSATTest(server);
		DBTestBase.initWSATTest(server2);

		ShrinkHelper.defaultDropinApp(server, "simpleClient", "web.simpleclient");
		ShrinkHelper.defaultDropinApp(server2, "simpleService", "web.simpleservice");

		FATUtils.startServers(runner, server, server2, server3);
	}

    public static SetupRunner runner = new SetupRunner() {
        @Override
        public void run(LibertyServer s) throws Exception {
            setUp(s);
        }
    };

    public static void setUp(LibertyServer s) throws Exception {

        //Get driver name
        s.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(s, testContainer);

        s.setServerStartTimeout(START_TIMEOUT);
    }

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server, server2, server3);

		DBTestBase.cleanupWSATTest(server);
		DBTestBase.cleanupWSATTest(server2);
		
		testContainer.stop();
	}

	@After
	public void sleep() throws InterruptedException {
		// Sleep a little to ensure stray async messages are all done
		Thread.sleep(5000);
	}

	@Test
	public void testWSATRE001FVT() {
		//Client: Begin Tx, getStatus, Call Web Service, getStatus, Commit Tx
		//Server: getStatus
		callServlet("WSATRE001FVT");		
	}

	@Test
	public void testWSATRE002FVT() {
		//Client: Begin Tx, getStatus, Call Web Service, getStatus, Rollback Tx
		//Server: getStatus
		callServlet("WSATRE002FVT");
	}

	@Test
	public void testWSATRE003FVT() {
		callServlet("WSATRE003FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE004FVT() {
		callServlet("WSATRE004FVT");
	}

	@Test
	public void testWSATRE005FVT() {
		callServlet("WSATRE005FVT");
	}

	@Test
	public void testWSATRE006FVT() {
		callServlet("WSATRE006FVT");
	}

	@Test
	public void testWSATRE007FVT() {
		callServlet("WSATRE007FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE008FVT() {
		callServlet("WSATRE008FVT");
	}

	@Test
	public void testWSATRE009FVT() {
		callServlet("WSATRE009FVT");
	}

	@Test
	public void testWSATRE010FVT() {
		callServlet("WSATRE010FVT");
	}

	@Test
	public void testWSATRE011FVT() {
		callServlet("WSATRE011FVT");
	}

	@Test
	@Mode(TestMode.LITE)
	public void testWSATRE012FVT() {
		callServlet("WSATRE012FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.HeuristicCommitException" })
	public void testWSATRE013FVT() {
		callServlet("WSATRE013FVT");
	}

	@Test
	public void testWSATRE014FVT() {
		callServlet("WSATRE014FVT");
	}

	@Test
	public void testWSATRE015FVT() {
		callServlet("WSATRE015FVT");
	}

	@Test
	public void testWSATRE016FVT() {
		callServlet("WSATRE016FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void testWSATRE017FVT() {
		callServlet("WSATRE017FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void testWSATRE018FVT() {
		callServlet("WSATRE018FVT");
	}

	@Test
	public void testWSATRE019FVT() {
		callServlet("WSATRE019FVT");
	}

	@Test
	public void testWSATRE020FVT() {
		callServlet("WSATRE020FVT");
	}

	@Test
	public void testWSATRE021FVT() {
		callServlet("WSATRE021FVT");
	}

	@Test
	public void testWSATRE022FVT() {
		callServlet("WSATRE022FVT");
	}

	@Test
	public void testWSATRE023FVT() {
		callServlet("WSATRE023FVT");
	}

	@Test
	public void testWSATRE024FVT() {
		callServlet("WSATRE024FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE025FVT() {
		callServlet("WSATRE025FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE026FVT() {
		callServlet("WSATRE026FVT");
	}

	@Test
	public void testWSATRE027FVT() {
		callServlet("WSATRE027VT");
	}

	@Test
	public void testWSATRE028FVT() {
		callServlet("WSATRE028FVT");
	}

	@Test
	public void testWSATRE029FVT() {
		callServlet("WSATRE029FVT");
	}

	@Test
	public void testWSATRE030FVT() {
		callServlet("WSATRE030FVT");
	}

	@Test
	public void testWSATRE031FVT() {
		callServlet("WSATRE031FVT");
	}

	@Test
	public void testWSATRE032FVT() {
		callServlet("WSATRE032FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE033FVT() {
		callServlet("WSATRE033FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE034FVT() {
		callServlet("WSATRE034FVT");
	}

	@Test
	public void testWSATRE035FVT() {
		callServlet("WSATRE035FVT");
	}

	@Test
	public void testWSATRE036FVT() {
		callServlet("WSATRE036FVT");
	}

	@Test
	public void testWSATRE037FVT() {
		callServlet("WSATRE037VT");
	}

	@Test
	public void testWSATRE038FVT() {
		callServlet("WSATRE038FVT");
	}

	@Test
	public void testWSATRE039FVT() {
		callServlet("WSATRE039FVT");
	}

	@Test
	public void testWSATRE040FVT() {
		callServlet("WSATRE040FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE041FVT() {
		callServlet("WSATRE041FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE042FVT() {
		callServlet("WSATRE042FVT");
	}

	@Test
	public void testWSATRE043FVT() {
		callServlet("WSATRE043FVT");
	}

	@Test
	public void testWSATRE044FVT() {
		callServlet("WSATRE044FVT");
	}

	@Test
	public void testWSATRE045FVT() {
		callServlet("WSATRE045FVT");
	}

	@Test
	public void testWSATRE046FVT() {
		callServlet("WSATRE046FVT");
	}

	@Test
	public void testWSATRE047FVT() {
		callServlet("WSATRE047VT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE048FVT() {
		callServlet("WSATRE048FVT");
	}

	@Test
	public void testWSATRE049FVT() {
		callServlet("WSATRE049FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE050FVT() {
		callServlet("WSATRE050FVT");
	}

	@Test
	public void testWSATRE051FVT() {
		callServlet("WSATRE051FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE052FVT() {
		callServlet("WSATRE052FVT");
	}

	@Test
	public void testWSATRE053FVT() {
		callServlet("WSATRE053FVT");
	}

	@Test
	public void testWSATRE054FVT() {
		callServlet("WSATRE054FVT");
	}

	@Test
	public void testWSATRE055FVT() {
		callServlet("WSATRE055FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE056FVT() {
		callServlet("WSATRE056FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE057FVT() {
		callServlet("WSATRE057VT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE058FVT() {
		callServlet("WSATRE058FVT");
	}

	@Test
	public void testWSATRE059FVT() {
		callServlet("WSATRE059FVT");
	}

	@Test
	public void testWSATRE060FVT() {
		callServlet("WSATRE060FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE061FVT() {
		callServlet("WSATRE061FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE062FVT() {
		callServlet("WSATRE062FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE063FVT() {
		callServlet("WSATRE063FVT");
	}

	private void callServlet(String testMethod){
		try {
			String urlStr = BASE_URL + "/simpleClient/SimpleClientServlet"
					+ "?method=" + Integer.parseInt(testMethod.substring(6, 9))
					+ "&baseurl=" + BASE_URL2;
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
