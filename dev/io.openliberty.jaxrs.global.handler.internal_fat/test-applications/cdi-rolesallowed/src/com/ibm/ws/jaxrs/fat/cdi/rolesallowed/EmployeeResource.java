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
package com.ibm.ws.jaxrs.fat.cdi.rolesallowed;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("employee/{id}")
@RequestScoped
@RolesAllowed("Employee")
public class EmployeeResource {

    private static ConcurrentHashMap<Integer, String> datastore =
                                                                    new ConcurrentHashMap<Integer, String>();

    @GET
    public String getEmployeeInfo(@PathParam("id") Integer id) {
        String name = datastore.get(id);
        if (name == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return name + ":" + id;
    }

    @POST
    public String postEmployeeInfo(@PathParam("id") int id, @Context SecurityContext secContext) {
        Principal p = secContext.getUserPrincipal();
        if (p == null) {
            throw new WebApplicationException();
        }
        datastore.put(id, p.getName());

        return p.getName() + ":" + id;
    }

    @PUT
    @RolesAllowed("HR")
    public String putEmployeeInfo(@PathParam("id") int id, String name) {
        datastore.put(id, name);

        return name + ":" + id;
    }

    @DELETE
    @RolesAllowed("HR")
    public void deleteEmployee(@PathParam("id") int id) {
        datastore.remove(id);
    }
}
