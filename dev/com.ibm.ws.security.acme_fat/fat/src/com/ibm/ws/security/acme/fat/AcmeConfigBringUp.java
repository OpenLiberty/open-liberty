/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test doesn't do anything but bring up a server that is running
 * acmeCA-2.0 feature for manual analysis. This class can be removed or replaced
 * when additional FATs are added.
 */
@RunWith(FATRunner.class)
public class AcmeConfigBringUp extends FATServletClient {

	private static final Class<?> c = AcmeConfigBringUp.class;

	@Server("com.ibm.ws.security.acme.fat.acmeconfigbringup")
	public static LibertyServer server;

	@BeforeClass
	public static void setUp() throws Exception {
		server.startServer();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		server.stopServer();
	}

	@Test
	public void testBringUP() throws Exception {
		Log.info(c, "testBringUP", "Simple bring up test.");
	}
}
