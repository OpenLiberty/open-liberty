/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.restfulWS30.client.fat.pathparam;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("resource")
public class Resource {

    @GET
    @Path("bigdouble/{id}")
    public Response getID(@PathParam("id") double id) {
        System.out.println("getID double: " + Double.toString(id));
        return Response.ok("ok").build();
    }
    
    @GET
    @Path("biglong/{id}")
    public Response getID(@PathParam("id") long id) {
        System.out.println("getID long: " + Long.toString(id));
        return Response.ok("ok").build();
    }
    
    @GET
    @Path("smallshort/{id}")
    public Response getID(@PathParam("id") short id) {
        System.out.println("getID short: " + Short.toString(id));
        return Response.ok("ok").build();
    }
    @GET
    @Path("string/{id}")
    public Response getID(@PathParam("id") String id) {
        System.out.println("getID String: " + id);
        return Response.ok("ok").build();
    }
}
