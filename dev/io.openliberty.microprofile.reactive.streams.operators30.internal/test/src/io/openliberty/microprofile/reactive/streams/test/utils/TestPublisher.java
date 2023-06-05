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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.operators.multi.builders.CollectionBasedMulti;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 *
 */
public class TestPublisher<T> implements Publisher<T> {

    CollectionBasedMulti<T> multi;
    Publisher<T> rxPub;

    public TestPublisher(T... values) {
        multi = new CollectionBasedMulti<T>(values);
        rxPub = AdaptersToReactiveStreams.publisher(multi);
    }

    /** {@inheritDoc} */
    @Override
    public void subscribe(Subscriber<? super T> arg0) {
        rxPub.subscribe(arg0);
    }

}
