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
package jaxrs21sse.jaxb;

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
@Path("/jaxb")
public class SseJaxbResource extends Application {

    @GET
    @Path("/jaxb1")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void send3JaxbEvents(@Context SseEventSink eventSink, @Context Sse sse) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try (SseEventSink sink = eventSink) {
                    TestXML testxml = new TestXML(10, 20);
                    OutboundSseEvent event = sse.newEventBuilder()
                                    .mediaType(MediaType.APPLICATION_XML_TYPE)
                                    .data(TestXML.class, testxml)
                                    .build();
                    System.out.println("SseJaxbResource.send3JaxbEvents() sending: TestXML" + testxml.x + ":  " + testxml.y);
                    sink.send(event);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        SseJaxbTestServlet.resourceFailures.add("InterrupedException while sleeping in SseJaxbResource.sendeJaxbEvents");
                        ex.printStackTrace();
                    }

                }
                if (!eventSink.isClosed()) {
                    SseJaxbTestServlet.resourceFailures.add("AutoClose in SseJaxbResource.send3JaxbEvents failed for eventSink");
                }

            }

        };
        new Thread(r).start();
    }

}
