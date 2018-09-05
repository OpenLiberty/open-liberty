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
package jaxrs2x.cxfClientProps;

import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@ApplicationPath("/")
@Path("/resource")
public class Resource extends Application {
    private final static Logger _log = Logger.getLogger(Resource.class.getName());

    @Context
    private HttpHeaders httpHeaders;

    @GET
    @Path("/{sleepTime}")
    public Response sleep(@PathParam("sleepTime") @DefaultValue("30000") long sleepTime) {
        try {
            Thread.sleep(sleepTime);
            return Response.ok("Slept " + sleepTime + "ms").build();
        } catch (InterruptedException ex) {
            return Response.serverError().entity(ex).build();
        }
    }

    @GET
    @Path("/header")
    public String getHeader(@QueryParam("h") String headerName) {
        return httpHeaders.getHeaderString(headerName);
    }
    
    @POST
    @Path("/chunking")
    public String chunking(String entity) {
        String contentLength = httpHeaders.getHeaderString("Content-Length");
        int actualLength = entity.length();
        _log.info("chunking contentLength(from header)=" + contentLength + " actualLength=" + actualLength);
        return contentLength == null ? "CHUNKING" : contentLength + ":" + actualLength;
    }
}
