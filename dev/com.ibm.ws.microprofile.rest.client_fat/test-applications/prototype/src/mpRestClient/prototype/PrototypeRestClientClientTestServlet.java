/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package mpRestClient.prototype;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PrototypeRestClientClientTestServlet")
public class PrototypeRestClientClientTestServlet extends FATServlet {

    private static final String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/prototype/";

    private RestClientBuilder builder;

    @Override
    public void init() throws ServletException {
        URL baseUrl;
        try {
            baseUrl = new URL(URI_CONTEXT_ROOT);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrl);
    }

    @Test
    public void testHelloWorld() {
        PrototypeRestClient client = builder.build(PrototypeRestClient.class);
        String message = client.hello();
        assertEquals("Hello World!", message);
    }
}