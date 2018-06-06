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
package jaxrs21sse.jaxb;

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
@WebServlet(urlPatterns = "/SseJaxbTestServlet")
public class SseJaxbTestServlet extends FATServlet {
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

    public void testJaxbSse(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        if (System.getSecurityManager() != null) {
            return; // skip test to avoid Java 2 security issues in JDK XML/JAXB code.
        }

        final List<TestXML> receivedEvents = new ArrayList<TestXML>();
        final CountDownLatch executionLatch = new CountDownLatch(1);

        Client client = ClientBuilder.newClient();
        int port = req.getServerPort();
        WebTarget target = client.target("http://localhost:" + port + "/SseJaxbApp/jaxb/jaxb1");

        SseEventSource source = SseEventSource.target(target).build();
        try {
            System.out.println("client invoking server SSE resource on: " + source);
            source.register(
                            new Consumer<InboundSseEvent>() { // event

                                @Override
                                public void accept(InboundSseEvent t) {
                                    TestXML o = t.readData(TestXML.class, MediaType.APPLICATION_XML_TYPE);
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

            try {
                source.open();
            } catch (Throwable t) {
                t.printStackTrace();
                source.close();
                source = SseEventSource.target(target).build();
            }

            assertTrue("Completion listener runnable was not executed", executionLatch.await(30, TimeUnit.SECONDS));

        } catch (InterruptedException e) {
            // falls through
            e.printStackTrace();
        } finally {
            source.close();
        }

        assertTrue("Received an unexpected number of events", receivedEvents.size() == 1);
        assertTrue("Unexpected event", receivedEvents.get(0).x == 10);
        assertTrue("Unexpected event", receivedEvents.get(0).y == 20);
    }

}
