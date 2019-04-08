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
package com.ibm.ws.microprofile.reactive.streams.test.concurrent;

import java.util.ArrayList;

import javax.enterprise.context.Dependent;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A simple test subscriber that logs its results locally and is noisy on the log
 */
@Dependent
public class TestSubscriber implements Subscriber<Integer> {

    private Subscription sub;
    private final ArrayList<Integer> results = new ArrayList<Integer>();
    private boolean complete = false;
    private boolean error = false;
    private final boolean request1InOnSubscribe = true;
    private final String name;

    public TestSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void onComplete() {
        System.out.println("[" + Thread.currentThread().getName() + "]" + "onComplete");
        this.complete = true;
    }

    @Override
    public void onError(Throwable arg0) {
        System.out.println("[" + Thread.currentThread().getName() + "]" + "onError (" + arg0 + ")");
        this.error = true;
    }

    @Override
    public void onNext(Integer arg0) {
        System.out.println("[" + Thread.currentThread().getName() + "]" + "onNext (" + arg0 + ")");
        results.add(arg0);
        sub.request(1);
    }

    @Override
    public void onSubscribe(Subscription arg0) {
        this.sub = arg0;
        System.out.println("[" + Thread.currentThread().getName() + "]" + "onSubscribe (" + arg0 + ")");
        if (request1InOnSubscribe) {
            request(1);
        }
    }

    public void request(int i) {
        sub.request(i);
    }

    public boolean isComplete() {
        return this.complete;
    }

    public boolean isError() {
        return this.error;
    }

    public boolean isTerminated() {
        return this.error || this.complete;
    }

    public ArrayList<Integer> getResults() {
        return results;
    }

}
