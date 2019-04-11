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
package com.ibm.ws.microprofile.reactive.streams.operators.spi.impl;

import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.eclipse.microprofile.reactive.streams.operators.spi.SubscriberWithCompletionStage;
import org.eclipse.microprofile.reactive.streams.operators.spi.UnsupportedStageException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threadcontext.WSContextService;

import io.smallrye.reactive.streams.Engine;

@Component(name = "com.ibm.ws.microprofile.reactive.streams.operators.spi.impl.WASReactiveStreamsEngineImpl", service = {
        ReactiveStreamsEngine.class }, property = {
                "service.vendor=IBM" }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class WASReactiveStreamsEngineImpl extends Engine implements ReactiveStreamsEngine {

    private static final TraceComponent tc = Tr.register(WASReactiveStreamsEngineImpl.class);

    private static ReactiveStreamsEngine singleton = null;

    private final AtomicServiceReference<ExecutorService> executorServiceRef = new AtomicServiceReference<ExecutorService>(
            "executorService");

    private final AtomicServiceReference<WSContextService> contextServiceRef = new AtomicServiceReference<WSContextService>(
            "contextService");

    /**
     * The OSGi component active call
     *
     * @param cc the OSGi component context
     */
    public void activate(ComponentContext cc) {
        executorServiceRef.activate(cc);
        contextServiceRef.activate(cc);
        ReactiveStreamsFactoryResolver.setInstance(new ReactiveStreamsFactoryImpl());
        ReactiveStreamsEngineResolver.setInstance(this);
        singleton = this;
    }

    /**
     * The OSGi component deactive call
     *
     * @param cc the OSGi component context
     */
    public void deactivate(ComponentContext cc) {
        singleton = null;
        ReactiveStreamsEngineResolver.setInstance(null);
        ReactiveStreamsFactoryResolver.setInstance(null);
        executorServiceRef.deactivate(cc);
        contextServiceRef.deactivate(cc);
    }

    @Reference(name = "executorService", service = ExecutorService.class)
    protected void setExecutorService(ServiceReference<ExecutorService> ref) {
        executorServiceRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the context service reference
     *
     * @param ref reference to the service
     */
    @Reference(name = "contextService", service = WSContextService.class)
    protected void setContextService(ServiceReference<WSContextService> ref) {
        contextServiceRef.setReference(ref);
    }

    public WASReactiveStreamsEngineImpl() {
        super();
    }

    /**
     * A means for Unit Tests to get hold of the engine
     *
     * @return a usable ReactiveStreamsEngine
     */
    public static ReactiveStreamsEngine getEngine() {
        if (singleton == null) {
            singleton = new WASReactiveStreamsEngineImpl();
        }
        return singleton;
    }

    /**
     * Get the real or UnitTest executor service for getting threads from. Will use
     * the ForkJoin.commonPool for UT and the WAS "ExecutorService" service in a
     * server
     *
     * @return the executor
     */
    public ExecutorService getExecutor() {
        ExecutorService executor = executorServiceRef != null ? executorServiceRef.getService() : null;
        if (executor != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The Liberty ExecutorService is being used to run asynch reactive work");
            }
            return executor;
        } else {
            // For unit testing
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The ForkJoinPool.commonPool is being used to run asynch reactive work");
            }
            return ForkJoinPool.commonPool();

        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {

        PrivilegedAction<CompletionStage<T>> action = new PrivilegedAction<CompletionStage<T>>() {
            @Override
            public CompletionStage<T> run() {
                return WASReactiveStreamsEngineImpl.super.buildCompletion(graph);
            }
        };

        WSContextService contextService;
        if (contextServiceRef != null) {
            contextService = contextServiceRef.getService();
        } else {
            contextService = null;
        }

        StreamRunner<T> runner = new StreamRunner<>(getExecutor(), contextService, action);

        StreamTask<?> streamTask = runner.startStream();

        CompletableFuture<T> wrapper = (CompletableFuture<T>) streamTask.getWrapperCompletableFuture();

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
