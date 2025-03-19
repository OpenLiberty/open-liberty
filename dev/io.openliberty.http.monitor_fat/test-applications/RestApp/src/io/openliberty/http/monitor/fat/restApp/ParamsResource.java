/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat.restApp;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Params Resource
 */
@ApplicationScoped
@Path("/params")
public class ParamsResource {

    @GET
    @Path("/{anything}")
    public String anything(@PathParam("anything") String anything) throws InterruptedException {
        return "Hello, you can say " + anything;
    }

    @GET
    @Path("/name/{name}")
    public String getNameParam(@PathParam("name") String name) throws InterruptedException {
        return "Hello, person named " + name;
    }

    @POST
    @Path("/name/{name}")
    public String postNameParam(@PathParam("name") String name) throws Exception {
        return "Hello, PUT me in! My name is: " + name;
    }

    @GET
    @Path("/query")
    public String queryParam(@QueryParam("useless") String useless) throws InterruptedException {
        return "Hello, queryParam!";
    }

    @GET
    @Path("/pq/{pq}")
    public String pathQueryParam(@PathParam("pq") String pq, @QueryParam("useless") String useless) throws InterruptedException {
        return "Hello, pathQueryParam!. Retrieved path: " + pq + " and query : " + useless;
    }

}
