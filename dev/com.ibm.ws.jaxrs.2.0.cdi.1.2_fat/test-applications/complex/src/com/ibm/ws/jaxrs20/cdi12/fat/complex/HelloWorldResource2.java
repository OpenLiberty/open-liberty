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
package com.ibm.ws.jaxrs20.cdi12.fat.complex;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path("/helloworld2")
public class HelloWorldResource2 {

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new <code>HelloWorldResource</code> object is created
     * per request.
     */

    private @Inject
    SimpleBean simpleBean;

    @Context
    private UriInfo uriinfo;

    // private @Inject
    Person person;

    @Inject
    public void setPerson(Person person) {
        this.person = person;
        System.out.println("Resource2 Injection successful...");
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        String result = "Hello World";
        return "Resource2 Resource: " + result;
    }

    @GET
    @Path("/uriinfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUriinfo() {
        String result = "";
        result = uriinfo == null ? "null uriinfo"
                        : uriinfo.getPath();
        return "Resource2 Resource Context: " + result;
    }

    @GET
    @Path("/simplebean")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimpleBean() {
        String result = "";
        if (simpleBean != null)
            result = simpleBean.getMessage();
        else
            result = "simpleBean is null";

        return "Resource2 Resource Inject: " + result;
    }

    @GET
    @Path("/person")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPerson() {
        String result = "";

        if (person != null)
            result = person.talk();
        else
            result = "person is null";

        return "Resource2 Resource Inject: " + result;
    }

}