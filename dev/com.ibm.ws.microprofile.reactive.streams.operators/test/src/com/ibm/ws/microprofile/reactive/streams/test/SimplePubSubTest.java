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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 */
public class SimplePubSubTest extends AbstractReactiveUnitTest {

    @Test
    public void testPubSub() {
        MyPublisher<String> publisher = new MyPublisher<String>();
        MySubscriber<String> subscriber = new MySubscriber<String>();

        PublisherBuilder<String> pBuilder = ReactiveStreams.fromPublisher(publisher);
        SubscriberBuilder<String, Void> sBuilder = ReactiveStreams.fromSubscriber(subscriber);

        CompletionRunner<Void> runner = pBuilder.to(sBuilder);
        runner.run();

        publisher.publish("one");
        publisher.publish("two");
        publisher.publish("three");
    }

    private static class MyPublisher<T> implements Publisher<T> {

        private MySubscription<T> subscription;

        /** {@inheritDoc} */
        @Override
        public void subscribe(Subscriber<? super T> arg0) {
            System.out.println("subscribe: " + arg0);
            this.subscription = new MySubscription<T>(arg0);
            arg0.onSubscribe(this.subscription);
        }

        public void publish(T value) {
            System.out.println("publish: " + value);
            subscription.queue(value);
        }
    }

    private static class MySubscription<T> implements Subscription {

        BlockingQueue<T> queue = new LinkedBlockingQueue<T>();

        private final Subscriber<? super T> subscriber;

        /**
         * @param myPublisher
         */
        public MySubscription(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        /**
         * @param value
         */
        public void queue(T value) {
            try {
                this.queue.put(value);
            } catch (InterruptedException e) {
                this.subscriber.onError(e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void cancel() {
            System.out.println("cancel");
        }

        /** {@inheritDoc} */
        @Override
        public void request(long arg0) {
            for (int i = 0; i < arg0; i++) {
                try {
                    T value = queue.take();
                    subscriber.onNext(value);
                } catch (InterruptedException e) {
                    this.subscriber.onError(e);
                }
            }
        }

    }

    private static class MySubscriber<T> implements Subscriber<T> {

        /** {@inheritDoc} */
        @Override
        public void onComplete() {
            System.out.println("onComplete");
        }

        /** {@inheritDoc} */
        @Override
        public void onError(Throwable arg0) {
            System.out.println("onError");
            arg0.printStackTrace();
        }

        /** {@inheritDoc} */
        @Override
        public void onNext(T arg0) {
            System.out.println("onNext: " + arg0);
        }

        /** {@inheritDoc} */
        @Override
        public void onSubscribe(Subscription arg0) {
            System.out.println("onSubscribe: " + arg0);
        }

    }
}
