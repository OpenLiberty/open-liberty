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
package com.ibm.ws.jaxrs.fat.security.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.junit.Test;

import com.ibm.ws.jaxrs.fat.securitycontext.xml.ObjectFactory;
import com.ibm.ws.jaxrs.fat.securitycontext.xml.SecurityContextInfo;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/SecurityContextTestServlet")
public class SecurityContextTestServlet extends FATServlet {

    private static final long serialVersionUID = 4563456788769868446L;

    // "/context/securitycontext" is common to all the Security*Resource @Path values,
    // differing only in the ending string after the last forward slash.
    // For this test, the url-pattern specified in web.xml is just /*
    // to simplify the URL, because otherwise it gets too crowded with duplicate strings.
    // e.g., http://localhost:8010/security/context/securitycontext/param is URL to
    // call GET, and SecurityContextParamResource is invoked.
    private String getSecurityContextTestUri() {
        return "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_secondary") + "/security/context/securitycontext";
    }

    /**
     * Tests that a security context can be injected via a parameter.
     *
     */
    @Test
    public void testSecurityContextParamResource() throws Exception {
        String uri = getSecurityContextTestUri() + "/param";

        Response response = null;
        Client client = ClientBuilder.newClient();
        WebTarget t = client.target(uri);
        response = t.request(MediaType.APPLICATION_XML).get();
        assertEquals(200, response.getStatus());
        client.close();

        JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        SecurityContextInfo secContextInfo = (SecurityContextInfo) context.createUnmarshaller().unmarshal(response.readEntity(InputStream.class));
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
    public void testSecurityContextConstructorResource() throws Exception, JAXBException {
        String uri = getSecurityContextTestUri() + "/constructor";
        Response response = null;
        Client client = ClientBuilder.newClient();
        WebTarget t = client.target(uri);
        response = t.request(MediaType.APPLICATION_XML).get();
        assertEquals(200, response.getStatus());
        client.close();

        JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        SecurityContextInfo secContextInfo = (SecurityContextInfo) context.createUnmarshaller().unmarshal(response.readEntity(InputStream.class));
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

        String uri = getSecurityContextTestUri() + "/bean";
        Response response = null;
        Client client = ClientBuilder.newClient();
        WebTarget t = client.target(uri);
        response = t.request(MediaType.APPLICATION_XML).get();
        assertEquals(200, response.getStatus());
        client.close();

        JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        SecurityContextInfo secContextInfo = (SecurityContextInfo) context.createUnmarshaller().unmarshal(response.readEntity(InputStream.class));
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

        String uri = getSecurityContextTestUri() + "/notbeanmethod";
        Response response = null;
        Client client = ClientBuilder.newClient();
        WebTarget t = client.target(uri);
        response = t.request(MediaType.APPLICATION_XML).get();
        assertEquals(200, response.getStatus());
        client.close();
    }

    /**
     * Tests that a security context can be injected via a member field.
     *
     */
    @Test
    public void testSecurityContextFieldResource() throws Exception {

        String uri = getSecurityContextTestUri() + "/field";
        Response response = null;
        Client client = ClientBuilder.newClient();
        WebTarget t = client.target(uri);
        response = t.request(MediaType.APPLICATION_XML).get();
        assertEquals(200, response.getStatus());
        client.close();

        JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        SecurityContextInfo secContextInfo = (SecurityContextInfo) context.createUnmarshaller().unmarshal(response.readEntity(InputStream.class));
        assertNotNull(secContextInfo);
        assertEquals(false, secContextInfo.isSecure());
        assertEquals(false, secContextInfo.isUserInRoleAdmin());
        assertEquals(false, secContextInfo.isUserInRoleNull());
        assertEquals(false, secContextInfo.isUserInRoleUser());
        assertEquals("null", secContextInfo.getUserPrincipal());
        assertNull(secContextInfo.getAuthScheme(), secContextInfo.getAuthScheme());
    }

    public static String asString(Response response) throws IOException {
        if (response.getEntity() == null) {
            return "";
        }

        final InputStream in = response.readEntity(InputStream.class);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

}