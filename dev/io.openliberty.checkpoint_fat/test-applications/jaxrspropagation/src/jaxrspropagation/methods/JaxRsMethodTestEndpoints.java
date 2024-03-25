/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jaxrspropagation.methods;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

@Path("methodTestEndpoints")
public class JaxRsMethodTestEndpoints {

    @GET
    public String get() {
        new Exception("Test stack").printStackTrace();
        return "get";
    }

    @HEAD
    public void head() {
    }

    @POST
    public String post(String string) {
        return "post";
    }

    @PUT
    public String put(String string) {
        return "put";
    }

    @DELETE
    public String delete() {
        return "delete";
    }

    @PATCH
    public String patch(String string) {
        return "patch";
    }

    @OPTIONS
    public Response options() {
        return Response.ok("options")
                        .header(HttpHeaders.ALLOW, "GET")
                        .header(HttpHeaders.ALLOW, "HEAD")
                        .header(HttpHeaders.ALLOW, "POST")
                        .header(HttpHeaders.ALLOW, "PUT")
                        .header(HttpHeaders.ALLOW, "DELETE")
                        .header(HttpHeaders.ALLOW, "PATCH")
                        .header(HttpHeaders.ALLOW, "OPTIONS")
                        .build();
    }
}
