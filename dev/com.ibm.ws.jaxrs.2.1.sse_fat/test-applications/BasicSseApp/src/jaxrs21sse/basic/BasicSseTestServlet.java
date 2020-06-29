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
package jaxrs21sse.basic;

import static jaxrs21sse.basic.JaxbObject.JAXB_OBJECTS;
import static jaxrs21sse.basic.JsonObject.JSON_OBJECTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.junit.After;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/BasicSseTestServlet")
public class BasicSseTestServlet extends FATServlet {
    static List<String> resourceFailures = new ArrayList<String>();

    @After
    public void checkForResourceFailures() {
        try {
            String msg = null;
            if (!resourceFailures.isEmpty()) {
                msg = "ResourceFailures: ";
                for (String failure : resourceFailures) {
                    msg += failure + "\n";
                }
            }
            assertNotNull("Detected failures in the SSE resource: " + msg, msg);
        } finally {
            resourceFailures.clear();
        }
    }

    public void testSimpleDirectTextPlainSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<String> receivedEvents = new ArrayList<String>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/plain3");

        try (SseEventSource source = SseEventSource.target(target).build()) {
            System.out.println("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    System.out.println("new plain event: " + t.getId() + " " + t.getName() + " " + t.readData());
                                    receivedEvents.add(t.readData(String.class));
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    t.printStackTrace();
                                    fail("Caught unexpected exception: " + t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    System.out.println("completion runnable executed");
                                    executionLatch.countDown();
                                }
                            });

            source.open();
            System.out.println("client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        }

        assertEquals("Received an unexpected number of events", 3, receivedEvents.size());
        assertEquals("Unexpected event or event out of order", "uno", receivedEvents.get(0));
        assertEquals("Unexpected event or event out of order", "dos", receivedEvents.get(1));
        assertEquals("Unexpected event or event out of order", "tres", receivedEvents.get(2));
    }

    public void testIntegerSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<Integer> receivedEvents = new ArrayList<Integer>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/integer3");

        try (SseEventSource source = SseEventSource.target(target).build()) {
            System.out.println("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    System.out.println("new integer event: " + t.getId() + " " + t.getName() + " " + t.readData());
                                    receivedEvents.add(t.readData(Integer.class));
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    t.printStackTrace();
                                    fail("Caught unexpected exception: " + t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    System.out.println("completion runnable executed");
                                    executionLatch.countDown();
                                }
                            });

            source.open();
            System.out.println("client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        }

        assertEquals("Received an unexpected number of events", 3, receivedEvents.size());
        assertEquals("Unexpected event or event out of order", 1, receivedEvents.get(0).intValue());
        assertEquals("Unexpected event or event out of order", 2, receivedEvents.get(1).intValue());
        assertEquals("Unexpected event or event out of order", 3, receivedEvents.get(2).intValue());
    }

    public void testJsonSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<JsonObject> receivedEvents = new ArrayList<JsonObject>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/json3");

        SseEventSource source = SseEventSource.target(target).build();
        try {
            System.out.println("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    JsonObject o = t.readData(JsonObject.class, MediaType.APPLICATION_JSON_TYPE);
                                    System.out.println("new json event: " + o);
                                    receivedEvents.add(o);
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    t.printStackTrace();
                                    fail("Caught unexpected exception: " + t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    System.out.println("completion runnable executed");
                                    executionLatch.countDown();
                                }
                            });

            for (int i = 0; i < 3; i++) {
                try {
                    source.open();
                    break;
                } catch (Throwable t) {
                    t.printStackTrace();
                    source.close();
                    source = SseEventSource.target(target).build();
                }
            }

            System.out.println("client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        } finally {
            source.close();
        }

