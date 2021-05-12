/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.singleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("oneLocalInterfaceSingletonFieldInjectionView")
public interface OneLocalInterfaceSingletonFieldInjectionView {

    @GET
    @Path("request")
    public Response getRequestResource();

    @GET
    @Path("securitycontext")
    public String getSecurityContextResource();

    @GET
    @Path("servletRequestField")
    public String getServletRequestResource();

    @GET
    @Path("uriinfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUriInfoResource();

    @GET
    @Path("httpheaders")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHttpHeadersResource(@QueryParam("q") String headerName);

    @GET
    @Path("providers")
    @Produces(MediaType.TEXT_PLAIN)
    public void getProvidersResource();

    @GET
    @Path("application")
    @Produces(MediaType.TEXT_PLAIN)
    public String getApplicationClasses();
}
