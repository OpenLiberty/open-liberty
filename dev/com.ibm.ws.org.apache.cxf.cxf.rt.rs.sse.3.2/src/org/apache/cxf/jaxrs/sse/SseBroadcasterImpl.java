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
package org.apache.cxf.jaxrs.sse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

public class SseBroadcasterImpl implements SseBroadcaster {
    private final Set<SseEventSink> subscribers = new CopyOnWriteArraySet<>();

    private final Set<Consumer<SseEventSink>> closers =
            new CopyOnWriteArraySet<>();

    private final Set<BiConsumer<SseEventSink, Throwable>> exceptioners =
            new CopyOnWriteArraySet<>();
    
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    @Override
    public void register(SseEventSink sink) {
        assertNotClosed();
        
        subscribers.add(sink);
    }

    @Override
    public CompletionStage<?> broadcast(OutboundSseEvent event) {
        assertNotClosed();

        
        final Collection<CompletableFuture<?>> futures = new ArrayList<>();
        subscribers.removeIf(sink -> {
            if (sink.isClosed()) {
                return true;
            }
            try {
                futures.add(sink.send(event).toCompletableFuture());
            } catch (final Exception ex) {
                exceptioners.forEach(exceptioner -> exceptioner.accept(sink, ex));
            }
            return false;
        });
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    @Override
    public void onClose(Consumer<SseEventSink> subscriber) {
        assertNotClosed();
        closers.add(subscriber);
    }

    @Override
    public void onError(BiConsumer<SseEventSink, Throwable> exceptioner) {
        assertNotClosed();
        exceptioners.add(exceptioner);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true))
        {
            subscribers.forEach(subscriber -> {
                subscriber.close();
                closers.forEach(closer -> closer.accept(subscriber));
            });
        }
    }
    
    private void assertNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("The SSE broadcaster is already closed");
        }
    }
}
