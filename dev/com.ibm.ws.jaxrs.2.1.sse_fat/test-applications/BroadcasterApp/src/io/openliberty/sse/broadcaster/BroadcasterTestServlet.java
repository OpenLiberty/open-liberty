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
package io.openliberty.sse.broadcaster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/BroadcasterTestServlet")
public class BroadcasterTestServlet extends FATServlet {
    private final static Logger _log = Logger.getLogger(BroadcasterTestServlet.class.getName());
    private final static int NUM_CLIENTS = 5;
    private long timeout = 5;
    
    private static final boolean isZOS() {
        String osName = System.getProperty("os.name");
        if (osName.contains("OS/390") || osName.contains("z/OS") || osName.contains("zOS")) {
            return true;
        }
        return false;
    }

    ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENTS * 2);

    @Test
    public void testClientReceivesBroadcastedEvents(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testClientReceivesBroadcastedEvents";
        if (isZOS()) {
            timeout = 35;
        }
        
        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        WebTarget target = client.target("http://localhost:" + port + "/BroadcasterApp/broadcaster");

        // setup the broadcaster
        target.request().post(Entity.text(""));

        CountDownLatch latch = new CountDownLatch(NUM_CLIENTS);
        List<ClientListener> clients = new ArrayList<>();
        try {
            for (int i = 0; i < NUM_CLIENTS; i++) {
                ClientListener clientListener = new ClientListener(target, latch);
                clients.add(clientListener);
                executor.submit(clientListener);
            }

            if (!latch.await(timeout, TimeUnit.SECONDS)) {                
                throw new RuntimeException(m + " timed out waiting for initial registration welcome with timeout of: " + timeout);
            }

            latch = new CountDownLatch(NUM_CLIENTS);
            for (ClientListener clientListener : clients) {
                List<String> events = clientListener.getReceivedEvents();
                assertTrue(events.size() == 0 || (events.size() == 1 && events.get(0).equals("Welcome")));
                events.clear();
                clientListener.setLatch(latch);
            }

            target.request().put(Entity.text("Event1"));

            if (!latch.await(timeout, TimeUnit.SECONDS)) {
                throw new RuntimeException(m + " timed out waiting for first broadcasted event with timeout of: " + timeout);
            }                    

            latch = new CountDownLatch(NUM_CLIENTS);
            for (ClientListener clientListener : clients) {                
                List<String> events = clientListener.getReceivedEvents();
                assertTrue(events.size() == 1 && events.get(0).equals("Event1"));
                events.clear();
                clientListener.setLatch(latch);
            }

            target.request().put(Entity.text("Event2"));

            if (!latch.await(timeout, TimeUnit.SECONDS)) {                
                throw new RuntimeException(m + " timed out waiting for second broadcasted event with timeout of: " + timeout);
            }            

            for (ClientListener clientListener : clients) {                
                List<String> events = clientListener.getReceivedEvents();
                assertTrue(events.size() == 1 && events.get(0).equals("Event2"));
                events.clear();
            }
        } finally {
            target.request().delete();
            for (ClientListener clientListener : clients) {
                clientListener.close();
            }
            while (latch.getCount() > 0) {
                latch.countDown();
            }           
            client.close();
        }
    }

