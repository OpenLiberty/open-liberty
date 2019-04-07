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
package com.ibm.ws.jaxrs.fat.exceptionmappers.mapped;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapProvider implements ExceptionMapper<WebApplicationException> {

    @Context
    private UriInfo uri;

    @Override
    public Response toResponse(WebApplicationException arg0) {
        int oldStatus = arg0.getResponse().getStatus();
        Response.ResponseBuilder builder =
                        Response.fromResponse(arg0.getResponse()).header("ExceptionPage",
                                                                         uri.getAbsolutePath().toASCIIString());

        if (oldStatus == 499) {
            builder.status(497);
        } else if (oldStatus == Response.Status.BAD_REQUEST.getStatusCode()) {
            System.out.println("SETTING 496");
            builder.status(496);
        } else if (oldStatus == 481) {
            builder.status(491);
            CommentError error = new CommentError();
            error.setErrorMessage("WebApplicationExceptionMapProvider set message");
            builder.entity(error).type(MediaType.APPLICATION_XML_TYPE);
        }

        return builder.build();
    }

}
