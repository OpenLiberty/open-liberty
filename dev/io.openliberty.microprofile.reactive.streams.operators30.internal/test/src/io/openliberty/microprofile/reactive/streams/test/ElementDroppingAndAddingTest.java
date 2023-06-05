/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * The initial set of unit test material was heavily derived from
 * tests at https://github.com/eclipse/microprofile-reactive
 * by James Roper.
 ******************************************************************************/

package io.openliberty.microprofile.reactive.streams.test;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.Test;

import io.openliberty.microprofile.reactive.streams.test.utils.TestSubscriber;

public class ElementDroppingAndAddingTest extends LibertyReactiveUT {

    /**
     * Test that the number of element that goes into a stream is not necessarily
     * the number that comes out
     *
     * @throws InterruptedException
     */
    @Test
    public void testDropping() throws InterruptedException {

        PublisherBuilder<Integer> data = ReactiveStreams.of(1, 2, 3, 4, 5);
        ProcessorBuilder<Integer, Integer> filter = ReactiveStreams.<Integer>builder().dropWhile(t -> t < 3);

        TestSubscriber<Integer> rxSub = new TestSubscriber<Integer>();
        data.via(filter).to(rxSub).run(getEngine());

        TestSubscriber<Integer> o = rxSub.await();
        o.assertComplete();
        o.assertNoErrors();
        o.assertResult(3, 4, 5);

    }

    /**
     * Test that we can splice streams together
     *
     * @throws InterruptedException
     */
    @Test
    public void testConcatFallback() throws InterruptedException {

        PublisherBuilder<Integer> stream1 = ReactiveStreams.of(1, 2, 3, 4, 5);
        PublisherBuilder<Integer> stream2 = ReactiveStreams.of(6, 7, 8, 9, 10);

        TestSubscriber<Integer> rxSub = new TestSubscriber<Integer>();

        ReactiveStreams.concat(stream1, stream2).to(rxSub).run(getEngine());
        TestSubscriber<Integer> o = rxSub.await();
        o.assertComplete();
        o.assertNoErrors();
        o.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    }
}
