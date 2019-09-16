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
package com.ibm.ws.microprofile.reactive.streams.test.context;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.ArrayList;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Dependent
public class IntegerSubscriber implements Subscriber<Integer> {

    private Subscription sub;
    private ArrayList<Integer> results = null;
    private boolean complete = false;
    private boolean alreadyUsed = false;

    @Inject
    Principal principle;

    private Throwable error;

    @Override
    public void onComplete() {
        System.out.println("onComplete");
        this.complete = true;
    }

    @Override
    public void onError(Throwable arg0) {
        System.out.println("onError: " + arg0);
        this.error = arg0;
        this.complete = true;
    }

    @Override
    public void onNext(Integer arg0) {
        System.out.println("Number received: " + arg0);
        assertTrue(arg0 >= 3);
        results.add(arg0);
        sub.request(1);
        assertNotNull("Subscriber Principle is null", principle);
    }

    @Override
    public void onSubscribe(Subscription arg0) {

        if (alreadyUsed) {
            throw new RuntimeException("Please don't reuse this instance");
        }
        alreadyUsed = true;
        sub = arg0;
        results = new ArrayList<Integer>();
        System.out.println("onSubscribe" + sub);
        startConsuming();
    }

    public void startConsuming() {
        sub.request(1);
    }

    public boolean isComplete() {
        return this.complete;
    }

    public Throwable getError() {
        return this.error;
    }

    public ArrayList<Integer> getResults() {
        return results;
    }

}
