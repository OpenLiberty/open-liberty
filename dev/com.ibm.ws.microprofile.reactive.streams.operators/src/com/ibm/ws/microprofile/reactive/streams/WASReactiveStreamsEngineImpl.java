package com.ibm.ws.microprofile.reactive.streams;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

import org.eclipse.microprofile.reactive.streams.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.spi.Graph;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.spi.UnsupportedStageException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import com.lightbend.microprofile.reactive.streams.zerodep.BuiltGraph;
import com.lightbend.microprofile.reactive.streams.zerodep.ReactiveStreamsEngineImpl;

@Component(name = "com.ibm.ws.microprofile.reactive.streams.ReactiveStreamsEngineImpl", service = { ReactiveStreamsEngine.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class WASReactiveStreamsEngineImpl extends ReactiveStreamsEngineImpl implements ReactiveStreamsEngine {

    @Override
    public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildPublisher(/* TODO CHANGE */ForkJoinPool.commonPool(), graph);
    }

    @Override
    public <T, R> CompletionSubscriber<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildSubscriber(/* TODO CHANGE */ForkJoinPool.commonPool(), graph);
    }

    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildProcessor(/* TODO CHANGE */ForkJoinPool.commonPool(), graph);
    }

    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildCompletion(/* TODO CHANGE */ForkJoinPool.commonPool(), graph);
    }

}
