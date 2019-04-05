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
package com.ibm.ws.microprofile.reactive.streams.test.basic;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import javax.enterprise.context.Dependent;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Dependent
public class IntegerSubscriber implements Subscriber<Integer> {

    private Subscription sub;
    private final ArrayList<Integer> results = new ArrayList<Integer>();
    private boolean complete = false;
    private boolean alreadyUsed = false;

    @Override
    public void onComplete() {
        System.out.println("onComplete");
        this.complete = true;
    }

    @Override
    public void onError(Throwable arg0) {
        System.out.println("onError: " + arg0);
        this.complete = true;
    }

    @Override
    public void onNext(Integer arg0) {
        System.out.println("onNext: " + arg0);
        assertTrue(arg0 >= 3);
        results.add(arg0);
        sub.request(1);
    }

    @Override
    public void onSubscribe(Subscription sub_parm) {

        System.out.println("onSubscribe" + sub_parm);

        if (alreadyUsed) {
            throw new RuntimeException("Please don't reuse this instance");
        }

        alreadyUsed = true;

        this.sub = sub_parm;

        startConsuming();
    }

    public void startConsuming() {
        sub.request(1);
    }

    public boolean isComplete() {
        System.out.println("isComplete");
        return this.complete;
    }

    public ArrayList<Integer> getResults() {
        System.out.println("results" + results.toArray().toString());
        return results;
    }

}
