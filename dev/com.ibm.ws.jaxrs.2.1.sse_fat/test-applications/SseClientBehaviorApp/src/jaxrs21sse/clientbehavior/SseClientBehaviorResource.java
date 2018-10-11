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
/**
 *
 */
package jaxrs21sse.clientbehavior;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

@ApplicationPath("/")
@Path("/clientBehavior")
@Produces(MediaType.SERVER_SENT_EVENTS)
public class SseClientBehaviorResource extends Application {
    private static Logger _log = Logger.getLogger(SseClientBehaviorResource.class.getName());

//    @Context
//    SseEventSink eventSink;

    @Context
    Sse sse;

    private static AtomicInteger retryCounter = new AtomicInteger(0);

    @GET
    @Path("/oneEventThenNormalClose")
    public void oneEventThenNormalClose(@Context SseEventSink eventSink) {
        normalSseSend(eventSink, sse, "foo");
    }

    @GET
    @Path("/200NonSseContentType")
    public void nonSseContentType(@Context SseEventSink eventSink) {
        throw new WebApplicationException(
            Response.ok("MediaType is application/json, not text/event-stream")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .build());
    }

    @GET
    @Path("/204")
    public void noContent(@Context SseEventSink eventSink) {
        throw new WebApplicationException(Response.noContent().build());
    }

    @GET
    @Path("/503NoRetryAfter")
    public void noRetryAfter(@Context SseEventSink eventSink) {
        throw new WebApplicationException(Response.status(503).build());
    }

    @GET
    @Path("/503InvalidRetryAfter")
    public void invalidRetryAfter(@Context SseEventSink eventSink) {
        throw new WebApplicationException(Response.status(503).header(HttpHeaders.RETRY_AFTER, "NEVER!!!").build());
    }

    @GET
    @Path("/503ValidRetryAfter")
    public void validRetryAfter(@Context SseEventSink eventSink) {
        _log.info("validRetryAfter retryCounter=" + retryCounter.get());
        if (0 == retryCounter.getAndIncrement()) {
            throw new WebApplicationException(Response.status(503).header(HttpHeaders.RETRY_AFTER, "3").build());
        }
        normalSseSend(eventSink, sse, "successAfterRetry1", "successAfterRetry2");
    }

    private static void normalSseSend(SseEventSink eventSink, Sse sse, final String... data) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try (SseEventSink sink = eventSink) {
                    for (String eventData : data) {
                        _log.info("normalSseSend() sending: " + eventData);
                        sink.send(sse.newEvent(eventData));
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                            SseClientBehaviorTestServlet.resourceFailures.add("InterrupedException while sleeping in SseClientBehaviorResource.oneEventThenNormalClose()");
                            ex.printStackTrace();
                        }
                    }
                } // auto close eventSink should cause the client's completion listener to fire
            }

        };
        new Thread(r).start();
    }
}
