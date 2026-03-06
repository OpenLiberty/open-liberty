/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package multiple.ham.common;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Protected REST API resource serving /resource/test
 */
@Path("/resource")
public class MultipleHAMProtectedResource {

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public String getProtectedResource(@Context SecurityContext securityContext) {
        return "Success";
    }
}
