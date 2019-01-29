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
package com.ibm.ws.microprofile.reactive.streams.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 */
public class IntegerSubscriber implements Subscriber<Integer> {

    private Subscription sub;
    private final ArrayList<Integer> results = new ArrayList<Integer>();
    private boolean complete = false;

    @Override
    public void onComplete() {
        System.out.println("onComplete");
        this.complete = true;
    }

    @Override
    public void onError(Throwable arg0) {
        System.out.println("onError");
    }

    @Override
    public void onNext(Integer arg0) {
        System.out.println("Number received: " + arg0);
        assertTrue(arg0 >= 3);
        results.add(arg0);
        sub.request(1);
    }

    @Override
    public void onSubscribe(Subscription arg0) {
        sub = arg0;
        System.out.println("onSubscribe" + sub);
    }

    public void startConsuming() {
        sub.request(1);
    }

    public boolean isComplete() {
        return this.complete;
    }

    public ArrayList<Integer> getResults() {
        return results;
    }

}
