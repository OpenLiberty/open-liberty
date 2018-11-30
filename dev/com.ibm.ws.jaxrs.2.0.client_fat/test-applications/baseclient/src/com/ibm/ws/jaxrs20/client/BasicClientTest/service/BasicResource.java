/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.BasicClientTest.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * basic resource to test jaxrs20 client API
 */
@Path("BasicResource")
public class BasicResource {

    private final String prefix = "[Basic Resource]:";

    @GET
    @Path("echo/{param}")
    public String echo(@PathParam("param") String param) {
        return prefix + param;
    }

    @GET
    @Path("defaultaccept")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHelloWorld() {
        return "Hello World";
    }

    @GET
    @Path("query")
    public String query(@QueryParam("param") String param) {
        System.out.println("param=" + param);
        if (param == null) {
            return "null";
        } else {
            return "param";
        }
    }
}
