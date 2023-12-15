/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.test.utils;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.SubscriberWithCompletionStage;
import org.eclipse.microprofile.reactive.streams.operators.spi.UnsupportedStageException;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.jakarta.streams.Engine;

/**
 * This is a simple subclass that merely records if its methods have been called
 * before delegating to the superclass
 */
public class TestReactiveStreamsEngine extends Engine {

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

    public boolean buildPublisherCalled() {
        return buildPublisherCalled;
    }

    public boolean buildSubscriberCalled() {
        return buildSubscriberCalled;
    }

    public boolean buildProcessorCalled() {
        return buildProcessorCalled;
    }

    public boolean buildCompletionCalled() {
        return buildCompletionCalled;
    }

}