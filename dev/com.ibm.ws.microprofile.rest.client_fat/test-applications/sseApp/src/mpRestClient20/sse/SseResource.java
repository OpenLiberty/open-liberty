/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient20.sse;

import java.math.BigInteger;
import java.util.concurrent.Executors;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import mpRestClient20.sse.SomeObject.Color;

@ApplicationPath("/app")
@Path("/sse")
@Produces(MediaType.SERVER_SENT_EVENTS)
public class SseResource extends Application {

    @GET
    @Path("/send3strings")
    public void sendThreeStrings(@Context Sse sse, @Context SseEventSink sink) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try (SseEventSink s = sink) {
                sink.send(sse.newEvent("foo"));
                try {Thread.sleep(100);} catch (InterruptedException ex) {}
                sink.send(sse.newEvent("bar"));
                try {Thread.sleep(100);} catch (InterruptedException ex) {}
                sink.send(sse.newEvent("baz"));
            }
        });
    }

    @GET
    @Path("/send3ints")
    public void send3ints(@Context Sse sse, @Context SseEventSink sink,
                          @QueryParam("startingAt") int startingAt) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try (SseEventSink s = sink) {
                sink.send(sse.newEvent("" + startingAt));
                try {Thread.sleep(100);} catch (InterruptedException ex) {}
                sink.send(sse.newEvent("" + (startingAt + 1)));
                try {Thread.sleep(100);} catch (InterruptedException ex) {}
                sink.send(sse.newEvent("" + (startingAt + 2)));
            }
        });
    }

    @GET
    @Path("/send7objects")
    public void send7objects(@Context Sse sse, @Context SseEventSink sink) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try (SseEventSink s = sink) {
                sink.send(sse.newEventBuilder()
                          .data(new SomeObject("obj1", 17, 30.5, new BigInteger("123"), Color.RED))
                          .mediaType(MediaType.APPLICATION_JSON_TYPE)
                          .build());
                try {Thread.sleep(50);} catch (InterruptedException ex) {}
                sink.send(sse.newEventBuilder()
                          .data(new SomeObject("obj2", 18, 30.5, new BigInteger("123"), Color.RED))
                          .mediaType(MediaType.APPLICATION_JSON_TYPE)
                          .build());
                try {Thread.sleep(50);} catch (InterruptedException ex) {}
                sink.send(sse.newEventBuilder()
                          .data(new SomeObject("obj3", 19, 30.5, new BigInteger("123"), Color.RED))
                          .mediaType(MediaType.APPLICATION_JSON_TYPE)
                          .build());
                try {Thread.sleep(50);} catch (InterruptedException ex) {}
                sink.send(sse.newEventBuilder()
                          .data(new SomeObject("obj4", 20, 30.5, new BigInteger("123"), Color.RED))
                          .mediaType(MediaType.APPLICATION_JSON_TYPE)
                          .build());
                try {Thread.sleep(50);} catch (InterruptedException ex) {}
                sink.send(sse.newEventBuilder()
                          .data(new SomeObject("obj5", 21, 30.5, new BigInteger("123"), Color.RED))
                          .mediaType(MediaType.APPLICATION_JSON_TYPE)
                          .build());
                try {Thread.sleep(50);} catch (InterruptedException ex) {}
                sink.send(sse.newEventBuilder()
                          .data(new SomeObject("obj6", 22, 30.5, new BigInteger("123"), Color.RED))
                          .mediaType(MediaType.APPLICATION_JSON_TYPE)
                          .build());
                try {Thread.sleep(50);} catch (InterruptedException ex) {}
                sink.send(sse.newEventBuilder()
                          .data(new SomeObject("obj7", 23, 30.5, new BigInteger("123"), Color.BRIGHT_RED))
                          .mediaType(MediaType.APPLICATION_JSON_TYPE)
                          .build());
            }
        });
    }
}
