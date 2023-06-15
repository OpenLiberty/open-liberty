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

import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A subscriber that simply records the values it is sent until it is complete (or error)
 */
public class MyStringSubscriber implements Subscriber<String> {

    private final List<String> messages = new ArrayList<String>();
    private Subscription subscription;
    private boolean complete = false;

    /**
     * @return
     */
    public boolean isComplete() {
        return complete;
    }

    /** {@inheritDoc} */
    @Override
    public void onComplete() {
        complete = true;
        System.out.println("onComplete");
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable throwable) {
        System.out.println("onError");
        complete = true;
        throwable.printStackTrace();
    }

    /** {@inheritDoc} */
    @Override
    public void onNext(String value) {
        System.out.println("onNext: " + value);
        this.messages.add(value);
    }

    /** {@inheritDoc} */
    @Override
    public void onSubscribe(Subscription subscription) {
        System.out.println("onSubscribe: " + subscription);
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    public List<String> getMessages() {
        return this.messages;
    }

    public void request(int count) {
        this.subscription.request(count);
    }
}