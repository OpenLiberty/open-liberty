/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 *
 */
package jaxrs21sse.delay;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

@ApplicationPath("/")
@Path("/delay")
public class DelaySseResource extends Application {
    private volatile static int retry = 0;
    private volatile static long startTime = 0;
    private volatile static long delayTime = 0;
    private static String returnMessage = null;

    @GET
    @Path("/retry2")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void send2retries(@Context SseEventSink eventSink, @Context Sse sse) {
        System.out.println("DelaySseResource:  In send2retries method.  Retry = " + retry);
        if (retry == 0) {
            retry = 1;
            startTime = System.currentTimeMillis();
            System.out.println("DelaySseResource:  Throwing 503-1");
            throw new WebApplicationException(Response.status(503).header(HttpHeaders.RETRY_AFTER, "3").build());
        } else if (retry == 1) {
            retry = 2;
            delayTime = System.currentTimeMillis() - startTime;
            if (!(delayTime >= 3000)) {
                returnMessage = "Test 1 failed.  Expected delay time 3000 ms, actual delay time:  " + delayTime;
                System.out.println("DelaySseResource:  Test 1 failed.  Expected delay time 3000 ms, actual delay time:  " + delayTime);
            }
            //reset startTime
            startTime = System.currentTimeMillis();
            //add 10 seconds
            long delayTime2 = startTime + 10000;
            //get the Date to send
            Date testDate = new Date(delayTime2);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            String sdfString = sdf.format(testDate);
            System.out.println("DelaySseResource:  Throwing 503-2:  sdfString = " + sdfString);
            throw new WebApplicationException(Response.status(503).header(HttpHeaders.RETRY_AFTER, sdfString).build());
        } else if (retry == 2) {
            delayTime = System.currentTimeMillis() - startTime;
            //Since HTTP dates are in seconds we need to allow for a slightly lower result
            if (!(delayTime >= 9000)) {
                returnMessage = "Test 2 failed.  Expected delay time (>=9000 ms), actual delay time:  " + delayTime;
                System.out.println("DelaySseResource:  Test 2 failed.  Expected delay time (>=9000 ms), actual delay time:  " + delayTime);
            } else {
                System.out.println("DelaySseResource:  Retry Test 2 Successful");
                returnMessage = "Retry Test Successful";
            }
            //reset startTime
            startTime = System.currentTimeMillis();

            try (SseEventSink s = eventSink) {
                System.out.println("DelaySseResource:  sending event");
                s.send(sse.newEventBuilder().data(returnMessage).build());
            }
            
            //reset
            retry = 0;
            returnMessage = null;
            startTime = 0;
            delayTime = 0;
            
            if (!eventSink.isClosed()) {
                System.out.println("DelaySseResource:  eventSink has autoclosed-1");
                DelaySseTestServlet.resourceFailures.add("AutoClose in DelaySseResource.send3retries failed for eventSink");
            }
        }
    }    
}