//    private void blah() {
//        final List<String> receivedEvents = new ArrayList<String>();
//        final CountDownLatch executionLatch = new CountDownLatch(1);
//
//        try (SseEventSource source = SseEventSource.target(target).build()) {
//            System.out.println("client invoking server SSE resource on: " + source);
//            source.register(
//                            new Consumer<InboundSseEvent>() { // event
//
//                                @Override
//                                public void accept(InboundSseEvent t) {
//                                    System.out.println("new plain event: " + t.getId() + " " + t.getName() + " " + t.readData());
//                                    receivedEvents.add(t.readData(String.class));
//                                }
//                            },
//                            new Consumer<Throwable>() {
//
//                                @Override
//                                public void accept(Throwable t) {
//                                    t.printStackTrace();
//                                    fail("Caught unexpected exception: " + t);
//                                }
//                            },
//                            new Runnable() {
//
//                                @Override
//                                public void run() {
//                                    System.out.println("completion runnable executed");
//                                    executionLatch.countDown();
//                                }
//                            });
//
//            source.open();
//            System.out.println("client source open");
//            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));
//
//        } catch (InterruptedException e) {
//            // falls through
//            e.printStackTrace();
//        }
//
//        assertEquals("Received an unexpected number of events", 3, receivedEvents.size());
//        assertEquals("Unexpected event or event out of order", "uno", receivedEvents.get(0));
//        assertEquals("Unexpected event or event out of order", "dos", receivedEvents.get(1));
//        assertEquals("Unexpected event or event out of order", "tres", receivedEvents.get(2));
//    }
//
//    public void testIntegerSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {
//
//        final List<Integer> receivedEvents = new ArrayList<Integer>();
//        final CountDownLatch executionLatch = new CountDownLatch(1);
//
//        Client client = ClientBuilder.newClient();
//        int port = req.getServerPort();
//        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/integer3");
//
//        try (SseEventSource source = SseEventSource.target(target).build()) {
//            System.out.println("client invoking server SSE resource on: " + source);
//            source.register(
//                            new Consumer<InboundSseEvent>() { // event
//
//                                @Override
//                                public void accept(InboundSseEvent t) {
//                                    System.out.println("new integer event: " + t.getId() + " " + t.getName() + " " + t.readData());
//                                    receivedEvents.add(t.readData(Integer.class));
//                                }
//                            },
//                            new Consumer<Throwable>() {
//
//                                @Override
//                                public void accept(Throwable t) {
//                                    t.printStackTrace();
//                                    fail("Caught unexpected exception: " + t);
//                                }
//                            },
//                            new Runnable() {
//
//                                @Override
//                                public void run() {
//                                    System.out.println("completion runnable executed");
//                                    executionLatch.countDown();
//                                }
//                            });
//
//            source.open();
//            System.out.println("client source open");
//            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));
//
//        } catch (InterruptedException e) {
//            // falls through
//            e.printStackTrace();
//        }
//
//        assertEquals("Received an unexpected number of events", 3, receivedEvents.size());
//        assertEquals("Unexpected event or event out of order", 1, receivedEvents.get(0).intValue());
//        assertEquals("Unexpected event or event out of order", 2, receivedEvents.get(1).intValue());
//        assertEquals("Unexpected event or event out of order", 3, receivedEvents.get(2).intValue());
//    }
//
//    public void testJsonSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {
//
//        final List<JsonObject> receivedEvents = new ArrayList<JsonObject>();
//        final CountDownLatch executionLatch = new CountDownLatch(1);
//
//        Client client = ClientBuilder.newClient();
//        int port = req.getServerPort();
//        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/json3");
//
//        SseEventSource source = SseEventSource.target(target).build();
//        try {
//            System.out.println("client invoking server SSE resource on: " + source);
//            source.register(
//                            new Consumer<InboundSseEvent>() { // event
//
//                                @Override
//                                public void accept(InboundSseEvent t) {
//                                    JsonObject o = t.readData(JsonObject.class, MediaType.APPLICATION_JSON_TYPE);
//                                    System.out.println("new json event: " + o);
//                                    receivedEvents.add(o);
//                                }
//                            },
//                            new Consumer<Throwable>() {
//
//                                @Override
//                                public void accept(Throwable t) {
//                                    t.printStackTrace();
//                                    fail("Caught unexpected exception: " + t);
//                                }
//                            },
//                            new Runnable() {
//
//                                @Override
//                                public void run() {
//                                    System.out.println("completion runnable executed");
//                                    executionLatch.countDown();
//                                }
//                            });
//
//            for (int i = 0; i < 3; i++) {
//                try {
//                    source.open();
//                    break;
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                    source.close();
//                    source = SseEventSource.target(target).build();
//                }
//            }
//
//            System.out.println("client source open");
//            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));
//
//        } catch (InterruptedException e) {
//            // falls through
//            e.printStackTrace();
//        } finally {
//            source.close();
//        }
//
//        assertEquals("Received an unexpected number of events", 3, receivedEvents.size());
//        assertEquals("Unexpected event or event out of order", JSON_OBJECTS[0], receivedEvents.get(0));
//        assertEquals("Unexpected event or event out of order", JSON_OBJECTS[1], receivedEvents.get(1));
//        assertEquals("Unexpected event or event out of order", JSON_OBJECTS[2], receivedEvents.get(2));
//    }
//
//    public void testJaxbSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {
//
//        final List<JaxbObject> receivedEvents = new ArrayList<JaxbObject>();
//        final CountDownLatch executionLatch = new CountDownLatch(1);
//
//        Client client = ClientBuilder.newClient();
//        int port = req.getServerPort();
//        System.out.println("port = " + port);
//        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/jaxb3");
//
//        SseEventSource source = SseEventSource.target(target).build();
//        try {
//            System.out.println("client invoking server SSE resource on: " + source);
//            source.register(
//                            new Consumer<InboundSseEvent>() { // event
//
//                                @Override
//                                public void accept(InboundSseEvent t) {
//                                    JaxbObject o = t.readData(JaxbObject.class, MediaType.APPLICATION_XML_TYPE);
//                                    System.out.println("new jaxb event: " + o);
//                                    receivedEvents.add(o);
//                                }
//                            },
//                            new Consumer<Throwable>() {
//
//                                @Override
//                                public void accept(Throwable t) {
//                                    t.printStackTrace();
//                                    fail("Caught unexpected exception: " + t);
//                                }
//                            },
//                            new Runnable() {
//
//                                @Override
//                                public void run() {
//                                    System.out.println("completion runnable executed");
//                                    executionLatch.countDown();
//                                }
//                            });
//
//            for (int i = 0; i < 3; i++) {
//                try {
//                    source.open();
//                    break;
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                    source.close();
//                    source = SseEventSource.target(target).build();
//                }
//            }
//
//            System.out.println("client source open");
//            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));
//
//        } catch (InterruptedException e) {
//            // falls through
//            e.printStackTrace();
//        } finally {
//            source.close();
//        }
//
//        assertEquals("Received an unexpected number of events", 3, receivedEvents.size());
//        assertEquals("Unexpected event or event out of order", JAXB_OBJECTS[0], receivedEvents.get(0));
//        assertEquals("Unexpected event or event out of order", JAXB_OBJECTS[1], receivedEvents.get(1));
//        assertEquals("Unexpected event or event out of order", JAXB_OBJECTS[2], receivedEvents.get(2));
//    }
//
//    public void testSseWithRX(HttpServletRequest req, HttpServletResponse resp) throws Exception {
//
//        String[] nameData = new String[] { "John", "Jacob", "Jingleheimer", "Schmidt" };
//        final List<String> receivedEvents = new ArrayList<String>();
//        final CountDownLatch executionLatch = new CountDownLatch(1);
//
//        Client client = ClientBuilder.newClient();
//        int port = req.getServerPort();
//        WebTarget postTarget = client.target("http://localhost:" + port + "/BasicSseApp/basic/postPort");
//        postTarget.request().post(Entity.xml(String.valueOf(port)));
//
//        WebTarget nameTarget = client.target("http://localhost:" + port + "/BasicSseApp/basic/postName");
//        for (int i = 0; i < nameData.length; i++) {
//            nameTarget.request().rx().post(Entity.xml(nameData[i]));
//        }
//
//        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/rx");
//        SseEventSource.Builder sseBuilder = SseEventSource.target(target);
//        SseEventSource source = sseBuilder.build();
//
//        try {
//            System.out.println("testSseWithRX:client invoking server SSE resource on: " + source);
//            source.register(
//                            new Consumer<InboundSseEvent>() { // event
//
//                                @Override
//                                public void accept(InboundSseEvent t) {
//                                    System.out.println("testSseWithRX: new event: " + t.readData());
//                                    receivedEvents.add(t.readData(String.class));
//                                }
//                            },
//                            new Consumer<Throwable>() {
//
//                                @Override
//                                public void accept(Throwable t) {
//                                    t.printStackTrace();
//                                    fail("Caught unexpected exception: " + t);
//                                }
//                            },
//                            new Runnable() {
//
//                                @Override
//                                public void run() {
//                                    System.out.println("testSseWithRX: completion runnable executed");
//                                    executionLatch.countDown();
//                                }
//                            });
//
//            source.open();
//            System.out.println("testSseWithRX: client source open");
//            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));
//
//        } catch (InterruptedException e) {
//            // falls through
//            e.printStackTrace();
//        } finally {
//            source.close();
//        }
//        assertEquals("Received an unexpected number of events", nameData.length, receivedEvents.size());
//        System.out.println("testSseWithRX: " + receivedEvents + " that's my name too");
//    }
//
//    public void testErrorSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {
//
//        final List<JaxbObject> receivedEvents = new ArrayList<JaxbObject>();
//        final CountDownLatch executionLatch = new CountDownLatch(1);
//
//        Client client = ClientBuilder.newClient();
//        int port = req.getServerPort();
//        System.out.println("port = " + port);
//        WebTarget target = client.target("http://localhost:" + port + "/BasicSseApp/basic/error");
//
//        SseEventSource source = SseEventSource.target(target).build();
//        try {
//            System.out.println("client invoking server SSE resource on: " + source);
//            source.register(
//                            new Consumer<InboundSseEvent>() { // event
//
//                                @Override
//                                public void accept(InboundSseEvent t) {
//                                    JaxbObject o = t.readData(JaxbObject.class, MediaType.APPLICATION_XML_TYPE);
//                                    System.out.println("new error event: " + o);
//                                    receivedEvents.add(o);
//                                }
//                            },
//                            new Consumer<Throwable>() {
//
//                                @Override
//                                public void accept(Throwable t) {
//                                    t.printStackTrace();
//                                    fail("Caught unexpected exception: " + t);
//                                }
//                            },
//                            new Runnable() {
//
//                                @Override
//                                public void run() {
//                                    System.out.println("completion runnable executed");
//                                    executionLatch.countDown();
//                                }
//                            });
//
//            try {
//                source.open();
//            } catch (Throwable t) {
//                t.printStackTrace();
//                source.close();
//                source = SseEventSource.target(target).build();
//            }
//
//            System.out.println("client source open");
//            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));
//
//        } catch (InterruptedException e) {
//            // falls through
//            e.printStackTrace();
//        } finally {
//            source.close();
//        }
//
//        assertEquals("Received an unexpected number of events", 0, receivedEvents.size());
//    }
}
