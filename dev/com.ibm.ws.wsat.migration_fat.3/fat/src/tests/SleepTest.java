/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import web.simpleclient.SleepClientServlet;

@RunWith(FATRunner.class)
public class SleepTest extends DBTestBase {

	private static final float MIN_PERF_FACTOR = 0.3f;

	@Server("WSATSleep")
    @TestServlet(servlet = SleepClientServlet.class, contextRoot = "simpleClient")
	public static LibertyServer server;

	@Server("MigrationServer2")
	public static LibertyServer server2;

	@BeforeClass
	public static void beforeTests() throws Exception {
		System.getProperties().entrySet().stream().forEach(e -> Log.info(SleepTest.class, "beforeTests", e.getKey() + " -> " + e.getValue()));

		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));

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

		final Duration meanStartTime = FATUtils.startServers(server, server2);
		if (meanStartTime.compareTo(normalStartTime) > 0) {
			float perfFactor = (float)normalStartTime.getSeconds() / (float)meanStartTime.getSeconds();
			if (perfFactor < MIN_PERF_FACTOR) {
				perfFactor = MIN_PERF_FACTOR;
			}
			Log.info(SleepTest.class, "beforeTests", "Mean startup time: "+meanStartTime+", Perf factor="+perfFactor);
			setTestQuerySuffix("perfFactor="+perfFactor);
		}
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server, server2);

		DBTestBase.cleanupWSATTest(server);
		DBTestBase.cleanupWSATTest(server2);
	}
}