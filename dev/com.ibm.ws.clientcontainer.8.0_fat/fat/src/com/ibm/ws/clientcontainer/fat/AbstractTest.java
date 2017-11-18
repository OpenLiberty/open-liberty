/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.ProgramOutput;

import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

public abstract class AbstractTest {
    @ClassRule
    public static TestRule java7Rule = new OnlyRunInJava7Rule();

    protected String testClientName = "com.ibm.ws.clientcontainer.fat.ClientContainerClient";
    protected LibertyClient client = LibertyClientFactory.getLibertyClient(getClientName());
    // protected String testServerName = "com.ibm.ws.clientcontainer.fat.ClientContainerServer";
    //protected LibertyServer server = LibertyServerFactory.getLibertyServer(getServerName());

    @Rule
    public TestName name = new TestName();

    protected ProgramOutput startProcess() throws Exception {
        return startProcess(true);
    }

    protected ProgramOutput startProcess(boolean isClient) throws Exception {
        return client.startClient();
    }

    protected void startProcessWithArgs(List<String> args) throws Exception {
        startProcessWithArgs(true, args);
    }

    protected void startProcessWithArgs(boolean isClient, List<String> args) throws Exception {
        if (isClient) {
            args.add(0, "--");
            client.startClientWithArgs(true, true, true, false, "run", args, true);
        } else {
            startProcess(false);
        }
    }

    protected void stopProcess() throws Exception {
        stopProcess(true);
    }

    protected void stopProcess(boolean isClient) throws Exception {
//        if (isClient) {
//            // nothing to do
//        } else {
//            // stop server.
//            if (server.isStarted()) {
//                server.stopServer("CWWk*");
//            }
//        }
    }

    protected void assertStartMessages() {
        assertStartMessages(true);
    }

    protected void assertStartMessages(boolean isClient) {
        if (isClient) {
            // assert client start messages

//			Skip the following two asserts to save running time.
//			assertNotNull("FAIL: Did not receive application started message:CWWKZ0001I",
//					client.waitForStringInCopiedLog("CWWKZ0001I:.*"));
//			assertNotNull("FAIL: Client should report installed features: "
//					+ client.waitForStringInCopiedLog("CWWKF0034I:.*" + "client"));
            assertNotNull("FAIL: Did not receive client running message:CWWKF0035I",
                          client.waitForStringInCopiedLog("CWWKF0035I:.*" + testClientName));
        } else {
            // assert server start messages
            //assertNotNull("FAIL: Did not receive smarter planet message:CWWKF0011I", server.waitForStringInLog("CWWKF0011I"));
        }
    }

    protected void assertAppMessage(String msg) {
        assertNotNull("FAIL: Did not receive " + msg + " message", client.waitForStringInCopiedLog(msg));
    }

    protected void assertNotAppMessage(String msg) throws Exception {
        assertEquals("FAIL: Detected \"" + msg + "\" message, but did not expect it", 0, client.findStringsInCopiedLogs(msg).size());
    }

    protected String getClientName() {
        return testClientName;
    }

    protected String getServerName() {
        return "";
        //return testServerName;
    }
}
