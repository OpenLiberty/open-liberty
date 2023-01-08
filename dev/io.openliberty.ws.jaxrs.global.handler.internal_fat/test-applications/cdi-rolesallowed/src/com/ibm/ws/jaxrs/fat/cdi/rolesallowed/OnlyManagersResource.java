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
