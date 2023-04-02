/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package test.service.scope;

import javax.annotation.PreDestroy;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/resource")
public class Resource {
    @GET
    @Path("/initial")
    public Response initial() {
        Response r = Response.ok("SUCCESS").header("Test-Initial", "true").build();
        App.mostRecent = Stage.RESOURCE_METHOD_EXIT;
        return r;
    }

    @GET
    @Path("/verify")
    public Response verify() {
        Response r;
        if (Stage.PREDESTROY_METHOD_EXIT.equals(App.mostRecent)) {
            r = Response.ok("SUCCESS").build();
        } else {
            r = Response.status(555).entity("Unexpected stage: " + App.mostRecent).build();
        }
        return r;
    }

    @GET
    @Path("/reset")
    public Response reset() {
        App.mostRecent = null;
        return Response.ok("Reset Successful").build();
    }

    @PreDestroy
    public void preDestroy() {
        App.mostRecent = Stage.PREDESTROY_METHOD_EXIT;
    }
}
