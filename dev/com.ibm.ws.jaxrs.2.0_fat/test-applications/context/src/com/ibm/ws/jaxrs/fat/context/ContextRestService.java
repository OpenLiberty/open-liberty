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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

@Path("/context")
public class ContextRestService {

    /**
     * This service is focus on UriInfo, HttpHeaders and Request test from Application
     */

    private final UriInfo uriInfo1;

    private final HttpHeaders httpheaders1;

    private final Request request1;

    private final Providers providers1;

    private final Configuration config1;

    private final ResourceContext resourceContext1;

    public ContextRestService(UriInfo ui, HttpHeaders hh, Request r, Providers p, Configuration c, ResourceContext rc) {
        this.uriInfo1 = ui;
        this.httpheaders1 = hh;
        this.request1 = r;
        this.providers1 = p;
        this.config1 = c;
        this.resourceContext1 = rc;
    }

    @GET
    @Path("/" + ContextUtil.URIINFONAME1)
    @Produces("text/plain")
    public String listQueryParamNames1() {
        return ContextUtil.URIINFONAME1 + ": " + ContextUtil.testUriInfo(uriInfo1);
    }

    @GET
    @Path("/" + ContextUtil.HTTPHEADERSNAME1)
    @Produces("text/plain")
    public String listHeaderNames1() {
        if (providers1 == null) {
            throw new RuntimeException();
        }
        return ContextUtil.HTTPHEADERSNAME1 + ": "
               + ContextUtil.findHttpHeadersValue(httpheaders1, ContextUtil.ADDHEADERNAME);
    }

    @GET
    @Path("/" + ContextUtil.REQUESTNAME1)
    @Produces("text/plain")
    public String listRequest1() {
        return ContextUtil.REQUESTNAME1 + ": " + request1.getMethod();
    }

    @GET
    @Path("/" + ContextUtil.CONFIGNAME1)
    @Produces("text/plain")
    public String listConfiguration1() {
        return ContextUtil.CONFIGNAME1 + ": " + config1.getRuntimeType().toString();
    }

    @Path("/" + ContextUtil.RESOURCECONTEXT1)
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book getBookSubResourceRC1() {
        return resourceContext1.getResource(Book.class);
    }
}