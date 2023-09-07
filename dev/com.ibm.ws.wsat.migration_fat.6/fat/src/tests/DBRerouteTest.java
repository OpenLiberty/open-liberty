/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.database.container.PostgreSQLContainer;
import componenttest.topology.impl.LibertyServer;
import suite.FATSuite;

@RunWith(FATRunner.class)
public class DBRerouteTest extends SimpleTest {
    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";

    public static JdbcDatabaseContainer<?> testContainer;

	@Server("MigrationServer3")
	public static LibertyServer server3;

	public static String[] serverNames = new String[] {"MigrationServer1", "MigrationServer2", "MigrationServer3"};

	@BeforeClass
	public static void beforeTests() throws Exception {

		runner = new SetupRunner() {
	        @Override
	        public void run(LibertyServer s) throws Exception {
	        	Log.info(DBRerouteTest.class, "setupRunner.run", "Setting up "+s.getServerName()+" for testcontainers");

	            //Get driver name
	            s.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

	            //Setup server DataSource properties
	            DatabaseContainerUtil.setupDataSourceDatabaseProperties(s, testContainer);

	            s.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
	        }
	    };

        testContainer = new PostgreSQLContainer("jonhawkes/postgresql-ssl:1.0")
                .withDatabaseName(POSTGRES_DB)
                .withUsername(POSTGRES_USER)
                .withPassword(POSTGRES_PASS)
                .withSSL()
                .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "postgre-ssl"));
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

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server, server2, server3);

		DBTestBase.cleanupWSATTest(server);
		DBTestBase.cleanupWSATTest(server2);
		
		testContainer.stop();
	}
}
