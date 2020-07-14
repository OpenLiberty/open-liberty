/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.options;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/test")
public class OptionsResource {
    @POST
    public Response create() {
        return Response.ok().build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {
        return Response.ok(id).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id) {
        return Response.ok().build();
    }

    @GET
    @Path("three/{id1}/{id2}/{id3}")
    public Response three(@PathParam("id1") String id1,@PathParam("id2") String id2, @PathParam("id3") String id3) {
        return Response.ok(id1).build();
    }
}




