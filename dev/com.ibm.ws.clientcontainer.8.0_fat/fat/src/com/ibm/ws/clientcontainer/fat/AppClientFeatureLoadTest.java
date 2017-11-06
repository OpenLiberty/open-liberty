/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import junit.framework.Assert;

public class AppClientFeatureLoadTest extends AbstractTest {

    /*
     * Pass a command-line argument to the test client application.
     * e.g., "client run com.ibm.ws.clientcontainer.fat.ClientContainerClient -- Add"
     * Check if the test application is printing "Add" back to the console.
     */
    @Test
    public void testCmdArgs() throws Exception {
        client.copyFileToLibertyClientRoot("clients/HelloAppClientCmdArgs/client.xml");
        String appClientMsg = "Hello Application Client.";
        String arg = "Add";

        List<String> args = new ArrayList<String>();
        args.add(arg);
        startProcessWithArgs(args);

        assertStartMessages();
        assertAppMessage(appClientMsg);
        assertAppMessage(arg);
    }

    /*
     * Feature load test
     * 1) Check for missing features
     * 2) Check for unresolved modules
     */
    @Test
    public void testClientFeatureLoad() throws Exception {
        client.copyFileToLibertyClientRoot("clients/javaeeClient/client.xml");
        String appClientMsg = "Hello Application Client.";
        startProcess();
        assertStartMessages();
        assertAppMessage(appClientMsg);
        assertMissingFeatures();
        //assertUnresolvedModules();
    }

    private void assertMissingFeatures() {
        Assert.assertNull(
                          "FAIL: A feature definition could not be found:CWWKF0042E:",
                          client.waitForStringInCopiedLog("CWWKF0042E:.*", 15000));
        Assert.assertNull(
                          "FAIL: The singleton features cannot be loaded at the same time:CWWKF0033E:",
                          client.waitForStringInCopiedLog("CWWKF0033E:.*", 15000));
    }

//	private void assertUnresolvedModules() {
//		Assert.assertNull(
//				"FAIL: Could not resolve module:CWWKE0702E:",
//				client.waitForStringInCopiedLog("CWWKE0702E:.*",15000));
//	}
}
