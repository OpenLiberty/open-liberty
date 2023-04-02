/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.linkheader;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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
        test("application/resource/multipleheaders");
    }

    @Test
    public void testResponseSingleLinkHeaderMultipleLinks() throws Exception {
        test("application/resource/singleheader");
    }

    private void test(String path) throws Exception {

        Response response = client.target(URI_CONTEXT_ROOT).path(path).request(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(200, response.getStatus());

        System.out.println("Headers:");
        System.out.println(response.getHeaders());

        Set<Link> links = response.getLinks();
        assertEquals(3, links.size());

        System.out.println("Links:");
        for (Link link : links) {
            System.out.println(link);
            if (link.toString().contains("first")) {
                assertThat(link.toString(), allOf(startsWith("<http://test>;"),
                                                  endsWith("rel=\"first\"")));
            } else if (link.toString().contains("next")) {
                assertThat(link.toString(), allOf(startsWith("<http://test>;"),
                                                  endsWith("rel=\"next\"")));
            } else if (link.toString().contains("last")) {
                assertThat(link.toString(), allOf(startsWith("<http://test>;"),
                                                  endsWith("rel=\"last\"")));
            } else {
                fail("invalid link returned");
            }
        }

        assertThat(response.getLink("first").toString(), allOf(startsWith("<http://test>;"),
                                                               endsWith("rel=\"first\"")));
        assertThat(response.getLink("next").toString(), allOf(startsWith("<http://test>;"),
                                                               endsWith("rel=\"next\"")));
        assertThat(response.getLink("last").toString(), allOf(startsWith("<http://test>;"),
                                                               endsWith("rel=\"last\"")));
    }
}