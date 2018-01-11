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

import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;

public class AppClientTest extends AbstractTest {

    /*
     * Basic client launch test.
     * e.g., "client run com.ibm.ws.clientcontainer.fat.ClientContainerClient"
     * Check if the test application is printing out "Hello Application Client." to the console.
     */
    @Test
    public void testHelloAppClient() throws Exception {
        client.copyFileToLibertyClientRoot("clients/HelloAppClient/client.xml");
        String cbhPostConstructMsg = "I have been in postConstruct of the callback handler.";
        String mainPostConstructMsg = "I have been in postConstruct of main.";
        String cbhPreDestroyMsg = "I have been in preDestroy of the callback handler.";
        String appClientMsg = "Hello Application Client.";
        startProcess();
        assertStartMessages();
        assertAppMessage(cbhPostConstructMsg);
        assertAppMessage(mainPostConstructMsg);
        assertAppMessage(appClientMsg);
        assertAppMessage(cbhPreDestroyMsg);
    }

    // Test ${client.config.dir}
    @Test
    public void testClientConfigDir() throws Exception {
        client.copyFileToLibertyClientRoot("clients/ClientConfigDir/client.xml");
        String appClientMsg = "Hello Application Client.";
        startProcess();
        assertStartMessages();
        assertAppMessage(appClientMsg);
    }

    @Test
    public void testInAppClientContainerLookup() throws Exception {
        client.copyFileToLibertyClientRoot("clients/InAppClientContainer/client.xml");
        String appClientMsg = "We are in the client container";
        startProcess();
        assertStartMessages();
        assertAppMessage(appClientMsg);
    }

    @Test
    public void testSystemExitFromClientMain() throws Exception {
        client.copyFileToLibertyClientRoot("clients/SystemExitClient/client.xml");
        ProgramOutput po = startProcess();
        System.out.println("System.out:\n" + po.getStdout());
        System.out.println("System.err:\n" + po.getStderr());
        assertStartMessages();
        assertAppMessage("SystemExitClient main entry");
        assertNotAppMessage("SystemExitClient main exit"); // client main method should have exited before this message
        assertAppMessage("CWWKE0084I:(?=.*com.ibm.ws.clientcontainer.fat.ClientContainerClient)(?=.*main)");
        assertNotAppMessage("This Liberty server has been running for too long");
    }

    @Test
    public void testSystemExitFromClientMainWithNoDD() throws Exception {
        client.copyFileToLibertyClientRoot("clients/SystemExitClientNoDD/client.xml");
        ProgramOutput po = startProcess();
        System.out.println("System.out:\n" + po.getStdout());
        System.out.println("System.err:\n" + po.getStderr());
        assertStartMessages();
        assertAppMessage("SystemExitClient main entry");
        assertNotAppMessage("SystemExitClient main exit"); // client main method should have exited before this message
        assertAppMessage("CWWKE0084I:(?=.*com.ibm.ws.clientcontainer.fat.ClientContainerClient)(?=.*main)");
        assertNotAppMessage("This Liberty client has been running for too long");
    }

    // Use <enterpriseApplication/>
    @Test
    public void testHelloAppClientWithEnterpriseApplication() throws Exception {
        client.copyFileToLibertyClientRoot("clients/EnterpriseApplication/client.xml");
        String appClientMsg = "Hello Application Client.";
        startProcess();
        assertStartMessages();
        assertAppMessage(appClientMsg);
    }

    @Test
    public void testCallbackHandlerNoDefaultConstructor() throws Exception {
        client.copyFileToLibertyClientRoot("clients/CallbackHandlerNoDefaultConstructor/client.xml");
        String errorMsg = "CWWKC2451E:";
        startProcess();
        assertAppMessage(errorMsg);
    }

    @Test
    public void testHelloAppClientNoClassDefFoundError() throws Exception {
        client.copyFileToLibertyClientRoot("clients/HelloAppClientNCDF/client.xml");
        String errorMsg = "CWWKZ0130E:"; // Could not start application client for unknown callback handler.
        startProcess();
        assertAppMessage(errorMsg);
    }
}
