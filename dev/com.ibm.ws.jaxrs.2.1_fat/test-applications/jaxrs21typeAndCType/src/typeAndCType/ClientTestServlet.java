/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package typeAndCType;

import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/ClientTestServlet")
public class ClientTestServlet extends FATServlet {

    private static final long serialVersionUID = -8965492570925619992L;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/typeAndCType/";

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
    public void testAcceptHeaderOverride() throws Exception {
        Response r = client.target(URI_CONTEXT_ROOT)
                           .path("path/accept")
                           .queryParam("_type", MediaType.APPLICATION_ATOM_XML)
                           .request(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                           .get();
        if (Boolean.getBoolean("jaxrs.cxf.use.noop.requestPreprocessor")) { // use actual header values
            assertEquals(MediaType.APPLICATION_OCTET_STREAM, r.readEntity(String.class));
        } else { // use overridden header values from _type query param
            assertEquals(MediaType.APPLICATION_ATOM_XML, r.readEntity(String.class));
        }
    }

    @Test
    public void testContentTypeHeaderOverride() throws Exception {
        Response r = client.target(URI_CONTEXT_ROOT)
                           .path("path/contentType")
                           .queryParam("_ctype", MediaType.TEXT_PLAIN)
                           .request()
                           .post(Entity.xml("<foo>bar</foo>"));
        if (Boolean.getBoolean("jaxrs.cxf.use.noop.requestPreprocessor")) { // use actual header values
            assertEquals(MediaType.TEXT_PLAIN_TYPE, r.readEntity(String.class));
        } else { // use overridden header values from _type query param
            assertEquals(MediaType.APPLICATION_XML, r.readEntity(String.class));
        }
    }
}