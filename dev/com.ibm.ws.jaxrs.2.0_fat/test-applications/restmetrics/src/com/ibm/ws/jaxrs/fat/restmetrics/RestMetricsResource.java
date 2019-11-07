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
package com.ibm.ws.jaxrs.fat.restmetrics;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * <code>RestMetricsResource</code> is a simple POJO which is annotated with
 * JAX-RS annotations to turn it into a JAX-RS resource.
 * <p/>
 * This class has a {@link Path} annotation with the value "metrics" which
 * means the resource will be available at:
 * <code>http://&lt;hostname&gt;:&lt;port&gt/&lt;context root&gt;/&lt;servlet path&gt;/restmetrics</code>
 * <p/>
 * Remember to add this resource class to the {@link RestMetricsApplication#getClasses()} method.
 */
@Path("/restmetrics")
public class RestMetricsResource {

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new <code>RestMetricsResource</code> object is created
     * per request.
     */
    private static volatile String message = "Metrics!";

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
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // no-op
        }
        return RestMetricsResource.message;
    }

    /**
     * Processes a POST request and returns the incoming request message.
     *
     * @param incomingMessage the request body is mapped to the String by the
     *            JAX-RS runtime using a built-in entity provider
     * @return the original request body
     */
    @Path("/post1")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String postMessage(String incomingMessage) {
        // A plain Java parameter is used to represent the request body. The
        // JAX-RS runtime will map the request body to a String.
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // no-op
        }
        RestMetricsResource.message = incomingMessage;
        return incomingMessage;
    }

    /**
     * Processes a PUT request and returns the incoming request message.
     *
     * @param incomingMessage the request body is mapped to the byte[] by the
     *            JAX-RS runtime using a built-in entity provider
     * @return the original request body in a JAX-RS Response object
     */
    @Path("/put1")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String putMessage(String incomingMessage) {
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // no-op
        }

        RestMetricsResource.message = incomingMessage;
        return incomingMessage;
    }

    /**
     * Processes a DELETE request.
     *
     * @return an empty response with a 204 status code
     */
    @Path("/delete1")
    @DELETE
    public Response deleteMessage() {
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // no-op
        }
        RestMetricsResource.message = null;
        // Note that a javax.ws.rs.core.Response object is returned. In this
        // method a HTTP 204 status code (No Content) is returned.
        return Response.noContent().build();
    }
}
