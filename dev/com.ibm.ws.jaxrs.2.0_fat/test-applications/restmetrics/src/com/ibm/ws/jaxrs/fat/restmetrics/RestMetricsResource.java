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

import java.lang.management.ManagementFactory;

import javax.ejb.Asynchronous;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.websphere.jaxrs.monitor.RestfulStatsMXBean;

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

    // Array to hold the names that identify the methods in monitor 1.0
    public static final String[] MONITOR_STRINGS = {
                                                   "WebSphere:type=RESTful_Stats,name=restmetrics/com.ibm.ws.jaxrs.fat.restmetrics."
                                                   + "RestMetricsResource.getMessage()",
                                                   "WebSphere:type=RESTful_Stats,name=restmetrics/com.ibm.ws.jaxrs.fat.restmetrics."
                                                   + "RestMetricsResource.asyncMethod(javax.ws.rs.container.AsyncResponse)",
                                                   "WebSphere:type=RESTful_Stats,name=restmetrics/com.ibm.ws.jaxrs.fat.restmetrics."
                                                   + "RestMetricsResource.getMultiParamMessage(java.lang.String)",
                                                   "WebSphere:type=RESTful_Stats,name=restmetrics/com.ibm.ws.jaxrs.fat.restmetrics."
                                                   + "RestMetricsResource.getMultiParamMessage(java.lang.String_java.lang.String)",
                                                   "WebSphere:type=RESTful_Stats,name=restmetrics/com.ibm.ws.jaxrs.fat.restmetrics."
                                                   + "RestMetricsResource.postMessage(java.lang.String)",
                                                   "WebSphere:type=RESTful_Stats,name=restmetrics/com.ibm.ws.jaxrs.fat.restmetrics."
                                                   + "RestMetricsResource.putMessage(java.lang.String)",
                                                   "WebSphere:type=RESTful_Stats,name=restmetrics/com.ibm.ws.jaxrs.fat.restmetrics."
                                                   + "RestMetricsResource.deleteMessage()",
                                                   "WebSphere:type=RESTful_Stats,name=restmetrics/com.ibm.ws.jaxrs.fat.restmetrics."
                                                   + "RestMetricsResource.getCheckedException(java.lang.String)",
                                                   "WebSphere:type=RESTful_Stats,name=restmetrics/com.ibm.ws.jaxrs.fat.restmetrics."
                                                   + "RestMetricsResource.getUncheckedException(java.lang.String)"};

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new <code>RestMetricsResource</code> object is created
     * per request.
     */
    private static volatile String message = "Metrics!";

    private final int sleepTime = 250;

    /**
     * Processes a GET request and returns the stored message.
     *
     * @return sting indicating success
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        // Note that if null is returned from a resource method, a HTTP 204 (No
        // Content) status code response is sent.
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
            // no-op
        }
        return RestMetricsResource.message;
    }

    /**
     * Processes a GET request to test exception mapping.
     *
     * @return sting indicating success
     */
    @GET
    @Path("/async")
    @Produces(MediaType.TEXT_PLAIN)
    @Asynchronous
    public void asyncMethod(@Suspended final AsyncResponse asyncResponse) {
        // Returns a response on another thread
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                asyncResponse.resume(testAsync());
            }
        });
        t.start();
    }

    private String testAsync() {
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
            // no-op
        }

        return RestMetricsResource.message;
    }


    /**
     * Processes a GET request with single parameters.
     *
     * @param one simple sting
     * @return string indicating success
     */
    @GET
    @Path("{param1}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMultiParamMessage(@PathParam("param1") String p1) {
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
            // no-op
        }
        return RestMetricsResource.message;
    }

    /**
     * Processes a GET request throwing a checked exception.
     */
    @GET
    @Path("/checked/{param1}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getCheckedException(@PathParam("param1") String p1) throws MetricsMappedCheckedException, MetricsUnmappedCheckedException {
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
            // no-op
        }
        // Option to check mapped checked exception
        if (p1.equals("mappedChecked")) {
            throw new MetricsMappedCheckedException("Mapped Checked");
        }
        // Option to check an mapped unchecked exception
        if (p1.equals("unmappedChecked")) {
            throw new MetricsUnmappedCheckedException("Unmapped Checked");
        }
        return "FAILED!";
    }

    /**
     * Processes a GET throwing an unchecked exception.
     */
    @GET
    @Path("/unchecked/{param1}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUncheckedException(@PathParam("param1") String p1) {
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
            // no-op
        }
        // Option to check an unmapped checked exception
        if (p1.equals("mappedUnchecked")) {
            throw new MetricsMappedUncheckedException("Mapped Unchecked");
        }
        // Option to check an unmapped unchecked exception
        if (p1.equals("unmappedUnchecked")) {
            throw new MetricsUnmappedUncheckedException("Unmapped Unchecked");
        }
        return "FAILED!";
    }

    /**
     * Processes a GET request with multiple parameters.
     *
     * @param two simple stings
     * @return string indicating success
     */
    @GET
    @Path("{param1}/{param2}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMultiParamMessage(
                                         @PathParam("param1") String p1,
                                       @PathParam("param2") String p3) {
        // Note that if null is returned from a resource method, a HTTP 204 (No
        // Content) status code response is sent.
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
            // no-op
        }
        return RestMetricsResource.message;
    }

    /**
     * Processes a POST request and returns the incoming request message.
     *
     * @param incomingMessage containing simple string
     * @return the original message sent indicating successful execution
     */
    @Path("/post1")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String postMessage(String incomingMessage) {
        // A plain Java parameter is used to represent the request body. The
        // JAX-RS runtime will map the request body to a String.
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
            // no-op
        }
        RestMetricsResource.message = incomingMessage;
        return incomingMessage;
    }

    /**
     * Processes a PUT request and returns the incoming request message.
     *
     * @param incomingMessage containing simple string
     * @throws exception to test having an exception in the signature
     * @return the original message sent indicating successful execution
     */
    @Path("/put1")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String putMessage(String incomingMessage) throws Exception {
        try {
            Thread.sleep(sleepTime);
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
            Thread.sleep(sleepTime);
        } catch (Exception e) {
            // no-op
        }
        RestMetricsResource.message = null;
        // Note that a javax.ws.rs.core.Response object is returned. In this
        // method a HTTP 204 status code (No Content) is returned.
        return Response.noContent().build();
    }

    /**
     * Resource method to verify the monitor 1.0 data is correct.
     *
     * @return the stored message
     */
    @GET
    @Path("{p1}/{p2}/{p3}")
    @Produces(MediaType.TEXT_PLAIN)
    public String checkMonitorValues(
                                     @PathParam("p1") int index,
                                     @PathParam("p2") int count,
                                     @PathParam("p3") double responseTime) throws Exception {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName monitorObject = new ObjectName(MONITOR_STRINGS[index]);

        // Check monitor stats
        RestfulStatsMXBean restfulStats = JMX.newMXBeanProxy(mbs, monitorObject, RestfulStatsMXBean.class);
        long monitorCount = restfulStats.getRequestCount();
        double monitorResponseTime = restfulStats.getResponseTime();
        if (monitorCount != count) {
            return "Failed:  Expected method count " + count + ", received " + monitorCount
                   + ":  index = " + index;

        }
        double threshold = .0001;
        monitorResponseTime /= 1000000;
        if (Math.abs(monitorResponseTime - responseTime) > threshold) {
            return "Failed:  Expected response time " + responseTime + ", received " + monitorResponseTime
                   + ":  index = " + index;
        }

        return "Passed!";
    }

}
