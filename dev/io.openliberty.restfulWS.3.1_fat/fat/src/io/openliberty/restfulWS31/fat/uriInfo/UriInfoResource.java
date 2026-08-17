/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.restfulWS31.fat.uriInfo;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Path("test")
public class UriInfoResource {

    @Context
    UriInfo uriInfo;

    @Context
    HttpHeaders httpHeaders;

    /**
     * Returns the raw value of UriInfo.getPath().
     * RESTEasy (restfulWS-3.1+): includes leading '/' e.g. "/test/getpath"
     * CXF (jaxrs-2.1): strips leading '/' e.g. "test/getpath"
     */
    @GET
    @Path("getpath")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPath() {
        return uriInfo.getPath();
    }

    /**
     * Returns "null" or "empty" based on what HttpHeaders.getRequestHeader()
     * returns for a header not present in the request.
     * RESTEasy (restfulWS-3.1+): returns empty List (never null)
     * CXF (jaxrs-2.1): returns null
     */
    @GET
    @Path("missingheader")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMissingHeader() {
        List<String> values = httpHeaders.getRequestHeader("X-Absent-Header");
        return (values == null) ? "null" : "empty";
    }
}
