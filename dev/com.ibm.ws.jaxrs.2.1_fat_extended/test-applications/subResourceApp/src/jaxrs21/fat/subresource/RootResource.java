/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.subresource;

import java.util.HashMap;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Path("/root")
@Produces(MediaType.APPLICATION_JSON)
public class RootResource {
    @Context ResourceContext rc;

    @Path("sub")
    public SubResource getSubResource() {
        return rc.initResource(new SubResource());
    }

    @GET
    @Path("id/{id}")
    public HashMap<String, String> getId(@PathParam("id") String id) {
        HashMap<String, String> map = new HashMap<>();
        map.put("root", id);
        return map;
    }
}