        assertEquals("Received an unexpected number of events", 3, receivedEvents.size());
        assertEquals("Unexpected event or event out of order", JSON_OBJECTS[0], receivedEvents.get(0));
        assertEquals("Unexpected event or event out of order", JSON_OBJECTS[1], receivedEvents.get(1));
        assertEquals("Unexpected event or event out of order", JSON_OBJECTS[2], receivedEvents.get(2));
    }

    public void testJaxbSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<JaxbObject> receivedEvents = new ArrayList<JaxbObject>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        System.out.println("port = " + port);
        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/jaxb3");

        SseEventSource source = SseEventSource.target(target).build();
        try {
            System.out.println("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    JaxbObject o = t.readData(JaxbObject.class, MediaType.APPLICATION_XML_TYPE);
                                    System.out.println("new jaxb event: " + o);
                                    receivedEvents.add(o);
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    t.printStackTrace();
                                    fail("Caught unexpected exception: " + t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    System.out.println("completion runnable executed");
                                    executionLatch.countDown();
                                }
                            });

            for (int i = 0; i < 3; i++) {
                try {
                    source.open();
                    break;
                } catch (Throwable t) {
                    t.printStackTrace();
                    source.close();
                    source = SseEventSource.target(target).build();
                }
            }

            System.out.println("client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        } finally {
            source.close();
        }

        assertEquals("Received an unexpected number of events", 3, receivedEvents.size());
        assertEquals("Unexpected event or event out of order", JAXB_OBJECTS[0], receivedEvents.get(0));
        assertEquals("Unexpected event or event out of order", JAXB_OBJECTS[1], receivedEvents.get(1));
        assertEquals("Unexpected event or event out of order", JAXB_OBJECTS[2], receivedEvents.get(2));
    }

    public void testSseWithRX(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String[] nameData = new String[] { "John", "Jacob", "Jingleheimer", "Schmidt" };
        final List<String> receivedEvents = new ArrayList<String>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        WebTarget postTarget = client.target("http://localhost:" + port + "/BasicSseApp/basic/postPort");
        postTarget.request().post(Entity.xml(String.valueOf(port)));

        WebTarget nameTarget = client.target("http://localhost:" + port + "/BasicSseApp/basic/postName");
        for (int i = 0; i < nameData.length; i++) {
            CompletionStage<Response> completionStage = nameTarget.request().rx().post(Entity.xml(nameData[i]));
            CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();

            if (!(completableFuture.isDone())) {
                if (completableFuture.isCompletedExceptionally() || completableFuture.isCancelled()) {
                    System.out.print("testSseWithRX: completableFuture failed with an exception");
                } else {
                    System.out.print("testSseWithRX: sleeping....waiting for completableFuture to complete");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if (!(completableFuture.isDone())) {
                        System.out.print("testSseWithRX: completableFuture failed because it took too long");
                    }
                }
            }
        } 

        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/rx");
        SseEventSource.Builder sseBuilder = SseEventSource.target(target);
        SseEventSource source = sseBuilder.build();

        try {
            System.out.println("testSseWithRX:client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    System.out.println("testSseWithRX: new event: " + t.readData());
                                    receivedEvents.add(t.readData(String.class));
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    t.printStackTrace();
                                    fail("Caught unexpected exception: " + t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    System.out.println("testSseWithRX: completion runnable executed");
                                    executionLatch.countDown();
                                }
                            });

            source.open();
            System.out.println("testSseWithRX: client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        } finally {
            source.close();
        }
        assertEquals("Received an unexpected number of events", nameData.length, receivedEvents.size());
        System.out.println("testSseWithRX: " + receivedEvents + " that's my name too");
    }

    public void testErrorSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<JaxbObject> receivedEvents = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        System.out.println("port = " + port);
        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/error");

        SseEventSource source = SseEventSource.target(target).build();
        try {
            System.out.println("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    JaxbObject o = t.readData(JaxbObject.class, MediaType.APPLICATION_XML_TYPE);
                                    System.out.println("new event: " + o);
                                    receivedEvents.add(o);
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    System.out.println("new error: " + t);
                                    errors.add(t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    System.out.println("completion runnable executed");
                                    executionLatch.countDown();
                                }
                            });

            try {
                source.open();
            } catch (Throwable t) {
                t.printStackTrace();
                source.close();
                source = SseEventSource.target(target).build();
            }

            System.out.println("client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        } finally {
            source.close();
        }

        assertEquals("Received an unexpected number of events", 0, receivedEvents.size());
        assertEquals("Received an unexpected number of errors", 1, errors.size());
    }
}
