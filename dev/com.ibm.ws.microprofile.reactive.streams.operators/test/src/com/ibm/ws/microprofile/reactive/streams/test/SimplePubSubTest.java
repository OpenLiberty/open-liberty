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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
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
        MySubscriber<String> subscriber = new MySubscriber<String>(publisher);

        PublisherBuilder<String> pBuilder = ReactiveStreams.fromPublisher(publisher);
        SubscriberBuilder<String, Void> sBuilder = ReactiveStreams.fromSubscriber(subscriber);

        CompletionRunner<Void> runner = pBuilder.to(sBuilder);
        CompletionStage<Void> result = runner.run();

        int loops = 0;

        while (!subscriber.isComplete() && loops++ < 10 * 60 * 5) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<String> messages = subscriber.getMessages();

        assertTrue(messages.contains("one"));
        assertTrue(messages.contains("two"));
        assertTrue(messages.contains("three"));
    }

    private static class MyPublisher<T> implements Publisher<T> {

        private MySubscription<T> subscription;

        /** {@inheritDoc} */
        @Override
        public void subscribe(Subscriber<? super T> arg0) {
            System.out.println("subscribe: " + arg0);
            this.subscription = new MySubscription<T>(arg0);
            publish("one");
            publish("two");
            publish("three");
            quiesce();

            arg0.onSubscribe(this.subscription);
        }

        public synchronized void publish(String string) {
            if (subscription.isQuiesced()) {
                throw new UnsupportedOperationException("Attempt to publish after quiesce");
            }
            System.out.println("publish: " + string);
            subscription.queue(string);
        }

        public void quiesce() {
            subscription.quiesce();
        }
    }

    private static class MySubscription<T> implements Subscription {

        BlockingQueue<T> queue = new LinkedBlockingQueue<T>();

        private final Subscriber<? super T> subscriber;

        private boolean quiesce = false;

        private int outstandingRequests = 0;

        /**
         * @param myPublisher
         */
        public MySubscription(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        /**
         * @param string
         */
        public void queue(String string) {
            try {
                if (outstandingRequests > 0) {
                    subscriber.onNext((T) string);
                    outstandingRequests--;
                } else {
                    this.queue.put((T) string);
                }
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
                    if (!queue.isEmpty()) {
                        T value = queue.take();
                        subscriber.onNext(value);
                    } else {
                        System.out.println("request on empty queue!");
                        outstandingRequests++;
                    }
                } catch (InterruptedException e) {
                    this.subscriber.onError(e);
                }
            }
            if (quiesce && queue.isEmpty()) {
                subscriber.onComplete();
            }

        }

        public synchronized void quiesce() {
            quiesce = true;
            if (queue.isEmpty()) {
                subscriber.onComplete();
            }
        }

        /**
         * @return the quiesce
         */
        public synchronized boolean isQuiesced() {
            return quiesce;
        }

    }

    private static class MySubscriber<T> implements Subscriber<T> {

        private final List<T> messages = new ArrayList<T>();
        private Subscription subscription;
        private final MyPublisher<String> publisher;
        private boolean complete = false;

        /**
         * @param publisher
         */
        public MySubscriber(MyPublisher<String> publisher) {
            this.publisher = publisher;
        }

        /**
         * @return
         */
        public boolean isComplete() {
            return complete;
        }

        /** {@inheritDoc} */
        @Override
        public void onComplete() {
            complete = true;
            System.out.println("onComplete");
        }

        /** {@inheritDoc} */
        @Override
        public void onError(Throwable arg0) {
            System.out.println("onError");
            complete = true;
            arg0.printStackTrace();
        }

        /** {@inheritDoc} */
        @Override
        public void onNext(T arg0) {
            System.out.println("onNext: " + arg0);
            this.messages.add(arg0);
            subscription.request(1);
        }

        /** {@inheritDoc} */
        @Override
        public void onSubscribe(Subscription arg0) {
            System.out.println("onSubscribe: " + arg0);
            this.subscription = arg0;
            subscription.request(1);
        }

        public List<T> getMessages() {
            return this.messages;
        }

        public void request(int count) {
            this.subscription.request(count);
        }
    }
}
