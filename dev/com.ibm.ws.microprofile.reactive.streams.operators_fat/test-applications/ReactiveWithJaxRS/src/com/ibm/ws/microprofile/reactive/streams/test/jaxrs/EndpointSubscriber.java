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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * This subscriber is used by the JaxRS endpoint to retrieve
 * a set number of elements from the stream ( the count is passed in on the GET request)
 */
public class EndpointSubscriber<T> implements Subscriber<T> {

    BlockingQueue<T> published = new LinkedBlockingQueue<T>();

    private boolean complete = false;
    private Subscription subscription;

    /** {@inheritDoc} */
    @Override
    public void onComplete() {
        setComplete(true);
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable arg0) {
        setComplete(true);
    }

    /** {@inheritDoc} */
    @Override
    public void onNext(T arg0) {
        published.add(arg0);
        this.subscription.request(1);
    }

    /** {@inheritDoc} */
    @Override
    public void onSubscribe(Subscription sub) {
        this.subscription = sub;
        this.subscription.request(1);
    }

    /**
     * @param count
     * @return
     */
    public List<String> getResponse(int count) {
        List<String> response = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            try {
                response.add((String) published.poll(1, TimeUnit.MINUTES));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    /**
     * @return the complete
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * @param complete the complete to set
     */
    public void setComplete(boolean complete) {
        this.complete = complete;
    }

}
