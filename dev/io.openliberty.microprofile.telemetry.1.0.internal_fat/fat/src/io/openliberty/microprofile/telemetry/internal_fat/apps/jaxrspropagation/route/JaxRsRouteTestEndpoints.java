/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.route;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Path("routeTestEndpoints")
public class JaxRsRouteTestEndpoints {

    @Path("/getWithId/{id}")
    @GET
    public String getWithId(@PathParam("id") String id) {
        return id;
    }

    @Path("/getWithQueryParam")
    @GET
    public String getWithQueryParam(@QueryParam("id") String id) {
        return id;
    }

    @Path("/getSubResourceWithPathParam/{id}")
    public JaxRsRouteTestSubResource getSubResourceWithPathParam(@PathParam("id") String id) {
        return new JaxRsRouteTestSubResource(id);
    }

    @Path("/getSubResourceWithQueryParam")
    public JaxRsRouteTestSubResource getSubResourceWithQueryParam(@QueryParam("id") String id) {
        return new JaxRsRouteTestSubResource(id);
    }

}
