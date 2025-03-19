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

import java.time.Duration;

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

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import web.simpleclient.SimpleClientServlet;

@RunWith(FATRunner.class)
public class SimpleTest extends DBTestBase {

	@Server("MigrationServer1")
    @TestServlet(servlet = SimpleClientServlet.class, contextRoot = "simpleClient")
	public static LibertyServer server;

	@Server("MigrationServer2")
	public static LibertyServer server2;

    protected static SetupRunner runner;

	public static String[] serverNames = new String[] {"MigrationServer1", "MigrationServer2"};

	@BeforeClass
	public static void beforeTests() throws Exception {

		System.getProperties().entrySet().stream().forEach(e -> Log.info(SimpleTest.class, "beforeTests", e.getKey() + " -> " + e.getValue()));

		runner = new SetupRunner() {
	        @Override
	        public void run(LibertyServer s) throws Exception {
	        	Log.info(SimpleTest.class, "setupRunner.run", "Setting up "+s.getServerName());
	            s.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
	        }
	    };

		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));

		DBTestBase.initWSATTest(server);
		DBTestBase.initWSATTest(server2);

		ShrinkHelper.defaultDropinApp(server, "simpleClient", "web.simpleclient");
		ShrinkHelper.defaultDropinApp(server2, "simpleService", "web.simpleservice");

		final Duration meanStartTime = FATUtils.startServers(runner, server, server2);
		final float perfFactor = (float)normalStartTime.getSeconds() / (float)meanStartTime.getSeconds();
		Log.info(SimpleTest.class, "beforeTests", "Mean startup time: "+meanStartTime+", Perf factor="+perfFactor);
		if (perfFactor < 1f) {
			setTestQuerySuffix("perfFactor="+perfFactor);
		}
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server, server2);

		DBTestBase.cleanupWSATTest(server);
		DBTestBase.cleanupWSATTest(server2);
		
		setTestQuerySuffix(null);
	}

	@After
	public void sleep() throws InterruptedException {
		// Sleep a little to ensure stray async messages are all done
		Thread.sleep(5000);
	}
	
	@Test
	public void testAsyncResponseTimeoutSetting() throws Exception {
		FATUtils.stopServers(server);
		FATUtils.startServers(runner, server);
		server.resetLogMarks();
		assertNotNull("asyncResponseTimeout not overridden", server.waitForStringInTraceUsingMark("asyncResponseTimeout setting overridden to [0-9]* by com.ibm.ws.wsat.asyncResponseTimeout system property"));
	}
}