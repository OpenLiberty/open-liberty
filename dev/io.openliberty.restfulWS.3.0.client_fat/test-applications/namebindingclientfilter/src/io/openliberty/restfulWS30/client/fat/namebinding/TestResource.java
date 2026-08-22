/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.restfulWS30.client.fat.namebinding;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Simple REST resource for testing client filters.
 * Echoes back request headers in the response so the client can verify if filters were applied.
 */
@Path("/resource")
public class TestResource {
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get(@Context HttpHeaders headers) {
        Response.ResponseBuilder builder = Response.ok("Test response");
        
        // Echo back the X-Annotated-Filter header if present
        String annotatedFilter = headers.getHeaderString("X-Annotated-Filter");
        if (annotatedFilter != null) {
            builder.header("X-Annotated-Filter", annotatedFilter);
        }
        
        // Echo back the X-Non-Annotated-Filter header if present
        String nonAnnotatedFilter = headers.getHeaderString("X-Non-Annotated-Filter");
        if (nonAnnotatedFilter != null) {
            builder.header("X-Non-Annotated-Filter", nonAnnotatedFilter);
        }
        
        return builder.build();
    }
}