/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test;

import org.eclipse.microprofile.reactive.streams.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.ReactiveStreams;
import org.junit.Test;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Checks that users can implement the org.reactivestreams interfaces
 */
public class TestEclipseReactiveStreamsOperatorsReachability {

    public class SingletonPublisher implements Publisher<Integer> {

        private Subscriber<? super Integer> subscriber;

        /** {@inheritDoc} */
        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new SingletonSubscription());
        }

    };

    public class SingletonProcessor implements Processor<Integer, Integer> {

        /** {@inheritDoc} */
        @Override
        public void onComplete() {}

        /** {@inheritDoc} */
        @Override
        public void onError(Throwable arg0) {}

        /** {@inheritDoc} */
        @Override
        public void onNext(Integer arg0) {}

        /** {@inheritDoc} */
        @Override
        public void onSubscribe(Subscription arg0) {}

        /** {@inheritDoc} */
        @Override
        public void subscribe(Subscriber<? super Integer> arg0) {}

    };

    public class SingletonSubscriber implements Subscriber<Integer> {

        /** {@inheritDoc} */
        @Override
        public void onComplete() {}

        /** {@inheritDoc} */
        @Override
        public void onError(Throwable arg0) {}

        /** {@inheritDoc} */
        @Override
        public void onNext(Integer arg0) {}

        /** {@inheritDoc} */
        @Override
        public void onSubscribe(Subscription arg0) {}

    };

    public class SingletonSubscription implements Subscription {

        /** {@inheritDoc} */
        @Override
        public void cancel() {}

        /** {@inheritDoc} */
        @Override
        public void request(long arg0) {}

    };

    @Test
    public void testPublisherBuilder() {
        PublisherBuilder<Integer> pb = ReactiveStreams.of(1, 2, 3, 4);
        PublisherBuilder<Integer> odds = pb.dropWhile(i -> ((i % 2) == 0));
        //odds.run(getEngine());
    }
}
