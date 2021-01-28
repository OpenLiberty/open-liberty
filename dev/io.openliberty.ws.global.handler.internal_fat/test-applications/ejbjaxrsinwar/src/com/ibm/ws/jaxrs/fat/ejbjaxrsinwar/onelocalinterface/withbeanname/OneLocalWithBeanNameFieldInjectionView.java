/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("oneLocalWithBeanNameFieldInjectionView")
public interface OneLocalWithBeanNameFieldInjectionView {

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
