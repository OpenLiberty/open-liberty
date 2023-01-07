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
package io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch;

import java.io.Serializable;

import io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch.simpleresource.Person;
import io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch.simpleresource.SimpleBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code HelloWorldResource} is a simple POJO which is annotated with
 * JAX-RS annotations to turn it into a JAX-RS resource.
 * <p/>
 * This class has a {@link Path} annotation with the value "helloworld" which
 * means the resource will be available at:
 * {@code http://&lt;hostname&gt;:&lt;port&gt/&lt;context root&gt;/&lt;servlet path&gt;/helloworld}
 * <p/>
 * Remember to add this resource class to the {@link HelloWorldApplication#getClasses()} method.
 */
@SessionScoped
@Path("/SessionScopedResource")
public class SessionScopedResource implements Serializable {

    private static final long serialVersionUID = 663911461107548545L;

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new {@code HelloWorldResource} object is created
     * per request.
     */
    private static volatile String message = "Hello World from restfulWS-3.0!";

    @Inject
    private SimpleBean injected;

    @Inject
    private Person person;

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
        if (injected == null) {
            s = s + "injected is null...FAILED";
        } else {
            s = s + "injected is NOT null...";
            try {
                String s2 = injected.getMessage();
                if (s2 != null) {
                    s = s + "injected.getMessage returned..." + s2 + ", " + person.talk();
                } else {
                    s = s + "injected.getMessage returned null...FAILED";
                }
            } catch (Exception e) {
                s = s + "caught exception: " + e + "...FAILED";
            }
        }

        return s;
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
        SessionScopedResource.message = incomingMessage;
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
        SessionScopedResource.message = new String(incomingMessage);

        // Note that a jakarta.ws.rs.core.Response object is returned. A Response
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
        SessionScopedResource.message = null;
        // Note that a jakarta.ws.rs.core.Response object is returned. In this
        // method a HTTP 204 status code (No Content) is returned.
        return Response.noContent().build();
    }
}
