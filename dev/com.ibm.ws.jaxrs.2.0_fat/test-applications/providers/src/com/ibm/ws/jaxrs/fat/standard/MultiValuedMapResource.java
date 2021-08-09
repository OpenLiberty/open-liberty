/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.standard;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

@Path("providers/standard/multivaluedmap")
public class MultiValuedMapResource {

    private MultivaluedMap<String, String> formData = null;

    @GET
    public Response getMultivaluedMap() {
        return Response.ok(formData).build();
    }

    @POST
    @Produces("application/x-www-form-urlencoded")
    public MultivaluedMap<String, String> postMultivaluedMap(MultivaluedMap<String, String> map) {
        return map;
    }

    @POST
    @Path("/noproduces")
    public MultivaluedMap<String, String> postMultivaluedMapNoProduces(MultivaluedMap<String, String> map) {
        return map;
    }

    @POST
    @Path("/subclasses/shouldfail")
    public MultivaluedMap<String, Object> postMultivaluedMapWithNotRightTypes(MultivaluedMap<String, Object> map) {
        return map;
    }

    @PUT
    public void putMultivaluedMap(MultivaluedMap<String, String> map) throws IOException {
        formData = map;
    }

    @POST
    @Produces("text/plain")
    @Path("/empty")
    public Response postEmptyMultivaluedMap(MultivaluedMap<String, String> map) {
        if (map != null && map.isEmpty()) {
            return Response.ok("expected").build();
        }
        return Response.serverError().build();
    }
}
