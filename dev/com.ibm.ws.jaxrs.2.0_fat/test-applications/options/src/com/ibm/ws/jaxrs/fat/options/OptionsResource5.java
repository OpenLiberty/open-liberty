/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.options;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/test5")
public class OptionsResource5 {

    public static class SubResource {
        private String subResourceString = " ";

        public SubResource(String myString) {
            this.subResourceString = myString;
        }

        private static SubResource lookup(String myString) {
            SubResource mySubResource = new SubResource(myString);
            return mySubResource;
        }

        @GET
        public Response get() {
            return Response.ok().build();
        }

    }

    @Path("/{id}")
    public SubResource subResource(@PathParam("id") String id) {
        SubResource mySubResource = SubResource.lookup("subResource");
        return mySubResource;
    }

    @PUT
    @Path("/{id}")
    public Response put(@PathParam("id") String id) {
        return Response.ok().build();
    }
}
