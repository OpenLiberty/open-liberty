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
package io.openliberty.microprofile.openapi20.fat.deployments;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/")
public class DeploymentTestResourceTest1 {

    @GET
    @Path("/test1/test")
    @Operation(summary = "test method")
    @APIResponse(responseCode = "200", description = "constant \"OK\" response")
    @Produces(value = MediaType.TEXT_PLAIN)
    public String test() {
        return "OK";
    }

}
