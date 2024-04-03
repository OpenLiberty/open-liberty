/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee11.internal.apps.jakartaee11.web.jaxrs;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

@ApplicationPath("/rest")
@Path("/test")
public class PatchTestApp extends Application {

    @GET
    public Response get() {
        return Response.ok("heartbeat").build();
    }

    @PATCH
    public Response patch() {
        System.out.println("inside PatchTestApp.patch()");
        return Response.ok("patch-success").build();
    }

}
