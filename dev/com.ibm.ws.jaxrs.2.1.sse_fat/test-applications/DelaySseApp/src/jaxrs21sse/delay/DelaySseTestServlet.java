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
package jaxrs21sse.delay;

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
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.junit.After;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DelaySseTestServlet")
public class DelaySseTestServlet extends FATServlet {
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

    public void testRetrySse(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final List<String> receivedEvents = new ArrayList<String>();
        final List<String> eventSourceTimes = new ArrayList<String>();
        final CountDownLatch executionLatch = new CountDownLatch(2);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        WebTarget target = client.target("http://localhost:" + port + "/DelaySseApp/delay/retry3");

        try (SseEventSource source = SseEventSource.target(target).build()) {
            System.out.println("DelaySseTestServlet:  client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    System.out.println("DelaySseResource:  new delay event: " + t.getId() + " " + t.getName() + " " + t.getReconnectDelay() + " " + t.readData());
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
                                    String sourceString = source.toString();
                                    int delayStringStart = sourceString.indexOf("delay=");
                                    String delayString = sourceString.substring(delayStringStart);
                                    int delaystart = delayString.indexOf("=") + 1;
                                    int delaystop = delayString.indexOf("|");
                                    eventSourceTimes.add(delayString.substring(delaystart, delaystop));
                                    executionLatch.countDown();
                                }
                            });

            source.open();
            System.out.println("DelaySseTestServlet:  client source open");

            boolean success = executionLatch.await(120, TimeUnit.SECONDS);
            if (!success) {
                for (String re : receivedEvents) {
                    int i = 0;
                    System.out.println("receivedEvent " + i + " : " + re);
                    i++;
                }
                for (String et : eventSourceTimes) {
                    int i = 0;
                    System.out.println("eventSourceTime " + i + " : " + et);
                    i++;
                }
            }
            assertTrue("Completion listener runnable was not executed", success);

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        }

        assertEquals("Received an unexpected number of events", 2, receivedEvents.size());
        assertEquals("Unexpected results", "Retry Test Successful", receivedEvents.get(0));
        assertEquals("Unexpected results", "Reset Test Successful", receivedEvents.get(1));
        assertEquals("Unexpected time results", "5000", eventSourceTimes.get(0));
    }

}
