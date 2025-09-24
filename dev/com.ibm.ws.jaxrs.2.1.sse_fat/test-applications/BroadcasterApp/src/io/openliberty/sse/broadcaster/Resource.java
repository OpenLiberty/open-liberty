/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package io.openliberty.sse.broadcaster;

import java.lang.reflect.Field;
import java.lang.NoSuchFieldException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
    final static AtomicBoolean closeAfterRegister = new AtomicBoolean(true);
    final static AtomicInteger closedClients = new AtomicInteger();

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public boolean setup(@Context Sse sse) { //returns whether setup was necessary
        synchronized (Resource.class) {
            //Always create a new broadcaster instance.
            broadcaster = sse.newBroadcaster();
            _log.info("setup created new Broadcaster: " + broadcaster);
            return true;
        }
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

    @GET
    @Path("/numClosedClients")
    @Produces(MediaType.TEXT_PLAIN)
    public int getNumOfClosedClients() throws Exception {
        return closedClients.get();
    }
    @GET
    @Path("/numSinks")
    @Produces(MediaType.TEXT_PLAIN)
    public int getNumOfSinksInBroadcaster() throws Exception {
        //this method uses reflection to check the number of event sinks actively registered with the broadcaster
        // this type of action is unsupported and subject to change

        //Class<?> broadcasterImplClass = Class.forName("org.apache.cxf.jaxrs.sse.SseBroadcasterImpl");
        Class<?> broadcasterImplClass = broadcaster.getClass();
        _log.info("broadcasterImplClass " + broadcasterImplClass);
        
        // CXF and RestEasy have different fields and types in their versions of BroadcasterImpl.
        Field subscribersField = null;
        int size = 0;
        try {
            subscribersField = broadcasterImplClass.getDeclaredField("subscribers");
            subscribersField.setAccessible(true);
            Set<SseEventSink> registeredSinks = (Set<SseEventSink>) subscribersField.get(broadcaster);
            size = registeredSinks.size();            
        } catch (NoSuchFieldException e) {  //check EE9
            subscribersField = broadcasterImplClass.getDeclaredField("outputQueue");
            subscribersField.setAccessible(true);
            ConcurrentLinkedQueue<SseEventSink> registeredSinks = (ConcurrentLinkedQueue<SseEventSink>) subscribersField.get(broadcaster);
            size = registeredSinks.size();          
        }
        
        _log.info("getNumOfSinksInBroadcaster " + size);
        return size;
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/closedSinkTest")
    public void registerForClosedSinkTest(@Context Sse sse, @Context SseEventSink sink) {  
        register(sse, sink);

        if (doClose()) {
            try {
                //automatically close every other client sink
                _log.info("registerForClosedSinkTest - closing new sink: " + sink);
                sink.close();
                _log.info("registerForClosedSinkTest - closed new sink: " + sink);
            } finally {
                closedClients.incrementAndGet();
            }
        }
        
    }

    /**
     * Returns true every other time this method is called.
     *
     * @return whether to close the sink
     */
    private boolean doClose() {
        boolean current;
        do {
            current = closeAfterRegister.get();
        } while (!closeAfterRegister.compareAndSet(current, !current));
        return current;
        
    }
}
