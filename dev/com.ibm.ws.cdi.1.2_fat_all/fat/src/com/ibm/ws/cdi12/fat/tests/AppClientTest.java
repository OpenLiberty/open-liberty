/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileWriter;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.ClientOnly;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;

@ClientOnly
public class AppClientTest {
    private static final Class<AppClientTest> c = AppClientTest.class;
    private final String testClientName = "cdiClient";
    private final LibertyClient client = LibertyClientFactory.getLibertyClient(testClientName);

    /*
     * Basic client launch test.
     * e.g., "client run com.ibm.ws.clientcontainer.fat.ClientContainerClient"
     * Check if the test application is printing out "Hello Application Client." to the console.
     */
    @Test
    public void testHelloAppClient() throws Exception {

        client.startClient();

        String featuresMessage = client.waitForStringInCopiedLog("CWWKF0034I", 0);
        assertNotNull("Did not receive features loaded message", featuresMessage);
        assertTrue("CDI was not among the loaded features", featuresMessage.contains("cdi-"));

        assertNotNull("Did not recieve app started message",
                      client.waitForStringInCopiedLog("Client App Start", 0));

//        assertNotNull("Did not get BeanManager from CDI",
//                      client.waitForStringInCopiedLog("Got BeanManager from CDI", 0));

        assertNotNull("Did not get BeanManager from JNDI",
                      client.waitForStringInCopiedLog("Got BeanManager from JNDI", 0));

        assertNotNull("Did not get the AppBean",
                      client.waitForStringInCopiedLog("Got AppBean", 0));

        assertNotNull("Did not receive the bean message",
                      client.waitForStringInCopiedLog("Bean hello", 10000));

        assertNotNull("Did not receive the app ended message",
                      client.waitForStringInCopiedLog("Client App End", 0));
    }

    public static void addServerPortsToClientBootStrapProp(LibertyClient client, LibertyServer server) throws Exception {

        String thisMethod = "addServerPortsToClientBootStrapProp";

        // add properties to bootstrap.properties
        String bootProps = client.getClientRoot() + "/bootstrap.properties";
        Log.info(c, thisMethod, "client property File: " + bootProps);
        // append to bootstrap.properties
        FileWriter writer = new FileWriter(bootProps, true);
        writer.append(System.getProperty("line.separator"));
        writer.append("ServerPort=" + server.getHttpDefaultPort());
        writer.append(System.getProperty("line.separator"));
        writer.append("ServerSecurePort=" + server.getHttpDefaultSecurePort());
        writer.append(System.getProperty("line.separator"));
        writer.close();

    }
}
