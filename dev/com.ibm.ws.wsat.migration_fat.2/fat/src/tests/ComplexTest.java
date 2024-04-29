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

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import web.simpleclient.ComplexClientServlet;

@AllowedFFDC(value = { "javax.transaction.SystemException" })
@RunWith(FATRunner.class)
public class ComplexTest extends DBTestBase {

	@Server("MigrationServer1")
    @TestServlet(servlet = ComplexClientServlet.class, contextRoot = "simpleClient")
	public static LibertyServer server1;

	@Server("MigrationServer2")
	public static LibertyServer server2;

	@Server("MigrationServer3")
	public static LibertyServer server3;

	@BeforeClass
	public static void beforeTests() throws Exception {

		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));
		server3.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_tertiary")));

		DBTestBase.initWSATTest(server1);
		DBTestBase.initWSATTest(server2);
		DBTestBase.initWSATTest(server3);

		ShrinkHelper.defaultDropinApp(server1, "simpleClient", "web.simpleclient");
		ShrinkHelper.defaultDropinApp(server2, "simpleService", "web.simpleservice");
		ShrinkHelper.defaultDropinApp(server3, "simpleService", "web.simpleservice");

		server1.setServerStartTimeout(START_TIMEOUT);
		server2.setServerStartTimeout(START_TIMEOUT);
		server3.setServerStartTimeout(START_TIMEOUT);

		final Duration meanStartTime = FATUtils.startServers(server1, server2, server3);
		final float perfFactor = (float)normalStartTime.getSeconds() / (float)meanStartTime.getSeconds();
		Log.info(ComplexTest.class, "beforeTests", "Mean startup time: "+meanStartTime+", Perf factor="+perfFactor);
		setTestQuerySuffix("perfFactor="+perfFactor);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers((String[])null, server1, server2, server3);

		DBTestBase.cleanupWSATTest(server1);
		DBTestBase.cleanupWSATTest(server2);
		DBTestBase.cleanupWSATTest(server3);
	}
}
