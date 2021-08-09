/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.linkheader;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Path("resource")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {

    @GET
    @Path("multipleheaders")
    public Response returnMultipleLinks() {
        Response response = Response.ok()
                        .links(Link.fromUri("http://test").rel("first").build(),
                               Link.fromUri("http://test").rel("next").build(),
                               Link.fromUri("http://test").rel("last").build())
                        .build();
        return response;

    }

    @GET
    @Path("singleheader")
    public Response returnMultipleLinksSingleHeader() {
        ResponseBuilder builder = Response.ok();
        builder.header("Link", "<http://test>;rel=\"first\","
                       + "<http://test>;rel=\"next\","
                       + "<http://test>;rel=\"last\"");
        return builder.build();
    }
}
