/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
//Heavily inspired by https://github.com/ReactiveX/RxJava/blob/3.x/src/main/java/io/reactivex/rxjava3/subscribers/TestSubscriber.java
package io.openliberty.microprofile.reactive.streams.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

public class TestSubscriber<T> implements Subscriber<T> {

    CountDownLatch done = new CountDownLatch(1);
    AssertSubscriber<T> assertSubscriber = new AssertSubscriber<>(Long.MAX_VALUE, false);
    Subscriber<T> rxSub = AdaptersToReactiveStreams.subscriber(assertSubscriber);

    /** {@inheritDoc} */
    @Override
    public void onComplete() {
        System.out.println("onComplete");
        rxSub.onComplete();
        done.countDown();
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable arg0) {
        System.out.println("onError: " + arg0);
        rxSub.onError(arg0);
        done.countDown();
    }

    /** {@inheritDoc} */
    @Override
    public void onNext(T arg0) {
        System.out.println("onNext: " + arg0);
        rxSub.onNext(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public void onSubscribe(Subscription arg0) {
        System.out.println("onSubscribe: " + arg0);
        rxSub.onSubscribe(arg0);
    }

    public TestSubscriber<T> await() throws InterruptedException {
        boolean reallyDone = done.await(5, TimeUnit.SECONDS);//wait max 5 seconds because this is only a unit test
        if (!reallyDone) {
            throw new InterruptedException("TestSubscriber.await() timed out after 5 seconds");
        }
        return this;
    }

    public TestSubscriber<T> assertComplete() {
        assertSubscriber.assertCompleted();
        return this;
    }

    public TestSubscriber<T> assertNotComplete() {
        assertFalse(assertSubscriber.hasCompleted());
        return this;
    }

    public TestSubscriber<T> assertNoErrors() {
        assertNull(assertSubscriber.getFailure());
        return this;
    }

    public TestSubscriber<T> assertError(Class<? extends Throwable> exceptionClass) {
        assertSubscriber.assertFailedWith(exceptionClass);
        return this;
    }

    public TestSubscriber<T> assertResult(T... results) {
        assertSubscriber.assertItems(results);
        return this;
    }

    public TestSubscriber<T> assertValueAt(int position, int value) {
        assertEquals(value, assertSubscriber.getItems().get(position));
        return this;
    }

}
