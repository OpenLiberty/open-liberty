/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.test.utils;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

public class TestProcessor<T> implements Processor<T, T> {

    UnicastProcessor<T> uniProcessor = UnicastProcessor.create();
    Processor<T, T> rxProcessor = AdaptersToReactiveStreams.processor(uniProcessor);

    /** {@inheritDoc} */
    @Override
    public void onComplete() {
        rxProcessor.onComplete();
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable arg0) {
        rxProcessor.onError(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public void onNext(T arg0) {
        rxProcessor.onNext(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public void onSubscribe(Subscription arg0) {
        rxProcessor.onSubscribe(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public void subscribe(Subscriber<? super T> arg0) {
        rxProcessor.subscribe(arg0);
    }

}
