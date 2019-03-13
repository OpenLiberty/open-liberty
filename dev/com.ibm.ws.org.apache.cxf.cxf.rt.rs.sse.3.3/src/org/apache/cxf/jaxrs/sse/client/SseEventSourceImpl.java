/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.sse.client;

import java.lang.IllegalArgumentException;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Date;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;


import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * SSE Event Source implementation 
 */
public class SseEventSourceImpl implements SseEventSource {
    private static final Logger LOG = LogUtils.getL7dLogger(SseEventSourceImpl.class);
    
    private final WebTarget target;
    private final Collection<InboundSseEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicReference<SseSourceState> state = new AtomicReference<>(SseSourceState.CLOSED);
    
    // It may happen that open() and close() could be called on separate threads
    private volatile ScheduledExecutorService executor;
    private volatile boolean managedExecutor = true;
    private volatile InboundSseEventProcessor processor;
    // delay here is always in Milliseconds - conversion takes place in the ctor
    private volatile long delay;
    // Indicates the this SseEventSource has been opened.  It will remain true even if this is moved back to the connecting
    // state due to a scheduled reconnect.
    private volatile boolean isOpened;

    private class InboundSseEventListenerDelegate implements InboundSseEventListener {
        private String lastEventId;
        
        @Override
        public void onNext(InboundSseEvent event) {
            lastEventId = event.getId();
            listeners.forEach(listener -> listener.onNext(event));
            
            // Reconnect delay is set in milliseconds
            if (event.isReconnectDelaySet()) {
                delay = event.getReconnectDelay();
            }
        }

        @Override
        public void onError(Throwable ex) {
            listeners.forEach(listener -> listener.onError(ex));
            if (delay >= 0) {
                scheduleReconnect(delay, lastEventId);
            }
        }

        @Override
        public void onComplete() {
            listeners.forEach(InboundSseEventListener::onComplete);
            if (delay >= 0) {
                scheduleReconnect(delay, lastEventId);
            }
            //reset the delay and units 
            delay = -1;
        }
    }

    @Trivial
    private class InboundSseEventListenerImpl implements InboundSseEventListener {
        private final Consumer<InboundSseEvent> onEvent;
        private final Consumer<Throwable> onError;
        private final Runnable onComplete;
        
        InboundSseEventListenerImpl(Consumer<InboundSseEvent> e) {
            this(e, ex -> { }, () -> { });
        }
        
        InboundSseEventListenerImpl(Consumer<InboundSseEvent> e, Consumer<Throwable> t) {
            this(e, t, () -> { });    
        }

        InboundSseEventListenerImpl(Consumer<InboundSseEvent> e, Consumer<Throwable> t, Runnable c) {
            this.onEvent = e;
            this.onError = t;
            this.onComplete = c;
        }

        @Override
        public void onNext(InboundSseEvent event) {
            onEvent.accept(event);
        }

        @Override
        public void onError(Throwable ex) {
            onError.accept(ex);
        }

        @Override
        public void onComplete() {
            onComplete.run();
        }
    }
    
    /**
     * https://www.w3.org/TR/2012/WD-eventsource-20120426/#dom-eventsource-connecting
     */
    private enum SseSourceState {
        CONNECTING,
        OPEN,
        CLOSED
    }
    
    SseEventSourceImpl(WebTarget target, long delay, TimeUnit unit) {
        this.target = target;
        this.delay = TimeUnit.MILLISECONDS.convert(delay, unit);
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent) {
        listeners.add(new InboundSseEventListenerImpl(onEvent));
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError) {
        listeners.add(new InboundSseEventListenerImpl(onEvent, onError));
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError, Runnable onComplete) {
        listeners.add(new InboundSseEventListenerImpl(onEvent, onError, onComplete));
    }

    @Override
    public void open() {
        if (!state.compareAndSet(SseSourceState.CLOSED, SseSourceState.CONNECTING)) {
            throw new IllegalStateException("The SseEventSource is already in " + state.get() + " state");
        }

        // Create the executor for scheduling the reconnect tasks 
        final Configuration configuration = target.getConfiguration();
        if (executor == null) {
            executor = (ScheduledExecutorService)configuration
                .getProperty("scheduledExecutorService");
            
            if (executor == null) {
                executor = Executors.newSingleThreadScheduledExecutor();
                managedExecutor = false; /* we manage lifecycle */
            }
        }
        
        final Object lastEventId = configuration.getProperty(HttpHeaders.LAST_EVENT_ID_HEADER);
        connect(lastEventId != null ? lastEventId.toString() : null);
        // If a 503 was receieved during connect we might be in the "Connecting" state,  however
        // the isOpened flag will need to be set indicating that the eventsource has been opened
        // and not yet closed.
        isOpened = true;
    }

