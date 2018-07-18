/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch;

import java.io.Serializable;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch.simpleresource.SimpleBean;

/**
 * <code>HelloWorldResource</code> is a simple POJO which is annotated with
 * JAX-RS annotations to turn it into a JAX-RS resource.
 * <p/>
 * This class has a {@link Path} annotation with the value "helloworld" which
 * means the resource will be available at:
 * <code>http://&lt;hostname&gt;:&lt;port&gt/&lt;context root&gt;/&lt;servlet path&gt;/helloworld</code>
 * <p/>
 * Remember to add this resource class to the {@link HelloWorldApplication#getClasses()} method.
 */
@Path("/ProviderResource")
public class ProviderResource implements Serializable {

    private static final long serialVersionUID = -4683480692069675503L;

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new <code>HelloWorldResource</code> object is created
     * per request.
     */
    private static volatile String message = "Hello World for jaxrs-1.1!";

    private UriInfo uiFromConstructor;

    private @Inject
    SimpleBean simpleBean;

    @Context
    private UriInfo uriinfo;

    int counter = 0;

    /**
     * Processes a GET request and returns the stored message.
     *
     * @return the stored message
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        // Note that if null is returned from a resource method, a HTTP 204 (No
        // Content) status code response is sent.
        return getTest() + " counter: " + counter++;
    }

    public String getTest() {
        String s = "inject test start...";
        if (simpleBean == null) {
            s = s + "injected is null...FAILED";
        } else {
            s = s + "injected is NOT null...";
            try {
                String s2 = simpleBean.getMessage();
                if (s2 != null) {
                    s = s + "injected.getMessage returned..." + s2;
                } else {
                    s = s + "injected.getMessage returned null...FAILED";
                }
            } catch (Exception e) {
                s = s + "caught exception: " + e + "...FAILED";
            }
        }

        return s;
    }

    @GET
    @Path("ApplicationScopedProvider/{id}")
    public String getJordanException(@PathParam("id") String msgId)
                    throws ApplicationScopedException {
        String name = "jordan";
        String url = "null uriinfo";
        if (msgId.trim() == name || msgId.trim().equals(name)) {
            if (uriinfo != null) {
                url = uriinfo.getPath();
            }
            throw new ApplicationScopedException("JordanException: Jordan is superman, you cannot be in this url: " + url);
        }

        return "No execute JordanExceptionMapProvider, this url: " + url;
    }

    @GET
    @Path("DefaultProvider/{id}")
    public String getDefaultException(@PathParam("id") String msgId)
                    throws DefaultException {
        String name = "jordan";
        String url = "null uriinfo";
        if (msgId.trim() == name || msgId.trim().equals(name)) {
            if (uriinfo != null) {
                url = uriinfo.getPath();
            }
            throw new DefaultException("JordanException: Jordan is superman, you cannot be in this url: " + url);
        }

        return "No execute JordanExceptionMapProvider, this url: " + url;
    }

    @GET
    @Path("DependentProvider/{id}")
    public String getDependentException(@PathParam("id") String msgId)
                    throws DependentException {
        String name = "jordan";
        String url = "null uriinfo";
        if (msgId.trim() == name || msgId.trim().equals(name)) {
            if (uriinfo != null) {
                url = uriinfo.getPath();
            }
            throw new DependentException("JordanException: Jordan is superman, you cannot be in this url: " + url);
        }

        return "No execute JordanExceptionMapProvider, this url: " + url;
    }

    @GET
    @Path("RequestScopedProvider/{id}")
    public String getRequestScopedException(@PathParam("id") String msgId)
                    throws RequestScopedException {
        String name = "jordan";
        String url = "null uriinfo";
        if (msgId.trim() == name || msgId.trim().equals(name)) {
            if (uriinfo != null) {
                url = uriinfo.getPath();
            }
            throw new RequestScopedException("JordanException: Jordan is superman, you cannot be in this url: " + url);
        }

        return "No execute JordanExceptionMapProvider, this url: " + url;
    }

    @GET
    @Path("SessionScopedProvider/{id}")
    public String getSessionScopedException(@PathParam("id") String msgId)
                    throws SessionScopedException {
        String name = "jordan";
        String url = "null uriinfo";
        if (msgId.trim() == name || msgId.trim().equals(name)) {
            if (uriinfo != null) {
                url = uriinfo.getPath();
            }
            throw new SessionScopedException("JordanException: Jordan is superman, you cannot be in this url: " + url);
        }

        return "No execute JordanExceptionMapProvider, this url: " + url;
    }

    /**
     * Processes a POST request and returns the incoming request message.
     *
     * @param incomingMessage the request body is mapped to the String by the
     *            JAX-RS runtime using a built-in entity provider
     * @return the original request body
     */
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String postMessage(String incomingMessage) {
        // A plain Java parameter is used to represent the request body. The
        // JAX-RS runtime will map the request body to a String.
        ProviderResource.message = incomingMessage;
        return incomingMessage;
    }

    /**
     * Processes a PUT request and returns the incoming request message.
     *
     * @param incomingMessage the request body is mapped to the byte[] by the
     *            JAX-RS runtime using a built-in entity provider
     * @return the original request body in a JAX-RS Response object
     */
    @PUT
    public Response putMessage(byte[] incomingMessage) {
        // Note that different Java types can be used to map the
        // incoming request body to a Java type.
        ProviderResource.message = new String(incomingMessage);

        // Note that a javax.ws.rs.core.Response object is returned. A Response
        // object can be built which contains additional HTTP headers, a status
        // code, and the entity body.
        return Response.ok(incomingMessage).type(MediaType.TEXT_PLAIN).build();
    }

    /**
     * Processes a DELETE request.
     *
     * @return an empty response with a 204 status code
     */
    @DELETE
    public Response deleteMessage() {
        ProviderResource.message = null;
        // Note that a javax.ws.rs.core.Response object is returned. In this
        // method a HTTP 204 status code (No Content) is returned.
        return Response.noContent().build();
    }
}
