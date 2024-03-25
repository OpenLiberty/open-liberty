/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;

import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.RestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class JAXRSDynamicHandlerTest {
    @Server("RSTestServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("RSTestServer");
    private RestClient client;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "rsApplication", "com.ibm.samples.jaxrs");
    }
    @Before
    public void setUp() throws Exception {
        client = new RestClient();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testDynamicallyAddRemovalRSGlobalHandlers() throws Exception {

        server.startServer();
        URI uri = URI.create(TestUtils.getBaseTestUri("rsApplication", "hello"));
        ClientResponse response = client.resource(uri).get();
        assertEquals(200, response.getStatusCode());

        server.stopServer();
        //server.installUserBundle("RSHandler1_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle1", "com.ibm.ws.rsuserbundle1.myhandler");
        TestUtils.installUserFeature(server, "RSHandler1Feature");
        server.startServer();
        server.setMarkToEndOfLog();
        TestUtils.setServerConfigurationFile(server, "dynamicallyAddRemoveJAXRS/WithFirstOne/server.xml");
        assertNotNull("Expected to see config update completed", server.waitForStringInLog("CWWKG0017I"));
        assertNotNull("Expected to see feature update completed", server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("usr:RSHandler1Feature install failed", server.waitForStringInLog("CWWKF0012I.*usr:RSHandler1Feature"));
        if (JakartaEEAction.isEE10OrLaterActive()) {
            assertNotNull("Expected to see application rsApplication updated", server.waitForStringInLog("CWWKZ0003I: The application rsApplication updated"));
        } else {
            assertNull("Did not expect to see application rsApplication updated", server.waitForStringInLog("CWWKZ0003I: The application rsApplication updated"));
        }
        response = client.resource(uri).get();
        assertEquals(200, response.getStatusCode());
        assertNotNull("No RSInHander1 message", server.waitForStringInLog("in RSInHandler1 handleMessage method"));

        server.stopServer();
        //server.installUserBundle("RSHandler2_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle2", "com.ibm.ws.rsuserbundle2.myhandler");
        TestUtils.installUserFeature(server, "RSHandler2Feature");
        server.startServer();
        server.setMarkToEndOfLog();
        TestUtils.setServerConfigurationFile(server, "dynamicallyAddRemoveJAXRS/WithTwo/server.xml");
        assertNotNull("Expected to see config update completed", server.waitForStringInLog("CWWKG0017I"));
        assertNotNull("Expected to see feature update completed", server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("usr:RSHandler2Feature install failed", server.waitForStringInLog("CWWKF0012I.*usr:RSHandler2Feature"));
        response = client.resource(uri).get();
        assertEquals(200, response.getStatusCode());
        assertNotNull("No RSInHander1 message", server.waitForStringInLog("in RSInHandler1 handleMessage method"));
        assertNotNull("No RSInHander2 message", server.waitForStringInLog("in RSInHandler2 handleMessage method"));

        server.setMarkToEndOfLog();
        TestUtils.setServerConfigurationFile(server, "dynamicallyAddRemoveJAXRS/WithSecondOne/server.xml");
        assertNotNull("Expected to see config update completed", server.waitForStringInLog("CWWKG0017I"));
        assertNotNull("Expected to see feature update completed", server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("usr:RSHandler1Feature remove failed", server.waitForStringInLog("CWWKF0013I.*usr:RSHandler1Feature"));
        server.stopServer();
        server.uninstallUserBundle("rsUserBundle1");
        server.uninstallUserFeature("RSHandler1Feature");
        server.startServer();
        response = client.resource(uri).get();
        assertEquals(200, response.getStatusCode());
        assertNotNull("No RSInHander2 message", server.waitForStringInLog("in RSInHandler2 handleMessage method"));
        server.stopServer();
        server.uninstallUserBundle("rsUserBundle2");
        server.uninstallUserFeature("RSHandler2Feature");
    }

    @Test
    public void testGlobalHandlerFeatureOnly() throws Exception {
        //server.installUserBundle("RSHandler1_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle1", "com.ibm.ws.rsuserbundle1.myhandler");
        TestUtils.installUserFeature(server, "RSHandler1Feature");
        TestUtils.setServerConfigurationFile(server, "GlobalHandlerFeatureOnly/WithoutUserBundle/server.xml");
        server.startServer();
        server.setMarkToEndOfLog();
        TestUtils.setServerConfigurationFile(server, "GlobalHandlerFeatureOnly/WithUserBundle/server.xml");
        assertNotNull("Expected to see config update completed", server.waitForStringInLog("CWWKG0017I"));
        assertNotNull("Expected to see feature update completed", server.waitForStringInLog("CWWKF0008I"));
        // In EE10 the Global Handler function was removed from automatic inclusion in the restfulWSClient-3.1 and 
        // xml-40 features and into a protected feature.  This changed the behavior for this testcase such that
        // the protected feature causes the bundle to resolve.
        if (JakartaEEAction.isEE10OrLaterActive()) {
            assertNotNull("Expected to see application rsApplication updated", server.waitForStringInLog("CWWKZ0003I: The application rsApplication updated"));
            assertNull("Did not expect to see user bundle could not resolve", server.waitForStringInLog("CWWKF0029E", 10));
        } else {
            assertNull("Did not expect to see application rsApplication updated", server.waitForStringInLog("CWWKZ0003I: The application rsApplication updated"));
            assertNotNull("Expected to see user bundle could not resolve", server.waitForStringInLog("CWWKF0029E", 10));
        }
        //131606 restore to default server.xml which contains jaxrs-2.0 feature
        server.setMarkToEndOfLog();
        TestUtils.setServerConfigurationFile(server, "GlobalHandlerFeatureOnly/Default/server.xml");
        assertNotNull("Expected to see config update completed", server.waitForStringInLog("CWWKG0017I"));
        assertNotNull("Expected to see feature update completed", server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Expected to see application rsApplication updated", server.waitForStringInLog("CWWKZ0003I: The application rsApplication updated"));
        server.stopServer("CWWKF0029E");
    }
}
