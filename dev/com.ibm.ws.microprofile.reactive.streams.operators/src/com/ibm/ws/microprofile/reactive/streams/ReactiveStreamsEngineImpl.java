package com.ibm.ws.microprofile.reactive.streams;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.spi.Graph;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.spi.UnsupportedStageException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

@Component(name = "com.ibm.ws.microprofile.reactive.streams.ReactiveStreamsEngineImpl", service = { ReactiveStreamsEngine.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ReactiveStreamsEngineImpl implements ReactiveStreamsEngine {

    /** {@inheritDoc} */
    @Override
    public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public <T, R> CompletionSubscriber<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {
        // TODO Auto-generated method stub
        return null;
    }

}
