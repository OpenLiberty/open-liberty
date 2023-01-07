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
package io.openliberty.restfulWS30.cdi30.fat.complex;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Path("/resourceprovider")
public class JordanResourceProvider implements ExceptionMapper<JordanException> {

    private UriInfo uriinfo;

    @Context
    public void setUriInfo(UriInfo ui) {
        uriinfo = ui;
    }

    @Inject
    SimpleBean simpleBean;

    @Override
    public Response toResponse(JordanException arg0) {
        System.out.println("ResourceProvider Context uriinfo: " + uriinfo.getPath());
        System.out.println("ResourceProvider Inject simplebean: " + simpleBean.getMessage());
        return Response.status(200).build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        String result = "Hello World";
        return "ResourceProvider: " + result;
    }

    @GET
    @Path("/uriinfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUriinfo() {
        String result = "";
        result = uriinfo == null ? "null uriinfo"
                        : uriinfo.getPath();
        return "ResourceProvider Context: " + result;
    }

    @GET
    @Path("/simplebean")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimpleBean() {
        String result = "";
        if (simpleBean != null)
            result = simpleBean.getMessage();
        else
            result = "simpleBean is null";

        return "ResourceProvider Inject: " + result;
    }
}
