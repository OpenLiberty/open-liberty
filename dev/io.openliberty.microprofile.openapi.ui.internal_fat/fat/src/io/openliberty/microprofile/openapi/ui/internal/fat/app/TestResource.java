/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi.ui.internal.fat.app;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * Very basic application so that we have something to look at in the OpenAPI UI
 */

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
