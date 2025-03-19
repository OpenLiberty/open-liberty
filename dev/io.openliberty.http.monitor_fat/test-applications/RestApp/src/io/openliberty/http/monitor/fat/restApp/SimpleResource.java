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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

/**
 * Simple Resource
 */
@ApplicationScoped
@Path("/simple")
public class SimpleResource {

    @GET
    @Path("/pathGet")
    public String normalPathGet() throws InterruptedException {
        return "Hello, do you GET it?";
    }

    @POST
    @Path("/pathPost")
    public String normalPathPost() throws InterruptedException {
        return "Hello, mister POST man!";
    }

    @PUT
    @Path("/pathPut")
    public String normalPathPut() throws Exception {
        return "Hello, PUT me in!";
    }

    @DELETE
    @Path("/pathDelete")
    public String normalPathDelete() throws Exception {
        return "Hello, don't DELETE me.";
    }

    @OPTIONS
    @Path("/pathOptions")
    public String normalPathOptions() throws Exception {
        return "Hello, give me my OPTIONS";
    }

    @HEAD
    @Path("/pathHead")
    public String normalPathHead() throws Exception {
        return "Hello, I've lost my HEAD!";
    }
}
