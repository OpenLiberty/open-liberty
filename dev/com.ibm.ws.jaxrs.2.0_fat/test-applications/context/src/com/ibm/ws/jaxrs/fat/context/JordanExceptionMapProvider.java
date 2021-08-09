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
package com.ibm.ws.jaxrs.fat.context;

import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

@Provider
public class JordanExceptionMapProvider implements ExceptionMapper<JordanException> {

    private UriInfo uriinfo;

    @Context
    private HttpHeaders httpHeaders;

    @Context
    private Request request;

    @Context
    private Application app;

    private ResourceContext resourceContext;

    @Context
    private Providers providers;

    @Context
    private Configuration config;

    @Context
    private SecurityContext sc;

    @Context
    public void setUriInfo(UriInfo ui) {
        uriinfo = ui;
    }

    @Context
    public void setResourceContext(ResourceContext rc) {
        resourceContext = rc;
    }

    @Override
    public Response toResponse(JordanException arg0) {
        if (uriinfo == null || uriinfo.equals(null) || httpHeaders == null || httpHeaders.equals(null) || request == null || request.equals(null) || app == null
            || app.equals(null) || resourceContext == null || resourceContext.equals(null) || providers == null || providers.equals(null) || sc == null || sc.equals(null)) {
            return Response.status(545).build();
        }

        CommentError error = new CommentError();
        error.setErrorMessage(arg0.getMessage());
        return Response.status(454).entity(error).type("application/xml").build();
    }
}
