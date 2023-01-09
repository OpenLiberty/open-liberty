/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package openAPIapp;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/defaultEndpoints")
public class Endpoints {

    @GET
    @Path("/defaultEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "test default endpoint",
               description = "returns JSON object with key and value")
    public String getEndpoint() {
        return "{key: value}";
    }

    @GET
    @Path("/alternateEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "test alternate endpoint",
               description = "returns JSON object with key and alternate value")
    public String alternateEndpoint() {
        return "{key: alternatValue}";
    }

}