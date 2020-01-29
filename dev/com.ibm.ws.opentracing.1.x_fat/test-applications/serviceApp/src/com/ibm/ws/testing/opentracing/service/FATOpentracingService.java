/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testing.opentracing.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * <p>Open tracing FAT service.</p>
 */
@ApplicationPath("rest") // 'APP_PATH' in the constants
@Path("testService") // 'SERVICE_PATH' in the constants
public class FATOpentracingService extends Application implements FATOpentracingConstants {
    // Trace ...
    
    private static final String CLASS_NAME = FATOpentracingService.class.getSimpleName();

    @SuppressWarnings("unused")
    private static void trace(String methodName) {
        System.out.println(CLASS_NAME + "." + methodName);
    }

    private static void trace(String methodName, String valueName, Object value) {
        System.out.println(CLASS_NAME + "." + methodName + " " + valueName + " [ " + value + " ]");
    }

    private static void trace(
        String methodName,
        String value1Name, Object value1,
        String value2Name, Object value2) {

        System.out.println(
            CLASS_NAME + "." + methodName + " " +
            value1Name + " [ " + value1 + " ] " +
            value2Name + " [ " + value2 + " ]");
    }

    private static void traceEnterReturn(String methodName, String valueName, Object value) {
        System.out.println(CLASS_NAME + "." + methodName + " ENTER / RETURN " + valueName + " [ " + value + " ]");
    }

    private static void traceEnter(String methodName) {
        System.out.println(CLASS_NAME + "." + methodName + " ENTER");
    }

    private static void traceEnter(String methodName, String valueName, Object value) {
        System.out.println(CLASS_NAME + "." + methodName + " ENTER " + valueName + " [ " + value + " ]");
    }

    private static void traceEnter(
        String methodName,
        String value1Name, Object value1,
        String value2Name, Object value2) {

        System.out.println(
            CLASS_NAME + "." + methodName + " ENTER " +
            value1Name + " [ " + value1 + " ] " +
            value2Name + " [ " + value2 + " ]");
    }

    private static void traceReturn(String methodName) {
        System.out.println(CLASS_NAME + "." + methodName + " RETURN");
    }

    private static void traceReturn(String methodName, String valueName, Object value) {
        System.out.println(CLASS_NAME + "." + methodName + " RETURN " + valueName + " [ " + value + " ]");
    }

    // Injected tracer ...
    //
    // Used for explicit trace operations, and to access the trace state.

    /**
     * <p>The open tracing tracer.  Injected.</p>
     *
     * <p>Specified with dependent scope.  But, since the tracer factory answers the same
     * tracer anywhere in an application, the scope is effectively application</p>
     */
    @Inject
    public Tracer tracer;

    /**
     * <p>Answer the injected tracer.  For testing this is expected to
     * be a mock tracer.</p>
     *
     * @return The injected tracer.
     */
    private Tracer getTracer() {
        return tracer;
    }

    /**
     * <p>Start a new child span of the active span.</p>
     *
     * <p>The child span must be finished using {@link #finishChildSpan}
     * before completing the service request which created the span.</p>
     * 
     * If there is no active span, the newly created span is made the
     * active span.
     *
     * @param spanName The name to give the new child span.
     *
     * @return The new child span.
     */
    private Span startChildSpan(String spanName) {
        String methodName = "createChildSpan";
        traceEnter(methodName, "SpanName", spanName);

        Span childSpan;

        Tracer useTracer = getTracer();
        if ( useTracer == null ) {
            childSpan = null;

        } else {
            Span activeSpan = useTracer.activeSpan();
            Tracer.SpanBuilder spanBuilder = useTracer.buildSpan(spanName);
            if (activeSpan != null) {
                spanBuilder.asChildOf( activeSpan.context() );
            }
            childSpan = spanBuilder.start();
            if (activeSpan == null) {
                useTracer.scopeManager().activate(childSpan, true);
            }
        }

        traceReturn(methodName, "ChildSpan", childSpan);
        return childSpan;
    }

