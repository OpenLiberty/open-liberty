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
package org.apache.cxf.microprofile.client.sse;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.sse.InboundSseEvent;

import org.apache.cxf.common.util.SystemPropertyAction;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class SseSubscription implements Subscription {

    private static final int DEFAULT_BUFFER_SIZE = 
        SystemPropertyAction.getInteger("org.apache.cxf.microprofile.client.sse.bufferSize", 256);
    private final SsePublisher publisher;
    private final Subscriber<? super InboundSseEvent> subscriber;
    private final AtomicLong requested = new AtomicLong();
    private final AtomicLong delivered = new AtomicLong();
    private final AtomicBoolean completed = new AtomicBoolean();
    private final AtomicBoolean canceled = new AtomicBoolean();
    private final AtomicBoolean onCompleteCalled = new AtomicBoolean();
    //CHECKSTYLE:OFF
    private final LinkedList<InboundSseEvent> buffer = new LinkedList<>(); //NOPMD
    //CHECKSTYLE:ON
    private final AtomicInteger bufferSize = new AtomicInteger(DEFAULT_BUFFER_SIZE);

    SseSubscription(SsePublisher publisher, Subscriber<? super InboundSseEvent> subscriber) {
        this.publisher = publisher;
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        if (canceled.get()) {
            return;
        }
        if (n < 1) {
            fireError(new IllegalArgumentException("Only positive values may be requested - passed-in " + n));
            return;
        }
        requested.addAndGet(n);
        synchronized (buffer) {
            InboundSseEvent bufferedEvent = buffer.peekFirst();
            while (deliverIfCan(bufferedEvent)) {
                buffer.removeFirst();
                bufferedEvent = buffer.peekFirst();
            }
        }
        fireCompleteIfReady();
    }

    private boolean deliverIfCan(InboundSseEvent event) {
        if (event != null && delivered.get() < requested.get()) {
            subscriber.onNext(event);
            delivered.incrementAndGet();
            return true;
        }
        return false;
    }
    @Override
    public void cancel() {
        canceled.set(true);
        publisher.removeSubscription(this);
    }

    void fireSubscribe() {
        subscriber.onSubscribe(this);
    }

    void fireEvent(InboundSseEvent event) {
        if (completed.get() || canceled.get()) {
            return;
        }
        synchronized(buffer) {
            if (!deliverIfCan(event)) {
                buffer(event);
            }
        }

        fireCompleteIfReady();
    }

    void fireCompleteIfReady() {
        if (completed.get() && buffer.isEmpty() && onCompleteCalled.compareAndSet(false, true)) {
            subscriber.onComplete();
        }
    }

    void fireError(Throwable t) {
        if (completed.compareAndSet(false, true)) {
            subscriber.onError(t);
        }
    }

    void setBufferSize(int newSize) {
        bufferSize.set(newSize);
    }

    private void buffer(InboundSseEvent event) {
        synchronized (buffer) {
            buffer.addLast(event);
            if (buffer.size() > bufferSize.get()) {
                buffer.removeFirst();
            }
        }
    }

    static boolean isActive(SseSubscription subscription) {
        return !subscription.completed.get() && !subscription.canceled.get();
    }

    void complete() {
        completed.set(true);
        fireCompleteIfReady();
    }
}