    @FFDCIgnore(Exception.class)
    private void connect(String lastEventId) {
        final InboundSseEventListenerDelegate delegate = new InboundSseEventListenerDelegate();
        Response response = null;

        try {
            Invocation.Builder builder = target.request(MediaType.SERVER_SENT_EVENTS);
            if (lastEventId != null) {
                builder.header(HttpHeaders.LAST_EVENT_ID_HEADER, lastEventId);
            }
            response = builder.get();

            // A client can be told to stop reconnecting using the HTTP 204 No Content 
            // response code. In this case, we should give up.
            if (response.getStatus() == 204) {
                LOG.fine("SSE endpoint " + target.getUri() + " returns no data, disconnecting");
                delegate.onComplete();
                state.set(SseSourceState.CLOSED);
                response.close();
                return;
            }

            // A client can be told to trigger a reconnect delay via a HTTP 503 Service Unavailable 
            // response code.
            if (response.getStatus() == 503) {
                LOG.fine("SSE endpoint " + target.getUri() + " returns 503");
                MultivaluedMap<String,Object> headerMap = response.getHeaders();
                //There should only be one header entry
                Object retryAfter = headerMap.getFirst(HttpHeaders.RETRY_AFTER);
                if (retryAfter != null) {
                    long retryAfterDelay = handleRetry((String)retryAfter);
                    delay = retryAfterDelay;
                    if (retryAfterDelay > -1) {
                        scheduleReconnect(retryAfterDelay, lastEventId);
                        response.close();
                        return;
                    }
                    
                }
            }

            int status = response.getStatus();
            String contentType = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
            if (status != 200 || !MediaType.SERVER_SENT_EVENTS.equals(contentType)) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "Received " + status + " Content-Type=" + contentType);
                }
                final Response fResponse = response;
                Throwable t;
                if (!MediaType.SERVER_SENT_EVENTS.equals(contentType)) {
                    t = new WebApplicationException("Unexpected Content-Type in response", response);
                } else {
                    t = AccessController.doPrivileged((PrivilegedExceptionAction<Throwable>)() -> {
                        Class<? extends Throwable> throwableClass = (Class<? extends Throwable>) 
                                        ExceptionUtils.getWebApplicationExceptionClass(fResponse, WebApplicationException.class);
                        Constructor<? extends Throwable> ctor;
                        try {
                            ctor = throwableClass.getConstructor(Response.class);
                        } catch (NoSuchMethodException ex) {
                            ctor = null;
                        }
                        return ctor == null ? throwableClass.newInstance() : ctor.newInstance(fResponse);
                    });
                }

