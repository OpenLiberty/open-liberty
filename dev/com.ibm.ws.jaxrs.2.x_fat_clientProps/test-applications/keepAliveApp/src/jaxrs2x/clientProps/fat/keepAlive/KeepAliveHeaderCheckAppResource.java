/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs2x.clientProps.fat.keepAlive;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@ApplicationPath("/rest")
@Path("/test")
@Produces(MediaType.TEXT_PLAIN)
public class KeepAliveHeaderCheckAppResource extends Application {

    private static Response fail(String msg) {
        return Response.serverError().entity(msg).build();
    }

    @PostConstruct
    public void init() {
        System.out.println("KeepAliveHeaderCheckAppResource - started");
    }

    @GET
    public Response checkForConnectionHeader(@Context HttpHeaders headers) {
        String connHeader = headers.getHeaderString("Connection");
        String expectedConnHeader = headers.getHeaderString("Expect-Connection");
        if (expectedConnHeader == null) {
            return fail("expected value is null - unable to check");
        } else if (connHeader == null) {
            return fail("Connection header value is not set.");
        } else if (!expectedConnHeader.equals(connHeader)) {
            return fail("Connection header value, " + connHeader + " is not what was expected, " + expectedConnHeader);
        }
        return Response.ok("success").build();
    }

}
