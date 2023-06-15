/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.test;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.junit.Test;

import io.openliberty.microprofile.reactive.streams.test.pubsub.MyStringPublisher;
import io.openliberty.microprofile.reactive.streams.test.pubsub.MyStringSubscriber;

/**
 *
 */
public class SimplePubSubTest extends AbstractReactiveUnitTest {

    /**
     * Test that a custom publisher and subscriber can be wired together
     */
    @Test
    public void testPubSub() {
        //create a publisher
        MyStringPublisher publisher = new MyStringPublisher();
        PublisherBuilder<String> pBuilder = ReactiveStreams.fromPublisher(publisher);

        //create a subscriber
        MyStringSubscriber subscriber = new MyStringSubscriber();
        SubscriberBuilder<String, Void> sBuilder = ReactiveStreams.fromSubscriber(subscriber);

        //create a runner to wire them together
        CompletionRunner<Void> runner = pBuilder.to(sBuilder);
        //run it
        runner.run();

        //wait for the subscriber to be complete
        int loops = 0;
        while (!subscriber.isComplete() && loops++ < 10 * 60 * 5) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //check that the messages received were the same as the ones sent
        List<String> messages = subscriber.getMessages();
        for (int i = 0; i < MyStringPublisher.VALUES.length; i++) {
            assertTrue(messages.contains(MyStringPublisher.VALUES[i]));
        }
    }
}
