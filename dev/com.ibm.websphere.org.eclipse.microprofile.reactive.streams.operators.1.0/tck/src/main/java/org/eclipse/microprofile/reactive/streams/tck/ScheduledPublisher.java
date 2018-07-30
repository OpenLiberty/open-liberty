/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.eclipse.microprofile.reactive.streams.tck;


import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;

/**
 * A publisher that publishes one element 100ms after being requested,
 * and then completes 100ms later. It also uses activePublishers to ensure
 * that it is the only publisher that is subscribed to at any one time.
 */
class ScheduledPublisher implements Publisher<Integer> {
    private final int id;
    private AtomicBoolean published = new AtomicBoolean(false);
    private final AtomicInteger activePublishers;
    private final Supplier<ScheduledExecutorService> supplier;

    ScheduledPublisher(int id, AtomicInteger activePublishers, Supplier<ScheduledExecutorService> supplier) {
        this.id = id;
        this.activePublishers = activePublishers;
        this.supplier = supplier;
    }

    @Override
    public void subscribe(Subscriber<? super Integer> subscriber) {
        assertEquals(activePublishers.incrementAndGet(), 1);
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                if (published.compareAndSet(false, true)) {
                    getExecutorService().schedule(() -> {
                        subscriber.onNext(id);
                        getExecutorService().schedule(() -> {
                            activePublishers.decrementAndGet();
                            subscriber.onComplete();
                        }, 100, TimeUnit.MILLISECONDS);
                    }, 100, TimeUnit.MILLISECONDS);
                }
            }

            @Override
            public void cancel() {
            }
        });
    }

    private ScheduledExecutorService getExecutorService() {
        return supplier.get();
    }
}
