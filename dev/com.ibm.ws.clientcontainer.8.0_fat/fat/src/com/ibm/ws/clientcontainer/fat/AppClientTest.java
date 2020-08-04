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
package com.ibm.ws.clientcontainer.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        JavaArchive jar = ShrinkHelper.buildJavaArchive(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.HelloAppClient.test");
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
    @SkipForRepeat({ "JAKARTAEECLIENT-9.0", SkipForRepeat.EE9_FEATURES })
    public void testHelloAppClient() throws Exception {
        ShrinkHelper.exportAppToClient(client, earHAC);

        String cbhPostConstructMsg = "I have been in postConstruct of the callback handler.";
        String mainPostConstructMsg = "I have been in postConstruct of main.";
        String cbhPreDestroyMsg = "I have been in preDestroy of the callback handler.";
        String appClientMsg = "Hello Application Client.";
        client.startClient();
        assertAppMessage(cbhPostConstructMsg);
        assertAppMessage(mainPostConstructMsg);
        assertAppMessage(appClientMsg);
        assertAppMessage(cbhPreDestroyMsg);
    }

    // Test ${client.config.dir}
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testClientConfigDir() throws Exception {
        ShrinkHelper.exportAppToClient(client, earHAC);
        client.startClient();
        assertAppMessage("Hello Application Client.");
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
        assertAppMessage("We are in the client container");
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
        assertAppMessage("SystemExitClient main entry");
        assertNotAppMessage("SystemExitClient main exit"); // client main method should have exited before this message
        assertAppMessage("CWWKE0084I.*testSystemExitFromClientMain.*java\\.lang\\.System\\.exit");
        assertNotAppMessage("This Liberty server has been running for too long");
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
        assertAppMessage("SystemExitClient main entry");
        assertNotAppMessage("SystemExitClient main exit"); // client main method should have exited before this message
        assertAppMessage("CWWKE0084I.*testSystemExitFromClientMainWithNoDD.*java\\.lang\\.System\\.exit");
        assertNotAppMessage("This Liberty client has been running for too long");
    }

    // Use <enterpriseApplication/>
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testHelloAppClientWithEnterpriseApplication() throws Exception {
        ShrinkHelper.exportAppToClient(client, earHAC);

        String appClientMsg = "Hello Application Client.";
        client.startClient();
        assertAppMessage(appClientMsg);
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
        assertAppMessage("CWWKC2451E:");
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
        assertAppMessage("CWWKZ0130E:"); // Could not start application client for unknown callback handler.
    }

    private void assertAppMessage(String msg) {
        assertNotNull("Did not find message in client logs: " + msg, client.waitForStringInCopiedLog(msg));
    }

    private void assertNotAppMessage(String msg) throws Exception {
        assertEquals("Shoudl NOT have found the following msg in logs: Detected \"" + msg + "\" message, but did not expect it", 0, client.findStringsInCopiedLogs(msg).size());
    }
}
