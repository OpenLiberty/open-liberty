/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.database.container.PostgreSQLContainer;
import componenttest.topology.impl.LibertyServer;

@AllowedFFDC(value = { "com.ibm.tx.jta.ut.util.AlreadyDumpedException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException" })
@RunWith(FATRunner.class)
public class DBRerouteRecoveryTest extends MultiRecoveryTest1 {

    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";
    public static PostgreSQLContainer testContainer;

	@Server("WSATRecovery3")
	public static LibertyServer server3;

	public static String[] serverNames = new String[] {"WSATRecoveryClient1", "WSATRecoveryServer1", "WSATRecovery3"};

	@BeforeClass
	public static void beforeTests() throws Exception {

		runner = new SetupRunner() {
	        @Override
	        public void run(LibertyServer s) throws Exception {
	        	Log.info(DBRerouteRecoveryTest.class, "setupRunner.run", "Setting up "+s.getServerName()+" for testcontainers");

	            //Get driver name
	            s.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

	            //Setup server DataSource properties
	            DatabaseContainerUtil.setupDataSourceDatabaseProperties(s, testContainer);

	            s.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
	        }
	    };

	    // The Dockerfile for 'jonhawkes/postgresql-ssl:1.0' can be found in the com.ibm.ws.jdbc_fat_postgresql project
	    testContainer = new PostgreSQLContainer("jonhawkes/postgresql-ssl:1.0")
	                    .withDatabaseName(POSTGRES_DB)
	                    .withUsername(POSTGRES_USER)
	                    .withPassword(POSTGRES_PASS)
	                    .withSSL()
	                    .withLogConsumer(new SimpleLogConsumer(DBRerouteRecoveryTest.class, "postgre-ssl"));

        testContainer.setStartupAttempts(2);
        testContainer.start();

//		System.getProperties().entrySet().stream().forEach(e -> Log.info(RerouteTest.class, "Properties", e.getKey() + " -> " + e.getValue()));
//		System.getenv().entrySet().stream().forEach(e -> Log.info(RerouteTest.class, "Environment", e.getKey() + " -> " + e.getValue()));

		BASE_URL = "http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort();

		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		server3.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_tertiary")));

		DBTestBase.initWSATTest(server1);
		DBTestBase.initWSATTest(server2);

        final WebArchive clientApp = ShrinkHelper.buildDefaultApp("recoveryClient", "client.*");
		ShrinkHelper.exportDropinAppToServer(server1, clientApp);
		ShrinkHelper.exportDropinAppToServer(server2, clientApp);

        final WebArchive serverApp = ShrinkHelper.buildDefaultApp("recoveryServer", "server.*");
		ShrinkHelper.exportDropinAppToServer(server1, serverApp);
		ShrinkHelper.exportDropinAppToServer(server2, serverApp);

		FATUtils.startServers(runner, server1, server2, server3);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server1, server2, server3);

		DBTestBase.cleanupWSATTest(server1);
		DBTestBase.cleanupWSATTest(server2);
		
		testContainer.stop();
	}

	@After
	public void sleep() throws InterruptedException {
		// Sleep a little to ensure stray async messages are all done
		Thread.sleep(5000);
	}
}