    /**
     * <p>Finish a child span of the active span.</p>
     *
     * @param spanName The name of the child span.
     * @param span The child span.
     */
    private void finishChildSpan(String spanName, Span span) {
        String methodName = "destroyChildSpan";
        traceEnter(methodName, "SpanName", spanName, "Span", span);

        span.finish();

        traceReturn(methodName);
    }

    //

    /**
     * <p>Service API used to peek at the tracer state.</p>
     *
     * <p>Produces the print string of the injected tracer as
     * plain text.</p>
     *
     * @return The print string of the injected tracer as plain
     *    text.
     */
    @GET
    @Path(GET_TRACER_STATE_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public String getTracerState() {
        Tracer useTracer = getTracer();
        if ( useTracer == null ) {
            return "*** TRACER INJECTION FAILURE ***";
        } else {
            return useTracer.toString();
        }
    }

    // Basic service API ... 'getImmediate' and 'getManual', and 'getDelayed'

    /**
     * <p>Service API: Handle a non-delayed GET.</p>
     *
     * @param responseText The text to answer by the service request.
     *     This is injected as the "response" query parameter.
     *
     * @return The response text as plain text.
     */
    @GET
    @Path(GET_IMMEDIATE_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public String getImmediate(@QueryParam(RESPONSE_PARAM_NAME) String responseText) {
        String methodName = "getImmediate";
        traceEnterReturn(methodName, "ResponseText", responseText);
        return responseText;
    }

    @GET
    @Path(GET_MANUAL_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public String getManual(@QueryParam(RESPONSE_PARAM_NAME) String responseText)
        throws Exception {
        traceEnter("getManual", "ResponseText", responseText);

        String childSpanName = "manualSpan";
        Span childSpan = startChildSpan(childSpanName);
        if ( childSpan != null ) {
            // TODO: Put something testable in the child span.
            finishChildSpan(childSpanName, childSpan);
        }

        traceReturn("getManual", "ResponseText", responseText);
        return responseText;
    }
    
    /**
     * <p>Service API: Handle a delayed GET.</p>
     *
     * @param asyncResponse An asynchronous response used to provide
     *     the response from the delay thread.  This is injected using
     *     {@link Suspended}.
     * @param delay The count of seconds to delay the response.
     *     This is injected as the "delay" query parameter.
     * @param responseText The text to answer by the service request.
     *     This is injected as the "response" query parameter.
     */
    @GET
    @Path(GET_DELAYED_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public void getDelayed(
        @Suspended AsyncResponse asyncResponse,
        @QueryParam(DELAY_PARAM_NAME) int delay,
        @QueryParam(RESPONSE_PARAM_NAME) String responseText) {

        String methodName = "getDelayed";
        traceEnter(methodName, "Delay", Integer.valueOf(delay), "ResponseText", responseText);

        try {
            Thread.sleep(delay * MSEC_IN_SEC);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        asyncResponse.resume(responseText);

        // Current OT spec doesn't support AsyncResponse in a different thread.
        // 
        // Thread delayThread = new Thread(
        //    delayResponse(asyncResponse,
        //    delay * MSEC_IN_SEC,
        //    responseText) );
        // delayThread.start();
        
        traceReturn(methodName);
    }

    /**
     * <p>Create a runnable which waits a specified number of milliseconds
     * then resumes the response using the specified text.</p>
     *
     * @param asyncResponse The response through which to resume the request.
     * @param delayMS The delay amount in milliseconds.
     * @param responseText Text used to resume the request.
     *
     * @return Runnable A new runnable used to delay responding to a request.
     */
    @SuppressWarnings("unused")
    private static Runnable delayResponse(
        final AsyncResponse asyncResponse,
        final int delayMS,
        final String responseText) {

        return new Runnable() {
            public void run() {
                String methodName = "run";
                innerTraceEnter(
                    methodName,
                    "Delay", Integer.valueOf(delayMS),
                    "ReponseText", responseText);

                try {
                    Thread.sleep(delayMS);
                    asyncResponse.resume(responseText);
                } catch ( InterruptedException ex ) {
                    throw new RuntimeException(ex);
                }

                innerTraceReturn(methodName);
            }
        };
    }

    // Delay utility ...

    /** <p>The count of milliseconds in one second.</p> */
    private final int MSEC_IN_SEC = 1000;

    private static final String INNER_CLASS_NAME = FATOpentracingService.class.getSimpleName() + "$Runnable";

    protected static void innerTraceEnter(
            String methodName,
            String value1Name, Object value1,
            String value2Name, Object value2) {

            System.out.println(
                INNER_CLASS_NAME + "." + methodName + " ENTER " +
                value1Name + " [ " + value1 + " ]" +
                value2Name + " [ " + value2 + " ]");
    }

    protected static void innerTraceReturn(String methodName) {
        System.out.println(INNER_CLASS_NAME + "." + methodName + " RETURN");
    }

    // Nested service API ... 'getNested'

    /**
     * <p>Test of nested operations.</p>
     *
     * <p>Three behaviors are supported, depending on the nesting depth parameter.</p>
     *
     * <p>A nesting depth of zero (0) causes an immediate return with the specified
     * response text.</p>
     *
     * <p>A nesting depth of one (1) causes nested calls to be made to the delay service,
     * with delays specified at two seconds (2s), four seconds (4s), and six seconds (6s).</p>
     *
     * <p>A nesting depth of greater than one causes an call to the nesting service with
     * the depth reduced by one (1).</p>
     *
     * @param hostName The host to which to make nested requests.
     * @param portNumber The port ot which to make nested requests.
     * @param nestDepth The depth of nesting to use when implementing the request.
     * @param responseText Test to answer from the request.  This is modified for
     *     nested requests.
     * @return HTML response text.
     * @throws Exception
     */
    @GET
    @Path(GET_NESTED_PATH)
    @Produces(MediaType.TEXT_HTML)
    public String getNested(
            @QueryParam(HOST_PARAM_NAME) String hostName,
            @QueryParam(PORT_PARAM_NAME) int portNumber,
            @QueryParam(NEST_DEPTH_PARAM_NAME) int nestDepth,
            @QueryParam(ASYNC_PARAM_NAME) boolean async,
            @QueryParam(CONTEXT_ROOT_PARAM_NAME) String contextRoot,
            @QueryParam(RESPONSE_PARAM_NAME) String responseText)
        throws Exception {

        String methodName = "getNested";
        traceEnter(methodName);
        trace(methodName, "HostName", hostName, "PortNumber", Integer.valueOf(portNumber));
        trace(methodName, "ContextRoot", contextRoot);
        trace(methodName, "NestDepth", Integer.valueOf(nestDepth));
        trace(methodName, "Async", Boolean.valueOf(async));
        trace(methodName, "ResponseText", responseText);

        String finalResponse;

        if ( nestDepth == 0 ) {
            finalResponse = responseText;

        } else if ( nestDepth > 1 ) {
            Map<String, Object> nestParameters  = new HashMap<String, Object>();
            nestParameters.put(HOST_PARAM_NAME, hostName);
            nestParameters.put(PORT_PARAM_NAME, Integer.valueOf(portNumber));
            nestParameters.put(NEST_DEPTH_PARAM_NAME, Integer.valueOf(nestDepth - 1));
            nestParameters.put(ASYNC_PARAM_NAME, Boolean.valueOf(async));
            nestParameters.put(CONTEXT_ROOT_PARAM_NAME, contextRoot);
            nestParameters.put(RESPONSE_PARAM_NAME, responseText);

            String requestPath = FATUtilsService.getRequestPath(
                contextRoot,
                APP_PATH, SERVICE_PATH, GET_NESTED_PATH, nestParameters);
                // throws UnsupportedEncodingException

            String requestUrl = FATUtilsService.getRequestUrl(hostName, portNumber, requestPath);

            Response nestedResponse = FATUtilsService.invoke(requestUrl);

            finalResponse = nestedResponse.readEntity(String.class);

        } else {
            Map<String, Object> delay2Parameters  = new HashMap<String, Object>();
            delay2Parameters.put(DELAY_PARAM_NAME, Integer.valueOf(2));
            delay2Parameters.put(RESPONSE_PARAM_NAME, responseText + " [ 2 ]");

            String delay2Path = FATUtilsService.getRequestPath(
                contextRoot,
                APP_PATH, SERVICE_PATH, GET_DELAYED_PATH,
                delay2Parameters);
                // throws UnsupportedEncodingException
            String delay2Url = FATUtilsService.getRequestUrl(hostName, portNumber, delay2Path);

            Map<String, Object> delay4Parameters  = new HashMap<String, Object>();
            delay4Parameters.put(DELAY_PARAM_NAME, Integer.valueOf(4));
            delay4Parameters.put(RESPONSE_PARAM_NAME, responseText + " [ 4 ]");

            String delay4Path = FATUtilsService.getRequestPath(
                contextRoot,
                APP_PATH, SERVICE_PATH, GET_DELAYED_PATH,
                delay4Parameters);
                // throws UnsupportedEncodingException
            String delay4Url = FATUtilsService.getRequestUrl(hostName, portNumber, delay4Path);

            Map<String, Object> delay6Parameters  = new HashMap<String, Object>();
            delay6Parameters.put(DELAY_PARAM_NAME, Integer.valueOf(6));
            delay6Parameters.put(RESPONSE_PARAM_NAME, responseText + " [ 6 ]");

            String delay6Path = FATUtilsService.getRequestPath(
                contextRoot,
                APP_PATH, SERVICE_PATH, GET_DELAYED_PATH,
                delay6Parameters);
                // throws UnsupportedEncodingException
            String delay6Url = FATUtilsService.getRequestUrl(hostName, portNumber, delay6Path);

            String response2;
            String response4;
            String response6;

            if ( async ) {
                Future<Response> delay2Response = FATUtilsService.invokeAsync(delay2Url);
                Future<Response> delay4Response = FATUtilsService.invokeAsync(delay4Url);
                Future<Response> delay6Response = FATUtilsService.invokeAsync(delay6Url);

                response2 = delay2Response.get().readEntity(String.class);
                response4 = delay4Response.get().readEntity(String.class);
                response6 = delay6Response.get().readEntity(String.class);
                // 'get' throws InterruptedException, ExecutionException

            } else {
                Response immediate2Response = FATUtilsService.invoke(delay2Url);
                response2 = immediate2Response.readEntity(String.class);

                Response immediate4Response = FATUtilsService.invoke(delay4Url);
                response4 = immediate4Response.readEntity(String.class);

                Response immediate6Response = FATUtilsService.invoke(delay6Url);
                response6 = immediate6Response.readEntity(String.class);
            }

            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("<h3>" + CLASS_NAME + "." + methodName + "</h3>" + "\n");
            responseBuilder.append("<ul>" + "\n");
            responseBuilder.append("<li>" + response2 + "</li>" + "\n");
            responseBuilder.append("<li>" + response4 + "</li>" + "\n");
            responseBuilder.append("<li>" + response6 + "</li>" + "\n");
            responseBuilder.append("</ul>" + "\n");
            finalResponse = responseBuilder.toString();
        }

        traceReturn(methodName, "FinalResponse", finalResponse);
        return finalResponse;
    }

    /**
     * Simple endpoint that takes a {@code responseText} query parameter and
     * returns its value as a plain/text response. This can be useful to vary
     * different requests and test different exclude/include filters.
     * @param responseText The text to return.
     * @return {@code responseText}
     */
    @GET
    @Path(GET_EXCLUDE_TEST_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public String excludeTest(@QueryParam(RESPONSE_PARAM_NAME) String responseText) {
        String methodName = "excludeTest";
        traceEnterReturn(methodName, "ResponseText", responseText);
        return responseText;
    }
}
