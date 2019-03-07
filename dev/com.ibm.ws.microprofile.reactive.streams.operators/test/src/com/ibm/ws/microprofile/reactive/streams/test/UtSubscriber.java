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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 */
final class UtSubscriber implements Subscriber<String> {
    /** {@inheritDoc} */
    @Override
    public void onComplete() {
        System.out.println("onComplete");
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable t) {
        System.out.println(t);
    }

    /** {@inheritDoc} */
    @Override
    public void onNext(String t) {
        System.out.println(t);
    }

    /** {@inheritDoc} */
    @Override
    public void onSubscribe(Subscription s) {
        System.out.println(s);
    }
}