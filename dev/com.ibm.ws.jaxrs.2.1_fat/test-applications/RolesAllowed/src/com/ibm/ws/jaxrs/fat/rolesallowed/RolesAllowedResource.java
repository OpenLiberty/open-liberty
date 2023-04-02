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
package com.ibm.ws.jaxrs.fat.rolesallowed;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@RolesAllowed({"User", "Admin"})
@Path("RolesAllowedResource")
public class RolesAllowedResource {

    @GET
    public Response get() {
        return Response.ok().build();
    }

    @GET
    @RolesAllowed("Admin")
    @Path("admin")
    public Response admin() {
        return Response.ok().build();
    }
}
