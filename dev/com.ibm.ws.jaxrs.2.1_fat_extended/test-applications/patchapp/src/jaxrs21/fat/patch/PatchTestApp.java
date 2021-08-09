/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.patch;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

@ApplicationScoped
@ApplicationPath("/rest")
@Path("/test")
public class PatchTestApp extends Application {

    @GET
    public Response get() {
        return Response.ok("heartbeat").build();
    }

    @PATCH
    public Response patch() {
        System.out.println("inside PatchTestApp.patch()");
        return Response.ok("patch-success").build();
    }

    @Path("/SubResource/{id}")
    public SubResource page(@PathParam("id") int pageNum) {
        return SubResource.lookup(pageNum);
    }
}