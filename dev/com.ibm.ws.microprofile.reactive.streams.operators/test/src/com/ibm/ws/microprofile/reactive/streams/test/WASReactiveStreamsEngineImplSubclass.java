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

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.operators.spi.SubscriberWithCompletionStage;
import org.eclipse.microprofile.reactive.streams.operators.spi.UnsupportedStageException;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import com.ibm.ws.microprofile.reactive.streams.operators.spi.impl.WASReactiveStreamsEngineImpl;

/**
 * This is a simple subclass that merely records if its methods have been called
 * before delegating to the superclass
 */
public class WASReactiveStreamsEngineImplSubclass
        extends WASReactiveStreamsEngineImpl implements ReactiveStreamsEngine {

    private boolean buildPublisherCalled;
    private boolean buildSubscriberCalled;
    private boolean buildProcessorCalled;
    private boolean buildCompletionCalled;

    /** {@inheritDoc} */
    @Override
    public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
        buildPublisherCalled = true;
        return super.buildPublisher(graph);
    }

    /** {@inheritDoc} */
    @Override
    public <T, R> SubscriberWithCompletionStage<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException {
        buildSubscriberCalled = true;
        return super.buildSubscriber(graph);
    }

    /** {@inheritDoc} */
    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
        buildProcessorCalled = true;
        return super.buildProcessor(graph);
    }

    /** {@inheritDoc} */
    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {
        buildCompletionCalled = true;
        return super.buildCompletion(graph);
    }

    /**
     * @return
     */
    String check() {
        String check = "BitString:" + buildPublisherCalled + buildSubscriberCalled + buildProcessorCalled +
                buildCompletionCalled;
        return check;
    }
}