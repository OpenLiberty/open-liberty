/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.client;

import java.io.File;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs20.fat.AbstractTest;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class ClientTest extends AbstractTest {

    @Server("com.ibm.ws.jaxrs.fat.client")
    public static LibertyServer server;

    private static final String clientwar = "client";
    private final static String target = clientwar + "/TestServlet";
    private static final String jsonLib = "publish/shared/resources/json/json-20080701.jar";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(clientwar, "com.ibm.ws.jaxrs.fat.client.echoapp",
                                                      "com.ibm.ws.jaxrs.fat.client.jaxb",
                                                      "com.ibm.ws.jaxrs.fat.client.timeout");
        app.addAsLibrary(new File(jsonLib));
        ShrinkHelper.exportDropinAppToServer(server, app);
        server.addInstalledAppForValidation(clientwar);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKW0061W");
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    /*
     * test that client configuration inside a server app respects the webtarget declarations in server.xml
     * see publish\servers\com.ibm.ws.jaxrs.fat.client\server.xml for the declarations we're testing against.
     * Note that these tests don't actually invoke the client and test the functionality of the properties,
     * that is covered elsewhere. We only verify that the right properties make it out of server.xml
     * and onto the WebTarget instance.
     *
     */
    @Test
    public void testClientConfigPropertyMerging() throws Exception {
        this.runTestOnServer(target, "testClientConfigPropertyMerging", null, "OK");
    }

    /*
     * for the 3 sso property shortnames, check that the correct long name is set.
     */
    @Test
    public void testClientConfigPropertiesSaml() throws Exception {
        this.runTestOnServer(target, "testClientConfigPropertiesSaml", null, "OK");
    }

    @Test
    public void testClientConfigPropertiesOauth() throws Exception {
        this.runTestOnServer(target, "testClientConfigPropertiesOauth", null, "OK");
    }

    @Test
    public void testClientConfigPropertiesLtpa() throws Exception {
        this.runTestOnServer(target, "testClientConfigPropertiesLtpa", null, "OK");
    }

    // in this case there should be no authntoken in the properties and we should have a warning message in the lgos
    @Test
    public void testClientConfigPropertiesBogus() throws Exception {
        this.runTestOnServer(target, "testClientConfigPropertiesBogus", null, "OK");
    }

    /*
     * test that proxy param shortnames are correctly translated to long name
     */
    @Test
    public void testClientConfigProxyProps() throws Exception {
        this.runTestOnServer(target, "testClientConfigProxyProps", null, "OK");
    }

    /*
     * test that timeout shortnames are correctly translated and inserted.
     */
    @Test
    public void testClientConfigPropertiesTimeouts() throws Exception {
        this.runTestOnServer(target, "testClientConfigPropertiesTimeouts", null, "OK");
    }

    @Test
    public void testClientConfigPropertiesMisc() throws Exception {
        this.runTestOnServer(target, "testClientConfigPropertiesMisc", null, "OK");
    }

    /**
     * Test that the system property for the read timeout can be overwritten
     * programatically
     */
    @Test
    public void testOverrideReadTimeout() throws Exception {
        this.runTestOnServer(target, "testOverrideReadTimeout", null, "OK");
    }

    /**
     * Test that the system property for the connect timeout can be overwritten
     * programmatically
     */
    @Test
    public void testOverrideConnectTimeout() throws Exception {
        this.runTestOnServer(target, "testOverrideConnectTimeout", null, "OK");
    }

    /**
     * Test that a request is processed if it takes less time than the timeout
     * value
     */
    @Mode(TestMode.FULL)
    @Test
    public void testReadTimeoutNoTimeout() throws Exception {
        this.runTestOnServer(target, "testReadTimeoutNoTimeout", null, "OK");
    }

    /**
     * Test that the client times out if the request is not processed in less
     * than the readTimeout value
     */
    @Mode(TestMode.FULL)
    @Test
    public void testReadTimeoutTimeout() throws Exception {
        this.runTestOnServer(target, "testReadTimeoutTimeout", null, "OK");
    }

    /**
     * If the Accept header is already set, then the Accept Header handler
     * should not attempt to set it. This is particularly useful for types like
     * String which would do MediaType.WILDCARD.
     *
     * @throws JSONException
     * @throws JAXBException
     */
    ////@Test
    // Test won't work because no serializer for class org.json.JSONObject
    public void testAcceptHeaderSet() throws Exception {
        this.runTestOnServer(target, "testAcceptHeaderSet", null, "OK");
    }

    /**
     * If the Accept header is not set, then let the AcceptHeaderHandler set it automatically.
     */
    @Test
    public void testAcceptHeaderNotSetString() throws Exception {
        this.runTestOnServer(target, "testAcceptHeaderNotSetString", null, "OK");
    }

    /**
     * If no entity class is specified in the initial GET, then the AcceptHeaderHandler should not set anything. However, the
     * underlying client may set the header as a failsafe.
     */
    @Test
    public void testAcceptHeaderNoEntity() throws Exception {
        this.runTestOnServer(target, "testAcceptHeaderNoEntity", null, "OK");
    }

    /**
     * For JAXB objects, the AcceptHeaderHandler should automatically
     * take care of the Accept header.
     */
    @Test
    public void testAcceptHeaderForJAXB() throws Exception {
        this.runTestOnServer(target, "testAcceptHeaderForJAXB", null, "OK");
    }

    /**
     * For JSON objects, the AcceptHeaderHandler should automatically
     * take care of the Accept header.
     */
    ////@Test
    // Test won't work because no serializer for class org.json.JSONObject
    public void testAcceptHeaderForJSON() throws Exception {
        this.runTestOnServer(target, "testAcceptHeaderForJSON", null, "OK");
    }

    /**
     * If the Accept header is not set, then let the client set a default to
     * send. In regular RestClient, it is set to {@link MediaType.WILDCARD}
     */
    @Test
    public void testNoAcceptHeaderNotSetString() throws Exception {
        this.runTestOnServer(target, "testNoAcceptHeaderNotSetString", null, "OK");
    }

    /**
     * If no entity class is specified in the initial GET, then the AcceptHeaderHandler should not set anything. However, the
     * underlying client may set the header as a failsafe.
     *
     * @throws JSONException
     */
    @Test
    public void testNoAcceptHeaderNoEntity() throws Exception {
        this.runTestOnServer(target, "testNoAcceptHeaderNoEntity", null, "OK");
    }

    /**
     * For JAXB objects, there will be an error as the resource will return a
     * text/plain representation.
     */
    @Test
    public void testNoAcceptHeaderForJAXB() throws Exception {
        this.runTestOnServer(target, "testNoAcceptHeaderForJAXB", null, "OK");
    }
}
