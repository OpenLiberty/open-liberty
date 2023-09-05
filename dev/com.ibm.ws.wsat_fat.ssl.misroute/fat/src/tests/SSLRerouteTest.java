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

import java.util.Hashtable;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class SSLRerouteTest extends SSLTest {

	@Server("WSATSSL_Server3")
	public static LibertyServer server3;
	
	@BeforeClass
	public static void startThirdServer() throws Exception {
		server3.setHttpDefaultPort(9083);
		FATUtils.startServers(runner, server3);
	}
	
	@AfterClass
	public static void stopThirdServer() throws Exception {
		FATUtils.stopServers(server3);
		DBTestBase.cleanupWSATTest(server3);
	}
}