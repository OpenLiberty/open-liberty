/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test.jaxrs;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 */
public class AsyncSubscription<T> implements Subscription {

    BlockingQueue<T> queue = new LinkedBlockingQueue<T>();

    private final Subscriber<? super T> subscriber;

    private volatile boolean cancel = false;

    private final ExecutorService executor;

    /**
     * @param myPublisher
     */
    public AsyncSubscription(Subscriber<? super T> subscriber, ExecutorService executor) {
        this.subscriber = subscriber;
        this.executor = executor;
    }

    /**
     * @param value
     */
    public void queue(T value) {
        try {
            this.queue.put(value);
        } catch (InterruptedException e) {
            this.subscriber.onError(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        System.out.println("cancel");
        this.cancel = true;
    }

    /** {@inheritDoc} */
    @Override
    public void request(long arg0) {
        System.out.println(subscriber + " requested " + arg0);
        for (int i = 0; i < arg0; i++) {
            if (!cancel) {
                executor.execute(() -> {
                    try {
                        T value = queue.take();
                        if (!cancel) {
                            System.out.println(subscriber + " sending " + value);
                            subscriber.onNext(value);
                        }
                    } catch (InterruptedException e) {
                        if (!cancel) {
                            this.subscriber.onError(e);
                        }
                    }
                });
            }
        }
    }

}
