/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import io.openliberty.microprofile.reactive.streams.test.utils.TestSubscriber;
import io.smallrye.mutiny.operators.multi.builders.CollectionBasedMulti;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 *
 */
public class RsoElementsTest extends AbstractReactiveUnitTest {

    /**
     * Tests we can import a reactive streams dot org Subscriber and uses a Mutiny
     * Subscriber to do it
     *
     * @throws InterruptedException
     */
    @Test
    public void testReactiveStreamsDotOrgSubscriber() throws InterruptedException {

        PublisherBuilder<Integer> data = ReactiveStreams.of(1, 2, 3, 4, 5);
        TestSubscriber<Integer> rxSub = new TestSubscriber<Integer>();
        data.to(rxSub).run(getEngine());
        rxSub.await()
                        .assertComplete()
                        .assertNoErrors()
                        .assertResult(1, 2, 3, 4, 5);

    }

    /**
     * Tests we can import a reactive streams dot org Publisher and uses a Mutiny
     * Publisher to do it
     *
     * @throws InterruptedException
     */
    @Test
    public void testReactiveStreamsDotOrgPublisher() throws InterruptedException {

        CollectionBasedMulti<String> multi = new CollectionBasedMulti<String>("one", "two", "three");
        Publisher<String> rxPub = AdaptersToReactiveStreams.publisher(multi);

        PublisherBuilder<String> data = ReactiveStreams.fromPublisher(rxPub);

        TestSubscriber<String> rxSub = new TestSubscriber<String>();
        data.to(rxSub).run(getEngine());
        rxSub.await()
                        .assertComplete()
                        .assertNoErrors()
                        .assertResult("one", "two", "three");

    }

    /**
     * Tests we can import a reactive streams dot org Processor and uses a Mutiny
     * Processor to do it
     *
     * @throws InterruptedException
     */
    @Test
    public void testReactiveStreamsDotOrgProcessor() throws InterruptedException {

        PublisherBuilder<Integer> data = ReactiveStreams.of(1, 2, 3, 4, 5);

        UnicastProcessor<Integer> uniProcessor = UnicastProcessor.create();
        Processor<Integer, Integer> rxProcessor = AdaptersToReactiveStreams.processor(uniProcessor);

        ProcessorBuilder<Integer, Integer> rxProcBuilder = ReactiveStreams.fromProcessor(rxProcessor);
        TestSubscriber<Integer> rxSub = new TestSubscriber<Integer>();

        data.via(rxProcBuilder).to(rxSub).run(getEngine());
        rxSub.await()
                        .assertComplete()
                        .assertNoErrors()
                        .assertResult(1, 2, 3, 4, 5);

    }

}
