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
package jaxrs21sse.clientbehavior;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SseClientBehaviorTestServlet")
public class SseClientBehaviorTestServlet extends FATServlet {
    private static Logger _log = Logger.getLogger(SseClientBehaviorTestServlet.class.getName());
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

    @Test
    public void testOneEventAndClose(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<String> receivedEvents = new ArrayList<String>();
        final List<Throwable> receivedThrowables = new ArrayList<>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        String uri = "http://localhost:" + port + "/SseClientBehaviorApp/clientBehavior/oneEventThenNormalClose";
        _log.info("testOneEventAndClose uri=" + uri);
        WebTarget target = client.target(uri);

        try (SseEventSource source = SseEventSource.target(target).build()) {
            _log.info("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    _log.info("new plain event: " + t.getId() + " " + t.getName() + " " + t.readData());
                                    receivedEvents.add(t.readData(String.class));
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    t.printStackTrace();
                                    receivedThrowables.add(t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    _log.info("completion runnable executed");
                                    executionLatch.countDown();
                                }
                            });

            source.open();
            _log.info("client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        }

        if (!receivedThrowables.isEmpty()) {
            fail("Error listener caught unexpected throwable: " + receivedThrowables.get(0));
        }
        assertEquals("Received an unexpected number of events", 1, receivedEvents.size());
        assertEquals("Unexpected event or event out of order", "foo", receivedEvents.get(0));
    }

    @Test
    public void testNonSseContentTypeResultsInError(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<String> receivedEvents = new ArrayList<String>();
        final List<Throwable> receivedThrowables = new ArrayList<>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        WebTarget target = client.target("http://localhost:" + port + "/SseClientBehaviorApp/clientBehavior/200NonSseContentType");

        try (SseEventSource source = SseEventSource.target(target).build()) {
            _log.info("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    _log.info("new plain event: " + t.getId() + " " + t.getName() + " " + t.readData());
                                    receivedEvents.add(t.readData(String.class));
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    t.printStackTrace();
                                    receivedThrowables.add(t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    _log.info("completion runnable executed");
                                    executionLatch.countDown();
                                }
                            });

            source.open();
            _log.info("client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        }

        if (!receivedEvents.isEmpty()) {
            fail("Event listener caught unexpected event: " + receivedEvents.get(0));
        }
        //TODO: uncomment once impl correctly throws an error in this case
        assertEquals("Received an unexpected number of errors", 1, receivedThrowables.size());
    }

    @Test
    public void testDoNotListenWhenResponseIs204(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        expectCompletionEventNoSseEvents("http://localhost:" + req.getServerPort() +
                                         "/SseClientBehaviorApp/clientBehavior/204", 0);
    }

    @Test
    public void testDoNotListenWhenResponseIs503NoRetryAfterHeader(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        expectCompletionEventNoSseEvents("http://localhost:" + req.getServerPort() +
                                         "/SseClientBehaviorApp/clientBehavior/503NoRetryAfter", 1);
    }

    @Test
    public void testDoNotListenWhenResponseIs503InvalidRetryAfterHeader(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        expectCompletionEventNoSseEvents("http://localhost:" + req.getServerPort() +
                                         "/SseClientBehaviorApp/clientBehavior/503InvalidRetryAfter", 1);
    }

    @Test
    public void testReconnectOn503WithValidRetryAfter(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<String> receivedEvents = new ArrayList<String>();
        final List<Throwable> receivedThrowables = new ArrayList<>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        WebTarget target = client.target("http://localhost:" + port + "/SseClientBehaviorApp/clientBehavior/503ValidRetryAfter");

        try (SseEventSource source = SseEventSource.target(target).reconnectingEvery(40, TimeUnit.SECONDS).build()) {
            long startTime = System.currentTimeMillis();
            _log.info("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    _log.info("new plain event: " + t.getId() + " " + t.getName() + " " + t.readData());
                                    receivedEvents.add(t.readData(String.class));
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    t.printStackTrace();
                                    receivedThrowables.add(t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    _log.info("completion runnable executed");
                                    _log.log(Level.WARNING, "ANDY completion runnable", new Throwable());
                                    executionLatch.countDown();
                                }
                            });

            source.open();
            _log.info("client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(20, TimeUnit.SECONDS));
            long elapsedTime = System.currentTimeMillis() - startTime;
            assertTrue("Did not reconnect soon enough - expected reconnect in 3 seconds, waited more than 30",
                       elapsedTime < 30000);

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        }

        if (!receivedThrowables.isEmpty()) {
            fail("Error listener caught unexpected throwable: " + receivedThrowables.get(0));
        }
        assertEquals("Received an unexpected number of events", 2, receivedEvents.size());
        assertEquals("Unexpected event or event out of order", "successAfterRetry1", receivedEvents.get(0));
        assertEquals("Unexpected event or event out of order", "successAfterRetry2", receivedEvents.get(1));
    }

    private void expectCompletionEventNoSseEvents(String url, int numExpectedErrors) {
        final List<String> receivedEvents = new ArrayList<>();
        final List<Throwable> receivedThrowables = new ArrayList<>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);

        try (SseEventSource source = SseEventSource.target(target).build()) {
            _log.info("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    _log.info("new plain event: " + t.getId() + " " + t.getName() + " " + t.readData());
                                    receivedEvents.add(t.readData(String.class));
                                }
                            },
                            new Consumer<Throwable>() {

                                @Override
                                public void accept(Throwable t) {
                                    t.printStackTrace();
                                    receivedThrowables.add(t);
                                }
                            },
                            new Runnable() {

                                @Override
                                public void run() {
                                    _log.info("completion runnable executed");
                                    executionLatch.countDown();
                                }
                            });

            source.open();
            _log.info("client source open");
            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        }

        assertEquals("Received an unexpected number of errors", numExpectedErrors, receivedThrowables.size());
        assertEquals("Received an unexpected number of events", 0, receivedEvents.size());
    }
}
