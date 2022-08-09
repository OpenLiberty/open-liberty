/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.microprofile.telemetry.internal_fat.apps.hotadd;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import componenttest.app.FATServlet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/")
@ApplicationScoped
public class Telemetry10Servlet extends FATServlet {
    
    @Inject
    Tracer tracer;

    @Inject
    Span span;

    public void spanTest() {
        // Generate a span
        span = this.tracer.spanBuilder("Start my use case").startSpan();
        span.addEvent("Event 0");
        // execute my use case - here we simulate a wait
        doWork();
        span.addEvent("Event 1");
        span.end();
    }

    private void doWork() {
        try {
          Thread.sleep(500);
          span.addEvent("Doing work");
        } catch (InterruptedException e) {
          //do the right thing here
        }
    }

    @Test
    public void testTracerNotNull(){
        assertNotNull(tracer);
    }

    @Test
    public void testSpanNotNull(){
        assertNotNull(span);
    }
    
    @Test
    public void testJAXRS(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        try {
            String url = "http://" + req.getServerName() + ':' + req.getServerPort() + "/TelemetryApp/rest/test";
            System.out.println("Making a PATCH request to URL: " + url);
            String result = client.target(url)
                            .request()
                            .method("PATCH", String.class);
            assertEquals("patch-success", result);
        } finally {
            client.close();
        }
    }

}