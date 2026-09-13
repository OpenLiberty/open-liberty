/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.restfulWS31.fat.uriInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.annotation.WebServlet;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;


@WebServlet(urlPatterns = "/UriInfoServlet")
public class UriInfoServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    static final String URI_CONTEXT_ROOT = "http://localhost:"
            + Integer.getInteger("bvt.prop.HTTP_default") + "/uriInfo/";

    private Client client;

    @Override
    public void init() throws jakarta.servlet.ServletException {
        client = ClientBuilder.newClient();
    }

    @After
    private void teardown() {
        client.close();
    }

    /**
     * returns UriInfo.getPath() WITH a leading '/'.
     */
    @Test
    public void testGetPathHasLeadingSlash() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                                  .path("resources/test/getpath")
                                  .request()
                                  .get();
        assertEquals(200, response.getStatus());
        String path = response.readEntity(String.class);
        assertTrue("Expected path to start with '/' but was: " + path, path.startsWith("/"));
        assertEquals("/test/getpath", path);
    }

    /**
     * returns an empty (non-null) List for an absent request header.
     */
    @Test
    public void testMissingHeaderReturnsEmpty() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                                  .path("resources/test/missingheader")
                                  .request()
                                  .get();
        assertEquals(200, response.getStatus());
        assertEquals("empty", response.readEntity(String.class));
    }
}
