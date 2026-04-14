/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class AppClientTest extends FATServletClient {

    public static EnterpriseArchive earHAC;

    public LibertyClient client;

    @BeforeClass
    public static void beforeClass() throws Exception {
        //HelloAppClient ear
        String APP_NAME = "HelloAppClient";

        // Build an application that uses ManagedBeans rather than CDI.
        JavaArchive jar = ShrinkHelper.buildJavaArchiveNoResources(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.HelloAppClient.test",
                                                                   "com.ibm.ws.clientcontainer.HelloAppClient.test.managedbean");

        // Add the ManagedBean resources.
        ShrinkHelper.addDirectory(jar, "test-applications/" + APP_NAME + ".jar" + "/resources");

        ShrinkHelper.addDirectory(jar, "test-applications/" + APP_NAME + ".jar" + "/resourcesCommon");

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
    @SkipForRepeat(SkipForRepeat.EE11_OR_LATER_FEATURES) // Uses ManagedBeans which are not available in Jakarta EE11+.
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
    @SkipForRepeat(SkipForRepeat.EE11_OR_LATER_FEATURES) // Uses ManagedBeans which are not available in Jakarta EE11+.
    public void testClientConfigDir() throws Exception {
        ShrinkHelper.exportAppToClient(client, earHAC);
        client.startClient();
        AppClientTestUtils.assertAppMessage("Hello Application Client.", client);
    }

    @Test
    public void testInAppClientContainerLookup() throws Exception {
        String APP_NAME = "InAppClientContainer";
        JavaArchive jar = ShrinkHelper.buildJavaArchive(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.InAppClientContainer.test");
        EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportAppToClient(client, app);

        client.startClient();
        AppClientTestUtils.assertAppMessage("We are in the client container", client);
    }

    @Test
    public void testSystemExitFromClientMain() throws Exception {
        //SystemExitClient ear
        String APP_NAME = "SystemExitClient";
        JavaArchive jar = ShrinkHelper.buildJavaArchive(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.SystemExitClient.test");
        EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportAppToClient(client, app);

        client.startClient();
        AppClientTestUtils.assertAppMessage("SystemExitClient main entry", client);
        AppClientTestUtils.assertNotAppMessage("SystemExitClient main exit", client); // client main method should have exited before this message
        AppClientTestUtils.assertAppMessage("CWWKE0084I.*testSystemExitFromClientMain.*java\\.lang\\.System\\.exit", client);
        AppClientTestUtils.assertNotAppMessage("This Liberty server has been running for too long", client);
    }

    @Test
    public void testSystemExitFromClientMainWithNoDD() throws Exception {
        String APP_NAME = "SystemExitClientNoDD";
        JavaArchive jar = ShrinkHelper.buildJavaArchive(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.SystemExitClientNoDD.test");
        EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportAppToClient(client, app);

        client.startClient();
        AppClientTestUtils.assertAppMessage("SystemExitClient main entry", client);
        AppClientTestUtils.assertNotAppMessage("SystemExitClient main exit", client); // client main method should have exited before this message
        AppClientTestUtils.assertAppMessage("CWWKE0084I.*testSystemExitFromClientMainWithNoDD.*java\\.lang\\.System\\.exit", client);
        AppClientTestUtils.assertNotAppMessage("This Liberty client has been running for too long", client);
    }

    // Use <enterpriseApplication/>
    @Test
    @SkipForRepeat(SkipForRepeat.EE11_OR_LATER_FEATURES) // Uses ManagedBeans which are not available in Jakarta EE11+.
    public void testHelloAppClientWithEnterpriseApplication() throws Exception {
        ShrinkHelper.exportAppToClient(client, earHAC);

        String appClientMsg = "Hello Application Client.";
        client.startClient();
        AppClientTestUtils.assertAppMessage(appClientMsg, client);
    }

    @Test
    public void testCallbackHandlerNoDefaultConstructor() throws Exception {
        String APP_NAME = "CallbackHandlerNoDefaultConstructor";
        JavaArchive jar = ShrinkHelper.buildJavaArchive(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.CallbackHandlerNoDefaultConstructor.test");
        EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportAppToClient(client, app);

        client.addIgnoreErrors("CWWKC2451E", // must have no-arg ctor
                               "CWWKZ0130E.*" + APP_NAME, // can't start app
                               "CWWKZ0002E.*" + APP_NAME); // exception occurred starting app
        client.startClient();
        AppClientTestUtils.assertAppMessage("CWWKC2451E:", client);
    }

    @Test
    public void testHelloAppClientNoClassDefFoundError() throws Exception {
        String APP_NAME = "HelloAppClientNCDF";
        JavaArchive jar = ShrinkHelper.buildJavaArchive(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.HelloAppClientNCDF.test");
        EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportAppToClient(client, app);

        client.addIgnoreErrors("CWWKZ0130E.*" + APP_NAME,
                               "CWWKZ0002E.*" + APP_NAME);
        client.startClient();
        AppClientTestUtils.assertAppMessage("CWWKZ0130E:", client); // Could not start application client for unknown callback handler.
    }

}
