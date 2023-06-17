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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@AllowedFFDC(value = { "com.ibm.tx.jta.ut.util.AlreadyDumpedException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException" })
@RunWith(FATRunner.class)
public class RerouteRecoveryTest extends MultiRecoveryTest1 {

	@Server("WSATRecovery3")
	public static LibertyServer server3;
	
	@BeforeClass
	public static void startThirdServer() throws Exception {
		server3.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_tertiary")));
		FATUtils.startServers(runner, server3);
	}
	
	@AfterClass
	public static void stopThirdServer() throws Exception {
		FATUtils.stopServers(server3);
	}
}
