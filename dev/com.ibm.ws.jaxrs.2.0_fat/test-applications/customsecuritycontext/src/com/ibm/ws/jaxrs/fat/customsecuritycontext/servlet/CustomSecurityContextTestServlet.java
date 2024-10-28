/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.customsecuritycontext.servlet;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/CustomSecurityContextTestServlet")
public class CustomSecurityContextTestServlet extends FATServlet {

    private static final long serialVersionUID = 4563456788769868446L;

    private final String defaultEndpoint = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/SecurityContext/CustomSecurityContextResource/";
    private final String customEndpoint = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/CustomSecurityContext/CustomSecurityContextResource/";
    private final String defaultParamEndpoint = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/SecurityContext/CustomSecurityContextParamResource/";
    private final String customParamEndpoint = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/CustomSecurityContext/CustomSecurityContextParamResource/";


    @Test
    public void testDefaultSecurityContext() throws Exception {
        testDefaultSecurityContext(defaultEndpoint);
    }

    @Test
    public void testDefaultSecurityContextParam() throws Exception {
        testDefaultSecurityContext(defaultParamEndpoint);
    }

    private void testDefaultSecurityContext(String defaultUri) throws Exception {
        String uri = defaultUri + "Get";
        String token = "adam:password1";
        String basicAuthentication = "Basic " + Base64.getEncoder().encodeToString(token.getBytes("UTF-8"));

        System.out.println("uri=" + uri);
        Response response = null;
        Client client = ClientBuilder.newClient();
        WebTarget t = client.target(uri);
        response = t.request().header("Authorization", basicAuthentication).get();
        assertEquals(403, response.getStatus());

        token = "bob:password1";
        basicAuthentication = "Basic " + Base64.getEncoder().encodeToString(token.getBytes("UTF-8"));

        response = t.request().header("Authorization", basicAuthentication).get();
        assertEquals(200, response.getStatus());

        client.close();
    }

    @Test
    @SkipForRepeat("JAXRS-2.1") // Fails when using RequestScoped resource due to the Proxy not including WrapperIntf so it fails with a ClassCastException
    public void testCustomSecurityContextSetInFilter() throws Exception {
        testCustomSecurityContextSetInFilter(customEndpoint);
    }

    @Test
    public void testCustomSecurityContextSetInFilterParam() throws Exception {
        testCustomSecurityContextSetInFilter(customParamEndpoint);
    }

    private void testCustomSecurityContextSetInFilter(String customUri) throws Exception {
        String uri = customUri + "GetCustom";
        String token = "adam:password1";
        final String basicAuthentication = "Basic " + Base64.getEncoder().encodeToString(token.getBytes("UTF-8"));

        System.out.println("uri=" + uri);
        Response response = null;
        Client client = ClientBuilder.newClient();
        WebTarget t = client.target(uri);

        response = t.request().header("Authorization", basicAuthentication).get();
        assertEquals(200, response.getStatus());
        assertEquals("Bob", response.readEntity(String.class));

        client.close();
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