                delegate.onError(t);
                delegate.onComplete();
                response.close();
                return;
            }
            // Should not happen but if close() was called from another thread, we could
            // end up there.
            if (state.get() == SseSourceState.CLOSED) {
                LOG.fine("SSE connection to " + target.getUri() + " has been closed already");
                response.close();
                return;
            }

            // Create new processor if this is the first time or the old one has been closed 
            if (processor == null || processor.isClosed()) {
                final Endpoint endpoint = WebClient.getConfig(target).getEndpoint();
                LOG.fine("Creating new instance of SSE event processor ...");
                processor = new InboundSseEventProcessor(endpoint, delegate);
            }

            // Start consuming events
            processor.run(response);
            LOG.fine("SSE event processor has been started ...");

            if (!state.compareAndSet(SseSourceState.CONNECTING, SseSourceState.OPEN)) {
                throw new IllegalStateException("The SseEventSource is already in " + state.get() + " state");
            }

            LOG.fine("Successfuly opened SSE connection to " + target.getUri());
        } catch (final Exception ex) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "caught exception in connect(...)", ex);
            }
            if (processor != null) {
                processor.close(1, TimeUnit.SECONDS);
                processor = null;
            }

            if (response != null) {
                response.close();
            }

            // We don't change the state here as the reconnection will be scheduled (if configured)
            LOG.fine("Failed to open SSE connection to " + target.getUri() + ". " + ex.getMessage());
            delegate.onError(ex);
        }
    }

    // return the milliseconds to delay before reconnecting; -1 means don't reconnect
    @FFDCIgnore({NumberFormatException.class, IllegalArgumentException.class, ParseException.class})
    private long handleRetry(String retryValue) {


        // RETRY_AFTER is a String that can either correspond to seconds (long) 
        // or a HTTP-Date (which can be one of 7 variations)"
        if (!(retryValue.contains(":"))) {
            // Must be a long since all dates include ":"
            try {
                Long retryLong = Long.valueOf(retryValue);
                //The RETRY_AFTER value is in seconds so change units
                return TimeUnit.MILLISECONDS.convert(retryLong.longValue(), TimeUnit.SECONDS);
            } catch (NumberFormatException e) {
                LOG.fine("SSE RETRY_AFTER Incorrect time value: " + e);
            }
        } else {
            char[] retryValueArray = retryValue.toCharArray();
            //handle date
            try {
                SimpleDateFormat sdf = null;
                // Determine the appropriate HTTP-Date pattern
                if (retryValueArray[3] == ',') {
                    sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"); // RTC 822, updated by RFC 1123
                } else if (retryValueArray[6] == ',') {
                    sdf = new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss z"); // RFC 850, obsoleted by RFC 1036
                } else if (retryValueArray[7] == ',') {
                    sdf = new SimpleDateFormat("EEEEEEE, dd-MMM-yy HH:mm:ss z"); // RFC 850, obsoleted by RFC 1036
                } else if (retryValueArray[8] == ',') {
                    sdf = new SimpleDateFormat("EEEEEEEE, dd-MMM-yy HH:mm:ss z"); // RFC 850, obsoleted by RFC 1036
                } else if (retryValueArray[9] == ',') {
                    sdf = new SimpleDateFormat("EEEEEEEEE, dd-MMM-yy HH:mm:ss z"); // RFC 850, obsoleted by RFC 1036
                } else if (retryValueArray[8] == ',') {
                    sdf = new SimpleDateFormat("EEEEEEEE, dd-MMM-yy HH:mm:ss z"); // RFC 850, obsoleted by RFC 1036
                } else if (retryValueArray[8] == ' ') {
                    sdf = new SimpleDateFormat("EEE MMM  d HH:mm:ss yyyy");  // ANSI C's asctime() format
                } else {
                    sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy"); // ANSI C's asctime() format
                }
                Date retryDate = sdf.parse(retryValue);

                long retryTime = retryDate.getTime();
                long now = System.currentTimeMillis();
                long delayTime = retryTime - now;
                if (delayTime > 0) {
                    return delayTime;//HTTP Date is in milliseconds
                }
                LOG.fine("SSE RETRY_AFTER Date value represents a time already past");
            } catch (IllegalArgumentException ex) {
                LOG.fine("SSE RETRY_AFTER Date value format incorrect:  " + ex);
            } catch (ParseException e2) {
                LOG.fine("SSE RETRY_AFTER Date value cannot be parsed: " + e2);
            }
        }
        return -1L;

    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public boolean close(long timeout, TimeUnit tunit) {
        isOpened = false;
        if (state.get() == SseSourceState.CLOSED) {
            return true;
        }
        
        if (state.compareAndSet(SseSourceState.CONNECTING, SseSourceState.CLOSED)) {
            LOG.fine("The SseEventSource was not connected, closing anyway");
        } else if (!state.compareAndSet(SseSourceState.OPEN, SseSourceState.CLOSED)) {
            throw new IllegalStateException("The SseEventSource is not opened, but in " + state.get() + " state");
        }
        
        if (executor != null && !managedExecutor) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                executor.shutdownNow();
                return null;
            });
            
            executor = null;
            managedExecutor = true;
        }

        // Should never happen
        if (processor == null) {
            return true;
        }
        
        return processor.close(timeout, tunit); 
    }

    private void scheduleReconnect(long tdelay, String lastEventId) {
        // If delay == RECONNECT_NOT_SET, no reconnection attempt should be performed
        if (tdelay < 0 || executor == null) {
            return;
        }
        
        // If the event source is already closed, do nothing
        if (state.get() == SseSourceState.CLOSED) {
            return;
        }
        
        // If the connection was still on connecting state, just try to reconnect
        if (state.get() != SseSourceState.CONNECTING) { 
            LOG.fine("The SseEventSource is still opened, moving it to connecting state");
            if (!state.compareAndSet(SseSourceState.OPEN, SseSourceState.CONNECTING)) {
                throw new IllegalStateException("The SseEventSource is not opened, but in " + state.get()
                    + " state, unable to reconnect");
            }
        }
                
        executor.schedule(() -> {
            // If we are still in connecting state (not closed/open), let's try to reconnect
            if (state.get() == SseSourceState.CONNECTING) {
                LOG.fine("Reestablishing SSE connection to " + target.getUri());
                connect(lastEventId);
            }
        }, tdelay, TimeUnit.MILLISECONDS);
        
        LOG.fine("The reconnection attempt to " + target.getUri() + " is scheduled in "
            + tdelay + "ms");
    }

    @Override
    public String toString() {
        return (this.getClass().getName() +
                "|target=" + target +
                "|listeners=" + listeners +
                "|state=" + state +
                "|executor=" + executor +
                "|managedExecutor=" + managedExecutor +
                "|processor = " + processor +
                "|delay=" + delay +
                "|isOpened=" + isOpened);

    }
}
