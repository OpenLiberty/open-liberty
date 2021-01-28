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

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("managersonly")
@RolesAllowed("Manager")
@ApplicationScoped
public class OnlyManagersResource {

    /*
     * effectively a singleton with application scoped
     */
    private volatile String managerData;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return managerData;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String post(String data) {
        this.managerData = data;
        return data;
    }

    @DELETE
    public void delete(String data) {
        this.managerData = null;
    }
}
