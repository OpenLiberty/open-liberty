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
package com.ibm.ws.jaxrs.fat.params.newcookie;

import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

@Path("cookiestests")
public class CookieResource {

    @Context
    private UriInfo uri;

    private String value3;

    @GET
    @Produces("text/plain")
    @Path("getAll")
    public Response getCookie(@Context HttpHeaders headers) {
        Map<String, Cookie> cookies = headers.getCookies();
        String ret = "";
        if (cookies != null) {
            for (String s : cookies.keySet()) {
                Cookie c = cookies.get(s);
                ret +=
                                c.getName() + ","
                                                + c.getValue()
                                                + ","
                                                + c.getPath()
                                                + ","
                                                + c.getDomain().toLowerCase()
                                                + "\r";
            }
        }
        return Response.ok(ret).build();
    }

    @GET
    @Produces("text/plain")
    @Path("getValue2")
    public Response getValue2() {
        return Response.status(Status.BAD_REQUEST).entity("value2").build();
        // return
        // Response.status(Status.BAD_REQUEST).entity(this.value2).build();
        // return Response.ok(this.value2).build();
    }

    @GET
    @Produces("text/plain")
    @Path("getStaticValue")
    public Response getStaticValue() {
        return null;
        // return Response.ok(value).build();
    }

    @GET
    @Produces("text/plain")
    @Path("getValue3")
    public Response getValue3() {
        return Response.status(Status.BAD_REQUEST).entity(this.value3).build();
        // return Response.ok(this.value3).build();
    }

    @PUT
    @Produces("text/plain")
    public Response setCookies() {
        ResponseBuilder rb = Response.ok();
        rb.cookie(new NewCookie("name", "value", uri.getBaseUri().getPath() + uri.getPath(), uri
                        .getBaseUri().getHost().toLowerCase(), "comment", 10, false));
        rb.cookie(new NewCookie("name2", "value2", uri.getBaseUri().getPath() + uri.getPath(), uri
                        .getBaseUri().getHost().toLowerCase(), "comment2", 10, false));
        rb.cookie(new NewCookie("name3", "value3", uri.getBaseUri().getPath() + uri.getPath(), uri
                        .getBaseUri().getHost().toLowerCase(), "comment2", 10, false));
        return rb.build();
    }

    @CookieParam("name3")
    public void setValue3(String value3) {
        this.value3 = value3;
    }
}
