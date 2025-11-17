/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mpRestClient.fat.multipart;

import java.io.IOException;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class MultipartResource {

    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFiles(List<EntityPart> parts) throws IOException {
        JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
        
        for (EntityPart part : parts) {
            JsonObjectBuilder jsonPartBuilder = Json.createObjectBuilder();
            jsonPartBuilder.add("name", part.getName());
            
            if (part.getFileName().isPresent()) {
                jsonPartBuilder.add("fileName", part.getFileName().get());
            }
            
            jsonPartBuilder.add("content", part.getContent(String.class));
            jsonBuilder.add(jsonPartBuilder);
        }
        
        return Response.status(201).entity(jsonBuilder.build()).build();
    }
}