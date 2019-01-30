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
package com.ibm.ws.jaxrs20.fat.securitycontext;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.securitycontext.xml.ObjectFactory;
import com.ibm.ws.jaxrs.fat.securitycontext.xml.SecurityContextInfo;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class SecurityContextTest {

    @Server("com.ibm.ws.jaxrs.fat.security")
    public static LibertyServer server;

    private static HttpClient client;
    private static final String secwar = "security";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, secwar, "com.ibm.ws.jaxrs.fat.security.annotations",
                                      "com.ibm.ws.jaxrs.fat.security.ssl",
                                      "com.ibm.ws.jaxrs.fat.securitycontext",
                                      "com.ibm.ws.jaxrs.fat.securitycontext.xml");
        ShrinkHelper.defaultApp(server, secwar + "NoWebXml", "com.ibm.ws.jaxrs.fat.security.annotations",
                                "com.ibm.ws.jaxrs.fat.security.ssl",
                                "com.ibm.ws.jaxrs.fat.securitycontext",
                                "com.ibm.ws.jaxrs.fat.securitycontext.xml");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
            assertNotNull("The Security Service should be ready", server.waitForStringInLog("CWWKS0008I"));
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
    public void getHttpClient() {
        client = new DefaultHttpClient();
    }

    @After
    public void resetHttpClient() {
        client.getConnectionManager().shutdown();
    }

    // "/context/securitycontext" is common to all the Security*Resource @Path values,
    // differing only in the ending string after the last forward slash.
    // For this test, the url-pattern specified in web.xml is just /*
    // to simplify the URL, because otherwise it gets too crowded with duplicate strings.
    // e.g., http://localhost:8010/security/context/securitycontext/param is URL to
    // call GET, and SecurityContextParamResource is invoked.
    private String getSecTestUri() {
        String uri = getBaseTestUri(secwar, "context/securitycontext");
        return uri;
    }

    /**
     * Tests that a security context can be injected via a parameter.
     *
     */
    @Test
    public void testSecurityContextParamResource() throws Exception {
        String uri = getSecTestUri() + "/param";
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        JAXBContext context =
                        JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        SecurityContextInfo secContextInfo =
                        (SecurityContextInfo) context.createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertNotNull(secContextInfo);
        assertEquals(false, secContextInfo.isSecure());
        assertEquals(false, secContextInfo.isUserInRoleAdmin());
        assertEquals(false, secContextInfo.isUserInRoleNull());
        assertEquals(false, secContextInfo.isUserInRoleUser());
        assertEquals("null", secContextInfo.getUserPrincipal());
        assertNull(secContextInfo.getAuthScheme(), secContextInfo.getAuthScheme());
    }

    /**
     * Tests that a security context can be injected via a constructor.
     *
     */
    @Test
    public void testSecurityContextConstructorResource() throws Exception,
                    JAXBException {
        HttpGet getMethod = new HttpGet(getSecTestUri() + "/constructor");
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        JAXBContext context =
                        JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        SecurityContextInfo secContextInfo =
                        (SecurityContextInfo) context.createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertNotNull(secContextInfo);
        assertEquals(false, secContextInfo.isSecure());
        assertEquals(false, secContextInfo.isUserInRoleAdmin());
        assertEquals(false, secContextInfo.isUserInRoleNull());
        assertEquals(false, secContextInfo.isUserInRoleUser());
        assertEquals("null", secContextInfo.getUserPrincipal());
        assertNull(secContextInfo.getAuthScheme(), secContextInfo.getAuthScheme());
    }

    /**
     * Tests that a security context can be injected via a bean method.
     *
     */
    //TODO
    //@Test
    public void testSecurityContextBeanResource() throws Exception {
        HttpGet getMethod = new HttpGet(getSecTestUri() + "/bean");
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        JAXBContext context =
                        JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        SecurityContextInfo secContextInfo =
                        (SecurityContextInfo) context.createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertNotNull(secContextInfo);
        assertEquals(false, secContextInfo.isSecure());
        assertEquals(false, secContextInfo.isUserInRoleAdmin());
        assertEquals(false, secContextInfo.isUserInRoleNull());
        assertEquals(false, secContextInfo.isUserInRoleUser());
        assertEquals("null", secContextInfo.getUserPrincipal());
        assertNull(secContextInfo.getAuthScheme(), secContextInfo.getAuthScheme());
    }

    /**
     * Tests that a security context will not be injected into non-bean methods.
     *
     */
    @Test
    public void testSecurityContextNotBeanResource() throws Exception {
        HttpGet getMethod =
                        new HttpGet(getSecTestUri() + "/notbeanmethod");
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals(asString(resp), "false");
    }

    /**
     * Tests that a security context can be injected via a member field.
     *
     */
    @Test
    public void testSecurityContextFieldResource() throws Exception {
        HttpGet getMethod = new HttpGet(getSecTestUri() + "/field");
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        JAXBContext context =
                        JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        SecurityContextInfo secContextInfo =
                        (SecurityContextInfo) context.createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertNotNull(secContextInfo);
        assertEquals(false, secContextInfo.isSecure());
        assertEquals(false, secContextInfo.isUserInRoleAdmin());
        assertEquals(false, secContextInfo.isUserInRoleNull());
        assertEquals(false, secContextInfo.isUserInRoleUser());
        assertEquals("null", secContextInfo.getUserPrincipal());
        assertNull(secContextInfo.getAuthScheme(), secContextInfo.getAuthScheme());
    }
}
