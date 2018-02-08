/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Rule;
import org.junit.rules.TestName;

import componenttest.topology.impl.LibertyClient;

public abstract class AbstractAppClientTest {
	protected static LibertyClient client;
	
	@Rule
	public TestName name = new TestName();
	
	protected void assertClientStartMessages(String testClientName) throws Exception {
		assertFalse(
				"FAIL: Did not receive application started message:CWWKZ0001I",
				client.findStringsInCopiedLogs("CWWKZ0001I:.*").isEmpty());
		assertFalse("FAIL: Client should report installed features message:CWWKF0034I",
				client.findStringsInCopiedLogs("CWWKF0034I:.*" + "client").isEmpty());
		assertFalse(
				"FAIL: Did not receive client start message:CWWKF0035I",
				client.findStringsInCopiedLogs("CWWKF0035I:.*"
						+ testClientName).isEmpty());		
	}
	
	protected void assertClientAppMessage(String msg) throws Exception {
		assertFalse("FAIL: Did not receive " + msg + " message",
				client.findStringsInCopiedLogs(msg).isEmpty());			
	}
	
	protected void startClientWithArgs(List<String> args) throws Exception {
		args.add(0, "--");
		client.startClientWithArgs(true, true, true, false, "run", args, true);
	}
}
