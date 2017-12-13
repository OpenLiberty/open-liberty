/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.clientcontainer.fat;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyClient;

public abstract class AbstractAppClientTest {
	protected static LibertyClient client;
	
    @ClassRule
    public static TestRule java7Rule = new OnlyRunInJava7Rule();

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
