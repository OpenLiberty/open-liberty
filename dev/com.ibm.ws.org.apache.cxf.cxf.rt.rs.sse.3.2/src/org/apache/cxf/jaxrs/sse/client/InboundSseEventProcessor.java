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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.client.ClientProviderFactory;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.sse.client.InboundSseEventImpl.Builder;
import org.apache.cxf.message.Message;

public class InboundSseEventProcessor {
    public static final String SERVER_SENT_EVENTS = "text/event-stream";
    public static final MediaType SERVER_SENT_EVENTS_TYPE = MediaType.valueOf(SERVER_SENT_EVENTS);

    private static final Logger LOG = LogUtils.getL7dLogger(InboundSseEventProcessor.class);
    private static final String COMMENT = ":";
    private static final String EVENT = "event:";
    private static final String ID = "id:";
    private static final String RETRY = "retry:";
    private static final String DATA = "data:";

    private final Endpoint endpoint;
    private final InboundSseEventListener listener;
    private final ExecutorService executor;
    
    private volatile boolean closed;
    
    protected InboundSseEventProcessor(Endpoint endpoint, InboundSseEventListener listener) {
        this.endpoint = endpoint;
        this.listener = listener;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }
    
    void run(final Response response) {
        if (closed) {
            throw new IllegalStateException("The SSE Event Processor is already closed");
        }
        
        final InputStream is = response.readEntity(InputStream.class);
        final ClientProviderFactory factory = ClientProviderFactory.getInstance(endpoint);
        
        Message message = null;
        if (response instanceof ResponseImpl) {
            message = ((ResponseImpl)response).getOutMessage();
        }
        
        executor.submit(process(response, is, factory, message));
    }
    
    private Callable<?> process(Response response, InputStream is, ClientProviderFactory factory, Message message) {
        return () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                InboundSseEventImpl.Builder builder = null;

                while (line != null && !Thread.interrupted() && !closed) {
                    if (StringUtils.isEmpty(line) && builder != null) { /* empty new line */
                        final InboundSseEvent event = builder.build(factory, message);
                        builder = null; /* reset the builder for next event */
                        listener.onNext(event);
                    } else {
                        // Parsing and interpreting event stream: 
                        // https://www.w3.org/TR/eventsource/#parsing-an-event-stream
                        if (line.startsWith(EVENT)) {
                            int beginIndex = findFirstNonSpacePosition(line, EVENT);
                            builder = getOrCreate(builder).name(line.substring(beginIndex));
                        } else if (line.startsWith(ID)) {
                            int beginIndex = findFirstNonSpacePosition(line, ID);
                            builder = getOrCreate(builder).id(line.substring(beginIndex));
                        } else if (line.startsWith(COMMENT)) {
                            int beginIndex = findFirstNonSpacePosition(line, COMMENT);
                            builder = getOrCreate(builder).comment(line.substring(beginIndex));
                        } else if (line.startsWith(RETRY)) {
                            int beginIndex = findFirstNonSpacePosition(line, RETRY);
                            builder = getOrCreate(builder).reconnectDelay(line.substring(beginIndex));
                        } else if (line.startsWith(DATA)) {
                            int beginIndex = findFirstNonSpacePosition(line, DATA);
                            builder = getOrCreate(builder).appendData(line.substring(beginIndex));
                        }
                    }
                    line = reader.readLine();
                }
                
                if (builder != null) {
                    listener.onNext(builder.build(factory, message));
                }

                // complete the stream
                listener.onComplete();
            } catch (final Exception ex) {
                listener.onError(ex);
            }

            if (response != null) {
                LOG.fine("Closing the response");
                response.close();
            }

            return null;
        };
    }
    
    boolean isClosed() {
        return closed;
    }
    
    boolean close(long timeout, TimeUnit unit) {
        try {
            closed = true;
            
            if (executor.isShutdown()) {
                return true;
            }
            
            AccessController.doPrivileged((PrivilegedAction<Void>)
                () -> { 
                    executor.shutdown();
                    return null;
                });
            return executor.awaitTermination(timeout, unit);
        } catch (final InterruptedException ex) {
            return false;
        }
    }
    
    /**
     * Create builder on-demand, without explicit event demarcation
     */
    private static Builder getOrCreate(final Builder builder) {
        return (builder == null) ? new InboundSseEventImpl.Builder() : builder;
    }
    
    /**
     * Remove only leading spaces from the line as per specification, space after 
     * the colon is optional.
     * 
     * The following stream fires two identical events:
     * 
     *   data:test
     *   data: test
     *   
     *   This is because the space after the colon is ignored if present.
     */
    private static int findFirstNonSpacePosition(final String str, final String prefix) {
        int beginIndex = prefix.length();
        
        for (; beginIndex < str.length(); ++beginIndex) {
            if (str.charAt(beginIndex) != ' ') {
                break;
            }
        }
        
        return beginIndex;
    }
}
