/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.ibm.ws.jaxrs.fat.cdi.rolesallowed;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;

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
