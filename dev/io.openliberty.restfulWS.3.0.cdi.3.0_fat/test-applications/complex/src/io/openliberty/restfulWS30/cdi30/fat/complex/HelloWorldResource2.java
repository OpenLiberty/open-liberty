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
package io.openliberty.restfulWS30.cdi30.fat.complex;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Path("/helloworld2")
public class HelloWorldResource2 {

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new {@code HelloWorldResource} object is created
     * per request.
     */
    @Inject
    private SimpleBean simpleBean;

    @Context
    private UriInfo uriinfo;

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