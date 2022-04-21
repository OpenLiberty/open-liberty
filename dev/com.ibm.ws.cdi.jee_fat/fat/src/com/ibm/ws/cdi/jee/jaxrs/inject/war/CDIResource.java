/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.jee.jaxrs.inject.war;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("cdiresource")
public class CDIResource {

    private HelloWorldBean helloWorldBean;

    public CDIResource() {}

    @Inject
    public CDIResource(HelloWorldBean helloWorldBean) {
        this.helloWorldBean = helloWorldBean;
    }

    @GET
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        System.out.println("GET!");
        System.out.println(helloWorldBean.message());
        return Response.ok().type(MediaType.TEXT_PLAIN).entity(helloWorldBean.message()).build();
    }

    @POST
    @Path("post")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post() {
        System.out.println("POST!");
        return Response.ok().build();
    }
}
