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
/**
 *
 */
package jaxrs21sse.jsonb;

import static jaxrs21sse.jsonb.JsonObject.JSON_OBJECTS;
import static jaxrs21sse.jsonb.JsonbObject.JSONB_OBJECTS;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

@ApplicationPath("/")
@Path("/jsonb")
public class SseJsonbResource extends Application {

    @GET
    @Path("/json3")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void send3JsonEvents(@Context SseEventSink eventSink, @Context Sse sse) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try (SseEventSink sink = eventSink) {
                    for (int i = 0; i < JSON_OBJECTS.length; i++) {
                        OutboundSseEvent event = sse.newEventBuilder()
                                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                        .data(JsonObject.class, JSON_OBJECTS[i])
                                        .build();
                        System.out.println("SseJsonbResource.send3JsonEvents() sending: " + JSON_OBJECTS[i]);
                        sink.send(event);
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                            SseJsonbTestServlet.resourceFailures.add("InterrupedException while sleeping in SseJsonbResource.send3JsonEvents");
                            ex.printStackTrace();
                        }
                    }

                }
                if (!eventSink.isClosed()) {
                    SseJsonbTestServlet.resourceFailures.add("AutoClose in SseJsonbResource.send3JsonEvents failed for eventSink");
                }

            }

        };
        new Thread(r).start();
    }

    @GET
    @Path("/jsonb3")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void send3JsonbEvents(@Context SseEventSink eventSink, @Context Sse sse) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try (SseEventSink sink = eventSink) {
                    for (int i = 0; i < JSONB_OBJECTS.length; i++) {
                        OutboundSseEvent event = sse.newEventBuilder()
                                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                        .data(JsonbObject.class, JSONB_OBJECTS[i])
                                        .build();
                        System.out.println("SseJsonbResource.send3JsonbEvents() sending: " + JSONB_OBJECTS[i]);
                        sink.send(event);
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                            SseJsonbTestServlet.resourceFailures.add("InterrupedException while sleeping in SseJsonbResource.send3JsonbEvents");
                            ex.printStackTrace();
                        }
                    }

                }
                if (!eventSink.isClosed()) {
                    SseJsonbTestServlet.resourceFailures.add("AutoClose in SseJsonbResource.send3JsonEvents failed for eventSink");
                }

            }

        };
        new Thread(r).start();
    }


}
