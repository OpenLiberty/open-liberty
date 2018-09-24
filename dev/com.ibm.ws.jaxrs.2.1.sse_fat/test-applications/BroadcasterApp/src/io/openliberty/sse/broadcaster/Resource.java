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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

@ApplicationPath("/")
@Path("/broadcaster")
@Consumes(MediaType.TEXT_PLAIN)
public class Resource extends Application {
    private static final Logger _log = Logger.getLogger(Resource.class.getName());

    static SseBroadcaster broadcaster;
    final static AtomicInteger registeredClients = new AtomicInteger();

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public boolean setup(@Context Sse sse) { //returns whether setup was necessary
        synchronized (Resource.class) {
            if (broadcaster == null) {
                broadcaster = sse.newBroadcaster();
                _log.info("setup created new Broadcaster: " + broadcaster);
                return true;
            }
        }
        _log.info("setup Broadcaster previously created: " + broadcaster);
        return false;
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void register(@Context Sse sse, @Context SseEventSink sink) {
        _log.info("register - registering new sink: " + sink);
        try {
            broadcaster.register(sink);
            int numClients = registeredClients.incrementAndGet();
            _log.info("register - new sink registered(total " + numClients + ")");
            sink.send(sse.newEvent("Welcome"));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public Response broadcast(@Context Sse sse, String msg) {
        int numClients;
        synchronized (Resource.class) {
            numClients = registeredClients.get();
            broadcaster.broadcast(sse.newEvent(msg));
        }
        _log.info("broadcast - just sent new event to " + numClients + " clients with message: " + msg);
        return Response.ok("Broadcast \"" + msg + "\" to " + numClients + " clients").build();
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public boolean clear() {
        try {
            synchronized (Resource.class) {
                broadcaster.close();
                registeredClients.set(0);
                return true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }
}
