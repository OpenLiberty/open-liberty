/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.restfulWS40.fat.rest40examples;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("/api/users")
public class UserResource {

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("/{userId}/orders/{orderId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserOrder(
            @PathParam("userId") Long userId,
            @PathParam("orderId") Long orderId) {

        // NEW in REST 4.0: getMatchedResourceTemplate method
        String template = uriInfo.getMatchedResourceTemplate();
        System.out.println("Matched template: " + template);
        // Output: /api/users/{userId}/orders/{orderId}

        // Use the template for logging, metrics, or routing decisions
        return Response.ok()
            .entity(new Order(orderId, userId))
            .header("X-Resource-Template", template)
            .build();
    }

    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("userId") Long userId) {
        String template = uriInfo.getMatchedResourceTemplate();
        return Response.ok()
            .entity("{\"userId\":" + userId + ",\"template\":\"" + template + "\"}")
            .header("X-Resource-Template", template)
            .build();
    }
}

// Made with Bob
