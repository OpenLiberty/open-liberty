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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

public abstract class AbstractLocalInterfaceEJBWithJAXRSInjectionResource {

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

    public Response getRequestResource() {
        final EntityTag etag = new EntityTag("myetagvalue");
        Response.ResponseBuilder respBuilder = getRequest().evaluatePreconditions(etag);
        if (respBuilder != null) {
            return respBuilder.build();
        }
        return Response.ok().entity("Hello").tag(etag).build();
    }

    public String getSecurityContextResource() {
        return "Is over https: " + getSecurityContext().isSecure();
    }

    public String getServletRequestResource() {
        return getServletRequest().getMethod();
    }

    public String getUriInfoResource() {
        return getUriInfo().getRequestUri().toASCIIString();
    }

    public String getHttpHeadersResource(@QueryParam("q") String headerName) {
        return getHttpHeaders().getRequestHeader(headerName).get(0);
    }

    public void getProvidersResource() {
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
