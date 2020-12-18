/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient20.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class GenericSubscriber<T> implements Subscriber<T> {

    public boolean onCompleteCalled;
    public List<Throwable> onErrors = new ArrayList<>();
    public List<T> onNexts = new ArrayList<>();
    public List<Subscription> onSubscribes = new ArrayList<>();
    public CountDownLatch latch;
    
    public GenericSubscriber(int expectedElements) {
        latch = new CountDownLatch(expectedElements+1);
    }

    @Override
    public void onComplete() {
        onCompleteCalled = true;
        System.out.println("GenericSubscriber onComplete");
        latch.countDown();
    }

    @Override
    public void onError(Throwable t) {
        onErrors.add(t);
        System.out.println("GenericSubscriber onError " + t);
        latch.countDown();
    }

    @Override
    public void onNext(T t) {
        System.out.println("GenericSubscriber onNext " + t);
        onNexts.add(t);
        latch.countDown();
    }

    @Override
    public void onSubscribe(Subscription sub) {
        System.out.println("GenericSubscriber onSubscribe " + sub);
        onSubscribes.add(sub);
    }

    public void request(long num) {
        System.out.println("GenericSubscriber request " + num);
        onSubscribes.forEach(s -> s.request(num));
    }
}
