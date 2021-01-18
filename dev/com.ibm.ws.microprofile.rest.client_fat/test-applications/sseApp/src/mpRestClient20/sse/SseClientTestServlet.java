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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SseClientTestServlet")
public class SseClientTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(SseClientTestServlet.class.getName());
    
    private static final int WAIT_TIME = 20; // in seconds

    private RestClientBuilder builder;

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    @Override
    public void init() throws ServletException {
        String baseUrlStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default") + "/sseApp/app";
        LOG.info("baseUrl = " + baseUrlStr);
        URL baseUrl;
        try {
            baseUrl = new URL(baseUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .executorService(Executors.newFixedThreadPool(4))
                        .baseUrl(baseUrl);
    }

    @Test
    public void testPublisherInboundSseEvent(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try (SseClient client = builder.build(SseClient.class)) {
            GenericSubscriber<InboundSseEvent> subscriber = new GenericSubscriber<>(3);
            client.anySse("send3strings").subscribe(subscriber);
            subscriber.request(3);
            assertTrue("Timed out waiting for all events", subscriber.latch.await(WAIT_TIME, TimeUnit.SECONDS));
            assertTrue("onComplete not called", subscriber.onCompleteCalled);
            assertEquals("Unexpected errors encounter", 0, subscriber.onErrors.size());
            assertEquals("Unepxected number of onNext calls", 3, subscriber.onNexts.size());
            List<String> strings = subscriber.onNexts.stream()
                                                     .map(o -> ((InboundSseEvent)o).readData(String.class))
                                                     .collect(Collectors.toList());
            assertTrue(strings.contains("foo") && strings.contains("bar") && strings.contains("baz"));
        }
    }

    @Test
    public void testPublisherString(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try (SseClient client = builder.build(SseClient.class)) {
            GenericSubscriber<String> subscriber = new GenericSubscriber<>(3);
            client.send3strings().subscribe(subscriber);
            subscriber.request(3);
            assertTrue("Timed out waiting for all events", subscriber.latch.await(WAIT_TIME, TimeUnit.SECONDS));
            assertTrue("onComplete not called", subscriber.onCompleteCalled);
            assertEquals("Unexpected errors encounter", 0, subscriber.onErrors.size());
            assertEquals("Unepxected number of onNext calls", 3, subscriber.onNexts.size());
            List<String> strings = subscriber.onNexts;
            assertTrue(strings.contains("foo") && strings.contains("bar") && strings.contains("baz"));
        }
    }

    @Test
    public void testPublisherInteger(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try (SseClient client = builder.build(SseClient.class)) {
            GenericSubscriber<Integer> subscriber = new GenericSubscriber<>(3);
            client.send3ints(6).subscribe(subscriber);
            subscriber.request(3);
            assertTrue("Timed out waiting for all events", subscriber.latch.await(WAIT_TIME, TimeUnit.SECONDS));
            assertTrue("onComplete not called", subscriber.onCompleteCalled);
            assertEquals("Unexpected errors encounter", 0, subscriber.onErrors.size());
            assertEquals("Unepxected number of onNext calls", 3, subscriber.onNexts.size());
            List<Integer> ints = subscriber.onNexts;
            assertTrue(ints.contains(6) && ints.contains(7) && ints.contains(8));
        }
    }

    @Test
    public void testPublisherSomeObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try (SseClient client = builder.build(SseClient.class)) {
            GenericSubscriber<SomeObject> subscriber = new GenericSubscriber<>(7);
            client.send7objects().subscribe(subscriber);
            subscriber.request(7);
            assertTrue("Timed out waiting for all events", subscriber.latch.await(WAIT_TIME, TimeUnit.SECONDS));
            assertTrue("onComplete not called", subscriber.onCompleteCalled);
            assertEquals("Unexpected errors encounter", 0, subscriber.onErrors.size());
            assertEquals("Unepxected number of onNext calls", 7, subscriber.onNexts.size());
            for (SomeObject o : subscriber.onNexts) {
                System.out.println(o);
            }
        }
    }
}