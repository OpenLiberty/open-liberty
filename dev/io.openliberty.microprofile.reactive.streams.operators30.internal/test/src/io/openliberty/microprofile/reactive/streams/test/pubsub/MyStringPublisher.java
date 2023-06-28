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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * A very basic publisher that publishes a fixed set of values as soon as a subscriber subscribes.
 * It then quiesces straight away.
 */
public class MyStringPublisher implements Publisher<String> {

    public static final String[] VALUES = { "one", "two", "three" };

    private MyStringSubscription subscription;

    /** {@inheritDoc} */
    @Override
    public void subscribe(Subscriber<? super String> subscriber) {
        System.out.println("subscribe: " + subscriber);
        this.subscription = new MyStringSubscription(subscriber);
        for (int i = 0; i < VALUES.length; i++) {
            publish(VALUES[i]);
        }
        quiesce();

        subscriber.onSubscribe(this.subscription);
    }

    public synchronized void publish(String string) {
        if (subscription.isQuiesced()) {
            throw new UnsupportedOperationException("Attempt to publish after quiesce");
        }
        System.out.println("publish: " + string);
        subscription.queue(string);
    }

    public void quiesce() {
        subscription.quiesce();
    }
}