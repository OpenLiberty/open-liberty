/*******************************************************************************
 * Copyright 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.utils.FATServletClient;

/**
 * Contains three tests from AppClientTest and uses CDI rather than ManagedBeans.
 */
@RunWith(FATRunner.class)
public class AppClientTestCDI extends FATServletClient {

    public static EnterpriseArchive earHAC;

    public LibertyClient client;

    @BeforeClass
    public static void beforeClass() throws Exception {
        //HelloAppClient ear
        String APP_NAME = "HelloAppClient";

        // Build an application that uses CDI rather than ManagedBeans.
        JavaArchive jar = ShrinkHelper.buildJavaArchiveNoResources(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.HelloAppClient.test",
                                                                   "com.ibm.ws.clientcontainer.HelloAppClient.test.cdi");

        // Add the CDI resources.
        ShrinkHelper.addDirectory(jar,
                                  "test-applications/" + APP_NAME + ".jar" + "/resourcesCDI");

        ShrinkHelper.addDirectory(jar,
                                  "test-applications/" + APP_NAME + ".jar" + "/resourcesCommon");

        earHAC = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"));
    }

    @Before
    public void getClient() throws Exception {
        client = LibertyClientFactory.getLibertyClient(getTestMethodSimpleName());
    }

    /*
     * Basic client launch test.
     * e.g., "client run com.ibm.ws.clientcontainer.fat.ClientContainerClient"
     * Check if the test application is printing out "Hello Application Client." to the console.
     */
    @Test
    public void testHelloAppClient() throws Exception {
        ShrinkHelper.exportAppToClient(client, earHAC);

        String cbhPostConstructMsg = "I have been in postConstruct of the callback handler.";
        String mainPostConstructMsg = "I have been in postConstruct of main.";
        String cbhPreDestroyMsg = "I have been in preDestroy of the callback handler.";
        String appClientMsg = "Hello Application Client.";
        client.startClient();
        AppClientTestUtils.assertAppMessage(cbhPostConstructMsg, client);
        AppClientTestUtils.assertAppMessage(mainPostConstructMsg, client);
        AppClientTestUtils.assertAppMessage(appClientMsg, client);
        AppClientTestUtils.assertAppMessage(cbhPreDestroyMsg, client);
    }

    // Test ${client.config.dir}
    @Test
    public void testClientConfigDir() throws Exception {
        ShrinkHelper.exportAppToClient(client, earHAC);
        client.startClient();
        AppClientTestUtils.assertAppMessage("Hello Application Client.", client);
    }

    // Use <enterpriseApplication/>
    @Test
    public void testHelloAppClientWithEnterpriseApplication() throws Exception {
        ShrinkHelper.exportAppToClient(client, earHAC);

        String appClientMsg = "Hello Application Client.";
        client.startClient();
        AppClientTestUtils.assertAppMessage(appClientMsg, client);
    }
}
