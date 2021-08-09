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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

public abstract class AbstractEJBWithJAXRSInjectionResource {

    abstract protected UriInfo getUriInfo();

    abstract protected Request getRequest();

    abstract protected HttpHeaders getHttpHeaders();

    abstract protected Providers getProviders();

    abstract protected SecurityContext getSecurityContext();

    abstract protected HttpServletRequest getServletRequest();

    abstract protected HttpServletResponse getServletResponse();

    abstract protected ServletConfig getServletConfig();

    abstract protected ServletContext getServletContext();

    abstract protected Application getApplication();

    @GET
    @Path("request")
    public Response getRequestResource() {
        final EntityTag etag = new EntityTag("myetagvalue");
        Response.ResponseBuilder respBuilder = getRequest().evaluatePreconditions(etag);
        if (respBuilder != null) {
            return respBuilder.build();
        }
        return Response.ok().entity("Hello").tag(etag).build();
    }

    @GET
    @Path("securitycontext")
    public String getSecurityContextResource() {
        return "Is over https: " + getSecurityContext().isSecure();
    }

    @GET
    @Path("servletRequestField")
    public String getServletRequestResource() {
        return getServletRequest().getMethod();
    }

    @GET
    @Path("uriinfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String getRequestURIResource() {
        return getUriInfo().getRequestUri().toASCIIString();
    }

    @GET
    @Path("httpheaders")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHttpHeaderResource(@QueryParam("q") String headerName) {
        return getHttpHeaders().getRequestHeader(headerName).get(0);
    }

    @GET
    @Path("providers")
    @Produces(MediaType.TEXT_PLAIN)
    public void getProvidersResponseResource() {
        MessageBodyWriter<String> stringProvider =
                        getProviders().getMessageBodyWriter(String.class,
                                                            String.class,
                                                            null,
                                                            MediaType.TEXT_PLAIN_TYPE);
        try {
            stringProvider.writeTo("Hello World!",
                                   String.class,
                                   String.class,
                                   new Annotation[0],
                                   MediaType.TEXT_PLAIN_TYPE,
                                   null,
                                   getServletResponse().getOutputStream());
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("application")
    @Produces(MediaType.TEXT_PLAIN)
    public String getApplicationClasses() {
        String ret = "";
        Set<Class<?>> classes = getApplication().getClasses();
        // all this to make sure the Set is returned in the right order
        Class<?>[] classArray = classes.toArray(new Class[] {});
        Arrays.sort(classArray, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> object1, Class<?> object2) {
                if (object1 == null) {
                    if (object2 == null) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    if (object2 == null) {
                        return 1;
                    } else {
                        return object1.getName().compareTo(object2.getName());
                    }
                }
            }
        });
        for (int i = 0; i < classArray.length; ++i)
            ret += (classArray[i].getName() + ";");
        return getApplication().getClass().getName() + ";" + ret;
    }
}
