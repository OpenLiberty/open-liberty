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
package jaxrs21sse.basic;

import static jaxrs21sse.basic.DataObject.DATA_OBJECTS;

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
@Path("/basic")
public class BasicSseResource extends Application {

    @GET
    @Path("/plain3")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void send3PlainEvents(@Context SseEventSink eventSink, @Context Sse sse) {
        String[] eventData = new String[] { "uno", "dos", "tres" };
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try (SseEventSink sink = eventSink) {
                    for (int i = 0; i < eventData.length; i++) {
                        System.out.println("BasicSseResource.send3PlainEvents() sending: " + eventData[i]);
                        sink.send(sse.newEvent(eventData[i]));
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                            BasicSseTestServlet.resourceFailures.add("InterrupedException while sleeping in BasicSseResource.send3PlainEvents");
                            ex.printStackTrace();
                        }
                    }

                }
                if (!eventSink.isClosed()) {
                    BasicSseTestServlet.resourceFailures.add("AutoClose in BasicSseResource.send3PlainEvents failed for eventSink");
                }

            }

        };
        new Thread(r).start();
    }

    @GET
    @Path("/json3")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void send3JsonEvents(@Context SseEventSink eventSink, @Context Sse sse) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try (SseEventSink sink = eventSink) {
                    for (int i = 0; i < DATA_OBJECTS.length; i++) {
                        OutboundSseEvent event = sse.newEventBuilder()
                                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                        .data(DataObject.class, DATA_OBJECTS[i])
                                        .build();
                        System.out.println("BasicSseResource.send3JsonEvents() sending: " + DATA_OBJECTS[i]);
                        sink.send(event);
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                            BasicSseTestServlet.resourceFailures.add("InterrupedException while sleeping in BasicSseResource.send3JsonEvents");
                            ex.printStackTrace();
                        }
                    }

                }
                if (!eventSink.isClosed()) {
                    BasicSseTestServlet.resourceFailures.add("AutoClose in BasicSseResource.send3Json3Events failed for eventSink");
                }

            }

        };
        new Thread(r).start();
    }
}
