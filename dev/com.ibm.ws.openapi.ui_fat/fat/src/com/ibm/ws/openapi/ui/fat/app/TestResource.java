/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.openapi.ui.fat.app;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

/**
 * Very basic application so that we have something to look at in the OpenAPI UI
 */
@OpenAPIDefinition(info = @Info(title = "UI Test App", version = "0.1"))
@ApplicationPath("/")
@Path("/test")
public class TestResource {

    @Path("/{id}")
    @GET
    public int testGet(@PathParam("id") int id) {
        return id;
    }

    @Path("/{id}")
    @PUT
    public void testPut(@PathParam("id") int id) {
        // store a message
    }

    @Path("/{id}")
    @POST
    public void testPost(@PathParam("id") int id) {
        // store a message
    }

    @Path("/{id}")
    @DELETE
    public void testDelete(@PathParam("id") int id) {
        // delete a message
    }
}
