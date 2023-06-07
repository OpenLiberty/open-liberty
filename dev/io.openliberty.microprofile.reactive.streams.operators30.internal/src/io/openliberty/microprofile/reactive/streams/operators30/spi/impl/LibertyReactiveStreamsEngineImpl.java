/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.operators30.spi.impl;

import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.eclipse.microprofile.reactive.streams.operators.spi.SubscriberWithCompletionStage;
import org.eclipse.microprofile.reactive.streams.operators.spi.UnsupportedStageException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import com.ibm.wsspi.threadcontext.WSContextService;

import io.smallrye.mutiny.jakarta.streams.Engine;

@Component(name = "io.openliberty.microprofile.reactive.streams.operators30.spi.impl.LibertyReactiveStreamsEngineImpl", service = { ReactiveStreamsEngine.class }, property = { "service.vendor=IBM" }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class LibertyReactiveStreamsEngineImpl extends Engine implements ReactiveStreamsEngine {

    private ExecutorService executorService;

    private WSContextService wsContextService;

    @Activate
    public void activate() {
        ReactiveStreamsFactoryResolver.setInstance(new ReactiveStreamsFactoryImpl());
        ReactiveStreamsEngineResolver.setInstance(this);
    }

    @Deactivate
    public void deactivate() {
        ReactiveStreamsEngineResolver.setInstance(null);
        ReactiveStreamsFactoryResolver.setInstance(null);
    }

    @Reference
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Reference
    public void setContextService(WSContextService wsContextService) {
        this.wsContextService = wsContextService;
    }

    /**
     * Get the executor service for getting threads from.
     *
     * @return the executor
     */
    public ExecutorService getExecutor() {
        return this.executorService;
    }

    /** {@inheritDoc} */
    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {

        PrivilegedAction<CompletionStage<T>> action = new PrivilegedAction<CompletionStage<T>>() {
            @Override
            public CompletionStage<T> run() {
                return LibertyReactiveStreamsEngineImpl.super.buildCompletion(graph);
            }
        };

        StreamRunner<T> runner = new StreamRunner<>(getExecutor(), this.wsContextService, action);

        StreamTask<T> streamTask = runner.startStream();

        CompletableFuture<T> wrapper = streamTask.getWrapperCompletableFuture();

        return wrapper;
    }

    /** {@inheritDoc} */
    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
        return super.buildProcessor(graph);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
        return super.buildPublisher(graph);
    }

    /** {@inheritDoc} */
    @Override
    public <T, R> SubscriberWithCompletionStage<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException {
        return super.buildSubscriber(graph);
    }

}
