/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.fat;

import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat({"EE9_FEATURES", "EE10_FEATURES"}) // this test uses a CXF-specific property that is not applicable in RESTEasy (EE9)
public class ServiceScopeTest {

    @Server("com.ibm.ws.jaxrs.fat.service.scope")
    public static LibertyServer server;

    private static final String war = "servicescope";
    private RestClient client;


    @BeforeClass
    public static void setupClass() throws Exception {
        ShrinkHelper.defaultApp(server, war, "test.service.scope");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
            assertNotNull("The server did not start", server.waitForStringInLog("CWWKF0011I"));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void setUp() {
        ClientConfig config = new ClientConfig();
        config.connectTimeout(120000);
        config.readTimeout(120000);
        client = new RestClient(config);
    }

    @After
    public void after() {

    }
    /**
     * tests that the @PreDestroy method is invoked after the container response filter
     */
    @Test
    public void testPreDestroyInvokedAfterFilter_AppProperty() throws Exception {
        doTestPreDestroyInvokedAfterFilter("appProp");
    }

    @Test
    public void testPreDestroyInvokedAfterFilter_WebXmlProperty() throws Exception {
        doTestPreDestroyInvokedAfterFilter("webXmlProp");
    }

    @Test
    public void testPreDestroyInvokedAfterFilter_WebXmlPropertyShorterSyntax() throws Exception {
        doTestPreDestroyInvokedAfterFilter("webXmlPropShort");
    }

    private void doTestPreDestroyInvokedAfterFilter(String app) {
        Resource resource = client.resource(getBaseTestUri(war, app + "/resource/initial"));

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals("SUCCESS", response.getEntity(String.class));
        assertEquals(200, response.getStatusCode());

        resource = client.resource(getBaseTestUri(war, app + "/resource/verify"));

        response = resource.accept("text/plain").get();
        assertEquals("SUCCESS", response.getEntity(String.class));
        assertEquals(200, response.getStatusCode());

        // reset for next text
        resource = client.resource(getBaseTestUri(war, app + "/resource/reset"));
        response = resource.accept("text/plain").get();
        assertEquals(200, response.getStatusCode());
    }

}
