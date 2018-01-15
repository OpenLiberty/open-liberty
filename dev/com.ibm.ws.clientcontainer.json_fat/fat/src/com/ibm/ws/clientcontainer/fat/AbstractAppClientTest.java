/*******************************************************************************
 * Copyright (c) 2015,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

public abstract class AbstractAppClientTest {
	
	protected LibertyClient client;
    protected String testClientName = "com.ibm.ws.clientcontainer.jsonp.fat.ClientContainerClient";

	@Rule
	public TestName name = new TestName();
	
	protected void assertClientStartMessages() {
		assertNotNull(
				"FAIL: Did not receive application started message:CWWKZ0001I",
				client.waitForStringInCopiedLog("CWWKZ0001I:.*"));
		assertNotNull("FAIL: Client should report installed features: "
				+ client.waitForStringInCopiedLog("CWWKF0034I:.*" + "client"));
		assertNotNull(
				"FAIL: Did not receive client start message:CWWKF0035I",
				client.waitForStringInCopiedLog("CWWKF0035I:.*"
						+ testClientName));		
	}
	
	protected void assertClientAppMessage(String msg) {
		assertNotNull("FAIL: Did not receive" + msg + " message",
				client.waitForStringInCopiedLog(msg));			
	}
	
	protected void startClientWithArgs(List<String> args) throws Exception {
		args.add(0, "--");
		client.startClientWithArgs(true, true, true, false, "run", args, true);
	}
}
