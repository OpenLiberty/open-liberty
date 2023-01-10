/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package jaxrs21executors;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Future;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JAXRS21ExecutorsClientTestServlet")
public class JAXRS21ExecutorsClientTestServlet extends FATServlet {

    @Test
    public void testDefaultExecutorRetainsContext() throws Exception {
        Client c = ClientBuilder.newBuilder()
                                .register(MyRequestFilter.class)
                                .register(MyResponseFilter.class)
                                .build();
        WebTarget target = c.target("http://localhost:9999/notused");
        Future<Response> future = target.request().async().get();
        Response r = future.get();
        assertEquals("Unexpected status code - expected 200. Check logs for exceptionx.",
                     200, r.getStatus());
        assertEquals("Unexpected entity indicating request filter was unable to load JNDI resource",
                     "jaxrs21executors", r.readEntity(String.class));
        assertEquals("Unexpected/missing response indicating response filter not invoked or was unable to load JNDI resource",
                     "jaxrs21executors", r.getHeaderString("JNDI-Result"));
    }
}
