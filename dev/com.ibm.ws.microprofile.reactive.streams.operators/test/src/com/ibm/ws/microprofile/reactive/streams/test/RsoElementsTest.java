/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The initial set of unit test material was heavily derived from
 * tests at https://github.com/eclipse/microprofile-reactive
 * by James Roper.
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.streams.test;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.junit.Test;

import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class RsoElementsTest extends WASReactiveUT {

    @Test
    public void rxPublisherTest() {

        ReactiveStreamsEngine engine = getEngine();

        PublishProcessor<String> rxpub = PublishProcessor.create();
        PublisherBuilder<String> mpPub = ReactiveStreams.fromPublisher(rxpub);

        PublishProcessor<String> rxproc = PublishProcessor.create();
        ProcessorBuilder<String, String> mpProc = ReactiveStreams.fromProcessor(rxproc);

        TestSubscriber<String> rxSub = new TestSubscriber<>();
        SubscriberBuilder mpSub = ReactiveStreams.fromSubscriber(rxSub);

        CompletionStage result = mpPub.via(mpProc).to(mpSub).run(engine);

        rxpub.onNext("one");
        rxpub.onNext("two");
        rxpub.onNext("three");
        rxpub.onComplete();

        // try {
//            result.wait();
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }

    }

}
