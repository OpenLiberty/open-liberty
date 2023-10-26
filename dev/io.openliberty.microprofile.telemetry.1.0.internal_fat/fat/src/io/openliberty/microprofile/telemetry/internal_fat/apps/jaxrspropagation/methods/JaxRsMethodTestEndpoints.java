/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.methods;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

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