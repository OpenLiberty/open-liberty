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
package com.ibm.ws.microprofile.reactive.streams;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import org.eclipse.microprofile.reactive.streams.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.spi.Graph;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.spi.UnsupportedStageException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.lightbend.microprofile.reactive.streams.zerodep.BuiltGraph;
import com.lightbend.microprofile.reactive.streams.zerodep.ReactiveStreamsEngineImpl;

@Component(name = "com.ibm.ws.microprofile.reactive.streams.ReactiveStreamsEngineImpl", service = { ReactiveStreamsEngine.class }, property = { "service.vendor=IBM" }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class WASReactiveStreamsEngineImpl extends ReactiveStreamsEngineImpl implements ReactiveStreamsEngine {

    private static final TraceComponent tc = Tr.register(ReactiveStreamsEngineImpl.class);

    private static ReactiveStreamsEngine singleton = null;

    private static ExecutorService serverExecutor = null;

    private static ExecutorService executor = null;

    /**
     * Declarative Services method for setting the Liberty executor.
     *
     * @param svc the service
     */
    @Reference(target = "(component.name=com.ibm.ws.threading)")
    protected void setExecutor(ExecutorService svc) {
        serverExecutor = svc;
    }

    /**
     * @param executor
     */
    public WASReactiveStreamsEngineImpl(ExecutorService ex) {
        // We have a fallback for unit testing
        WASReactiveStreamsEngineImpl.executor = (ex != null) ? ex : ForkJoinPool.commonPool();
    }

    @Override
    public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildPublisher(executor, graph);
    }

    @Override
    public <T, R> CompletionSubscriber<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildSubscriber(executor, graph);
    }

    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildProcessor(executor, graph);
    }

    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildCompletion(executor, graph);
    }

    /**
     * @return
     */
    public static ReactiveStreamsEngine getEngine() {
        if (singleton == null) {
            singleton = new WASReactiveStreamsEngineImpl(serverExecutor);
        }
        return singleton;
    }

    /**
     * @return the executor
     */
    public static ExecutorService getExecutor() {
        if (singleton == null) {
            singleton = new WASReactiveStreamsEngineImpl(serverExecutor);
        }
        return executor;
    }

}
