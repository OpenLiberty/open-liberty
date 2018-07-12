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
package web.jaxrstest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

@ApplicationPath("/testapp")
@Path("/test")
public class JAXRSExecutorTestApp extends Application {
    @GET
    @Path("/info")
    public Response info() {
        return Response.ok("test123").build();
    }

    @POST
    @Path("/post")
    public Response post() {
        return Response.ok("test456").build();
    }
}