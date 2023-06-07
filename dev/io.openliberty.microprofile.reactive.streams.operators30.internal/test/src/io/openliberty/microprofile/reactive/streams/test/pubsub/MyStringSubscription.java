/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.test.pubsub;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A subscription that queues values, passing them on to the subscriber when requested
 */
public class MyStringSubscription implements Subscription {

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    private final Subscriber<? super String> subscriber;
    private boolean quiesce = false;
    private long outstandingRequests = 0;

    public MyStringSubscription(Subscriber<? super String> subscriber) {
        this.subscriber = subscriber;
    }

    /**
     * Queue a new value. If there are outstanding requests from the subscriber, deliver it straight away
     *
     * @param value the value to queue
     */
    public void queue(String value) {
        try {
            this.queue.put(value);
            if (outstandingRequests > 0) {
                deliverNext();
            }
        } catch (InterruptedException e) {
            this.subscriber.onError(e);
        }
    }

    /**
     * For the purposes of unit test, this is not really implemented properly
     */
    @Override
    public void cancel() {
        System.out.println("cancel");
    }

    /**
     * A request for a certain number of values to be delivered to the subscriber.
     *
     * If there are not enough currently queued values then the outstanding requests will be recorded.
     */
    @Override
    public void request(long numberOfRequests) {

        long free = Long.MAX_VALUE - outstandingRequests;
        if (numberOfRequests > free) {
            outstandingRequests = Long.MAX_VALUE;
        } else {
            outstandingRequests = outstandingRequests + numberOfRequests;
        }

        while (!queue.isEmpty() && outstandingRequests > 0) {
            deliverNext();
        }

        if (quiesce && queue.isEmpty()) {
            subscriber.onComplete();
        }
    }

    /**
     * Deliver the next value to the subscriber, if one was requested and if there is a value queued
     */
    private void deliverNext() {
        if (!queue.isEmpty() && outstandingRequests > 0) {
            try {
                String value = queue.take();
                outstandingRequests--;
                subscriber.onNext(value);
            } catch (InterruptedException e) {
                this.subscriber.onError(e);
            }
        }
    }

    public synchronized void quiesce() {
        quiesce = true;
        if (queue.isEmpty()) {
            subscriber.onComplete();
        }
    }

    /**
     * @return true if quiesce was requested
     */
    public synchronized boolean isQuiesced() {
        return quiesce;
    }

}