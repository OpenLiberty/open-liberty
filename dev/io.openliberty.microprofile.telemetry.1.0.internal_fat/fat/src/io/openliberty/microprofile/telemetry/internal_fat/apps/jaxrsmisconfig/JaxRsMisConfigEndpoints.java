/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrsmisconfig;

import static org.junit.Assert.assertNotNull;

import io.opentelemetry.api.trace.Span;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@ApplicationPath("")
@Path("misconfig")
public class JaxRsMisConfigEndpoints extends Application {

    @GET
    @Path("/jaxrsclient")
    public Response getJax(@Context UriInfo uriInfo) {
        assertNotNull(Span.current());
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Client client = ClientBuilder.newClient();
        String url = new String(uriInfo.getAbsolutePath().toString());
        url = url.replace("jaxrsclient", "jaxrstwo"); //The jaxrsclient will use the URL as given so it needs the final part to be provided.

        String result = client.target(url)
                        .request(MediaType.TEXT_PLAIN)
                        .get(String.class);

        client.close();

        return Response.ok(result).build();
    }

    //A method to be called by JAX Clients
    //This method triggers span creation
    @GET
    @Path("/jaxrstwo")
    public Response getJaxRsTwo() {
        assertNotNull(Span.current());
        return Response.ok("Test Passed").build();
    }
}
