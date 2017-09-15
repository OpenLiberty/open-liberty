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

import static jaxrs21sse.basic.DataObject.DATA_OBJECTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
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
                                    System.out.println("new event: " + t.getId() + " " + t.getName() + " " + t.readData());
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

    public void testJsonSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<DataObject> receivedEvents = new ArrayList<DataObject>();
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
                                    DataObject o = t.readData(DataObject.class, MediaType.APPLICATION_JSON_TYPE);
                                    System.out.println("new event: " + o);
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
        assertEquals("Unexpected event or event out of order", DATA_OBJECTS[0], receivedEvents.get(0));
        assertEquals("Unexpected event or event out of order", DATA_OBJECTS[1], receivedEvents.get(1));
        assertEquals("Unexpected event or event out of order", DATA_OBJECTS[2], receivedEvents.get(2));
    }
}
