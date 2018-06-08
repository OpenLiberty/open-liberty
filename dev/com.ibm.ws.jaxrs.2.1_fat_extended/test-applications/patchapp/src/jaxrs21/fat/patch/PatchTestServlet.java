/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.patch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PatchTestServlet")
public class PatchTestServlet extends FATServlet {

    private static final int HTTPS_PORT = Integer.getInteger("bvt.prop.HTTP_default.secure", 8020);

    private Client client;

    
    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder()
                              .property("com.ibm.ws.jaxrs.client.ssl.config", "defaultSSLConfig")
                              .build();
    }

    @Override
    public void destroy() {
        client.close();
    }

    @Test
    public void testPatchOptions(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String allowedHeaders = target(req, "patchapp/rest/test")
                        .request()
                        .options()
                        .getHeaderString("Allow");
        System.out.println("Allowed headers are: " + allowedHeaders);
        assertTrue(allowedHeaders, allowedHeaders.contains("PATCH"));
    }

    @Test
    public void testSubResourcePatchOptions(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        SubResource.resources.put(1, new SubResource("resource 1 data"));

        String allowedHeaders = target(req, "patchapp/rest/test/SubResource/1")
                        .request()
                        .options()
                        .getHeaderString("Allow");
        System.out.println("Allowed headers of subresource are: " + allowedHeaders);
        assertTrue(allowedHeaders, allowedHeaders.contains("PATCH"));
    }

    @Test
    public void testPatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String result = target(req, "patchapp/rest/test")
                        .request()
                        .method("PATCH", String.class);
        assertEquals("patch-success", result);
    }

    @Test
    public void testSubResourcePatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        SubResource.resources.put(2, new SubResource("resource 2 data"));

        String result = target(req, "patchapp/rest/test/SubResource/2")
                        .request()
                        .method("PATCH", String.class);
        assertEquals("PATCH: resource 2 data", result);
    }

    private WebTarget target(HttpServletRequest request, String path) {
        String base = "https://" + request.getServerName() + ':' + HTTPS_PORT + '/';
        return client.target(base + path);
    }

}