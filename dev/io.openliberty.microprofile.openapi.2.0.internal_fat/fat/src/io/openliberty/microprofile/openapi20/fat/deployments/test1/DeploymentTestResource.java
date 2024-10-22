/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.deployments.test1;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/")
public class DeploymentTestResource {

    @GET
    @Path("/test")
    @Operation(summary = "test method")
    @APIResponse(responseCode = "200", description = "constant \"OK\" response")
    @Produces(value = MediaType.TEXT_PLAIN)
    public String test() {
        return "OK";
    }

    @GET
    @Path("/log")
    @Operation(summary = "log method", hidden = true)
    @APIResponse(responseCode = "200", description = "logs the queryparam message")
    @Produces(value = MediaType.TEXT_PLAIN)
    public String log(@QueryParam("message") String message) {
        System.out.println(message);
        return "OK";
    }

}
