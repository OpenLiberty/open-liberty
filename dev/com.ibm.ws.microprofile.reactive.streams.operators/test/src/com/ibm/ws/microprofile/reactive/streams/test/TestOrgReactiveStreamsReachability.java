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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Checks that users can implement the org.reactivestreams interfaces
 */
public class TestOrgReactiveStreamsReachability {

    public abstract class MyPublisher implements Publisher<Integer> {

    };

    public abstract class MyProcessor implements Processor<Integer, Integer> {

    };

    public abstract class MySubscriber implements Subscriber<Integer> {

    };

    public abstract class MySubscription implements Subscription {

    };

    @Test
    public void loadTest() {
        /*
         * Actually if we couldn't load these classes then this whole class would not link/load.
         * We have this test just so it can pass if lots of others fail
         * to eliminate a testrun classpath problem from looking at the test report.
         */
        assertNotNull("Cannot load MyPublisher", MyPublisher.class);
        assertNotNull("Cannot load MyProcessor", MyProcessor.class);
        assertNotNull("Cannot load MySubscriber", MySubscriber.class);
        assertNotNull("Cannot load MySubscription", MySubscription.class);
    }

}
