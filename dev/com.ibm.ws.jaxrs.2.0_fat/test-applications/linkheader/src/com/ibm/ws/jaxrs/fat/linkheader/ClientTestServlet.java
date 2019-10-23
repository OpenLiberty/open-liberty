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
package com.ibm.ws.jaxrs.fat.linkheader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/ClientTestServlet")
public class ClientTestServlet extends FATServlet {

    private static final long serialVersionUID = 4563445389586844836L;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/linkheader/";

    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @After
    private void teardown() {
        client.close();
    }

    @Test
    public void testResponseMultipleLinkHeaders() throws Exception {

        Response response = client.target(URI_CONTEXT_ROOT).path("application/resource/multipleheaders").request(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(200, response.getStatus());

        System.out.println("Headers:");
        System.out.println(response.getHeaders());

        Set<Link> links = response.getLinks();
        assertEquals(3, links.size());

        System.out.println("Links:");
        for (Link link : links) {
            System.out.println(link);
            if (link.toString().contains("first")) {
                assertEquals("<http://test>;rel=\"first\"", link.toString());
            } else if (link.toString().contains("next")) {
                assertEquals("<http://test>;rel=\"next\"", link.toString());
            } else if (link.toString().contains("last")) {
                assertEquals("<http://test>;rel=\"last\"", link.toString());
            } else {
                fail("invalid link returned");
            }
        }

        assertEquals("<http://test>;rel=\"first\"", response.getLink("first").toString());
        assertEquals("<http://test>;rel=\"next\"", response.getLink("next").toString());
        assertEquals("<http://test>;rel=\"last\"", response.getLink("last").toString());
    }

    @Test
    public void testResponseSingleLinkHeaderMultipleLinks() throws Exception {

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/singleheader")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();

        assertEquals(200, response.getStatus());

        System.out.println("Headers:");
        System.out.println(response.getHeaders());

        Set<Link> links = response.getLinks();
        assertEquals(3, links.size());

        System.out.println("Links:");
        for (Link link : links) {
            System.out.println(link);
            if (link.toString().contains("first")) {
                assertEquals("<http://test>;rel=\"first\"", link.toString());
            } else if (link.toString().contains("next")) {
                assertEquals("<http://test>;rel=\"next\"", link.toString());
            } else if (link.toString().contains("last")) {
                assertEquals("<http://test>;rel=\"last\"", link.toString());
            } else {
                fail("invalid link returned");
            }
        }

        assertEquals("<http://test>;rel=\"first\"", response.getLink("first").toString());
        assertEquals("<http://test>;rel=\"next\"", response.getLink("next").toString());
        assertEquals("<http://test>;rel=\"last\"", response.getLink("last").toString());
    }
}