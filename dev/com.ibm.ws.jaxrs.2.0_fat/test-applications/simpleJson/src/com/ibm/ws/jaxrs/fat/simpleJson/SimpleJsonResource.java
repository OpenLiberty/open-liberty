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
package com.ibm.ws.jaxrs.fat.simpleJson;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("simpleresource")
public class SimpleJsonResource {

    @POST
    @Path("post")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String post(Car car) {
        return Integer.toString(car.year) + " " + car.make + " " + car.model;
    }

    @GET
    @Path("badresponse")
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnBadJson() {
        return Response.ok("{ \"foo\": }", MediaType.APPLICATION_JSON).build();
    }
}
