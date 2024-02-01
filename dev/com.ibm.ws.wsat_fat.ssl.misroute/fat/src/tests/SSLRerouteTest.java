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

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATSecurityUtils;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class SSLRerouteTest extends SSLTest {
	
	static {
		startServers = false;
	}

	@Server("WSATSSL_Server3")
	public static LibertyServer server3;
	
	@BeforeClass
	public static void startThirdServer() throws Exception {
		Log.info(SSLRerouteTest.class, "startThirdServer", "");
		server3.setHttpDefaultPort(9083);
		
		// Create keys
		FATUtils.startServers(runner, server3);
		FATUtils.stopServers(server3);

//		FATSecurityUtils.createKeys(server3);
		FATSecurityUtils.extractPublicCertifcate(server3);
		FATSecurityUtils.establishTrust(client, server3);
		FATSecurityUtils.establishTrust(server1, server3);
		FATSecurityUtils.establishTrust(server2, server3);
		FATSecurityUtils.establishTrust(server3, client);
		FATSecurityUtils.establishTrust(server3, server1);
		FATSecurityUtils.establishTrust(server3, server2);

		// Start servers
		FATUtils.startServers(runner, client, server1, server2, server3);
	}
	
	@AfterClass
	public static void stopThirdServer() throws Exception {
		FATUtils.stopServers(server3);
		DBTestBase.cleanupWSATTest(server3);
	}
}