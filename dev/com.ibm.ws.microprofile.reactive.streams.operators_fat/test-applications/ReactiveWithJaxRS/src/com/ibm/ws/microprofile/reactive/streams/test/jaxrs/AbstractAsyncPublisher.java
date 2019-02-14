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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public abstract class AbstractAsyncPublisher<T> implements Publisher<T> {

    @Resource
    private ManagedExecutorService executorService;

    private final List<AsyncSubscription<T>> subscriptions = new ArrayList<>();

    public void publish(T message) {
        for (AsyncSubscription<T> sub : subscriptions) {
            sub.queue(message);
        }
    }

    @PreDestroy
    public void shutdown() {
        for (AsyncSubscription<T> sub : subscriptions) {
            sub.cancel();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void subscribe(Subscriber<? super T> arg0) {
        System.out.println(this + " subscribe: " + arg0);
        AsyncSubscription<T> subscription = new AsyncSubscription<T>(arg0, executorService);
        this.subscriptions.add(subscription);
        arg0.onSubscribe(subscription);
    }

}
