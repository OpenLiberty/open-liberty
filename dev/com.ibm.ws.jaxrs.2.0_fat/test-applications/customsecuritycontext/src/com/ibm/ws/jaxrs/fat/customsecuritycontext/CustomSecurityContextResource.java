/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.customsecuritycontext;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.ibm.ws.jaxrs.fat.customsecuritycontext.filter.WrapperIntf;

@Path("CustomSecurityContextResource")
@RequestScoped
public class CustomSecurityContextResource {

    @Context
    SecurityContext securityContext;

    @GET
    @Path("Get")
    @RolesAllowed("admin")
    public Response requestSecurityInfo() {
        String name;
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            name = securityContext.getUserPrincipal().getName();
        } else {
            name = "null";
        }
        return Response.ok().entity(name).build();
    }

    @GET
    @Path("GetCustom")
    @RolesAllowed("admin")
    public Response requestCustomSecurityInfo() {
        ((WrapperIntf) securityContext).getThis();
        return requestSecurityInfo();
    }
}
