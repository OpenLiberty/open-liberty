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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class SimpleSubscriber implements Subscriber<String> {

    private Subscription subscription;

    private final List<String> messages = new ArrayList<>();

    /** {@inheritDoc} */
    @Override
    public void onComplete() {
        System.out.println("onComplete");
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable arg0) {
        System.out.println("onError: " + arg0);
        arg0.printStackTrace();
    }

    /** {@inheritDoc} */
    @Override
    public void onNext(String arg0) {
        System.out.println("MESSAGE: " + arg0);
        messages.add(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public void onSubscribe(Subscription arg0) {
        this.subscription = arg0;
    }

    public List<String> getMessages(int count) throws InterruptedException {
        this.subscription.request(count);
        while (messages.size() < count) {
            Thread.sleep(100);
        }
        return messages;
    }
}
