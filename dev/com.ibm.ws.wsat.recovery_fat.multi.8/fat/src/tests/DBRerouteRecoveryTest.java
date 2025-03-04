/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

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
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.database.container.PostgreSQLContainer;
import componenttest.topology.impl.LibertyServer;

@AllowedFFDC(value = { "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "com.ibm.tx.jta.ut.util.AlreadyDumpedException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException" })
@RunWith(FATRunner.class)
public class DBRerouteRecoveryTest extends MultiRecoveryTest1 {

	@Server("WSATRecovery3")
	public static LibertyServer server3;

	public static String[] serverNames = new String[] {"WSATRecoveryClient1", "WSATRecoveryServer1", "WSATRecovery3"};

	@BeforeClass
	public static void beforeClass() throws Exception {
		Log.info(DBRerouteRecoveryTest.class, "beforeClass", "");

		runner = new SetupRunner() {
	        @Override
	        public void run(LibertyServer s) throws Exception {
	        	Log.info(DBRerouteRecoveryTest.class, "setupRunner.run", "Setting up "+s.getServerName()+" for testcontainers");

	            //Get driver name
	            s.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(TxTestContainerSuite.testContainer).getDriverName());

	            //Setup server DataSource properties
	            DatabaseContainerUtil.setupDataSourceDatabaseProperties(s, TxTestContainerSuite.testContainer);

	            s.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
	        }
	    };

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
	}

	@AfterClass
	public static void afterClass() throws Exception {
		Log.info(DBRerouteRecoveryTest.class, "afterClass", "");

		DBTestBase.cleanupWSATTest(server1);
		DBTestBase.cleanupWSATTest(server2);		
	}

	@Before
	public void before() throws Exception {
		Log.info(DBRerouteRecoveryTest.class, "before", "");
		FATUtils.startServers(runner, server1, server2, server3);
	}

	@After
	public void after() throws Exception {
		Log.info(DBRerouteRecoveryTest.class, "after", "");
		FATUtils.stopServers(server1, server2, server3);
	}
}
