/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.responses;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 *
 */
@Path("responseCodeEndpoints")
public class JaxRsResponseCodeTestEndpoints {

    @Path("200")
    @GET
    public Response get200() {
        return Response.ok("get200").build();
    }

    @Path("202")
    @GET
    public Response get202() {
        return Response.accepted("get202").build();
    }

    @Path("400")
    @GET
    public Response get400() {
        return Response.status(BAD_REQUEST)
                        .entity("get400")
                        .build();
    }

    @Path("404")
    @GET
    public Response get404() {
        return Response.status(Status.NOT_FOUND)
                        .entity("get404")
                        .build();
    }

    @Path("500")
    @GET
    public Response get500() {
        return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("get500")
                        .build();
    }

}
