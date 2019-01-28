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
package com.ibm.ws.jaxrs20.client.HandleResponsesTest.service;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

/**
 * basic resource to test jaxrs20 client API
 */
@ApplicationPath("/")
@Path("/resource")
public class Resource extends Application {

    @GET
    @Path("/202/empty")
    public Response empty202() {
        return Response.accepted().build();
    }

    @GET
    @Path("/202/echo/{param}")
    public Response echo202(@PathParam("param") String param) {
        return Response.accepted("202" + param).build();
    }
}
