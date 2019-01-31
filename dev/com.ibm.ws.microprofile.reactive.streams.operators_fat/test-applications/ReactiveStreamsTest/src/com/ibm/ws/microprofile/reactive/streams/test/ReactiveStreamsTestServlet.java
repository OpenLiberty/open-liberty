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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.eclipse.microprofile.reactive.streams.operators.spi.ToGraphable;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/ReactiveStreamsTest")
public class ReactiveStreamsTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    ReactiveStreamsEngine engine1;

    String value = "v";
    String expectedValue = "v";

    /*
     * A very simple test that accesses some of the SPI
     */
    @Test
    public void builderShouldBeImmutable() {
        ProcessorBuilder<Integer, Integer> builder = builder();
        ProcessorBuilder<Integer, Integer> mapped = builder.map(Function.identity());
        ProcessorBuilder<Integer, Integer> distinct = builder.distinct();
        SubscriberBuilder<Integer, Void> cancelled = builder.cancel();
        getAddedStage(Stage.Map.class, graphFor(mapped));
        getAddedStage(Stage.Distinct.class, graphFor(distinct));
        getAddedStage(Stage.Cancel.class, graphFor(cancelled));
    }

    /*
     * Another simple test that plumbs a list to Subscriber
     */
    @Test
    public void helloReactiveWorld() throws InterruptedException, ExecutionException {

        PublisherBuilder<Integer> data = ReactiveStreams.of(1, 2, 3, 4, 5);
        ProcessorBuilder<Integer, Integer> filter = ReactiveStreams.<Integer> builder().dropWhile(n -> n == 3);

        Subscriber<Integer> console = new Subscriber<Integer>() {

            private Subscription sub;

            @Override
            public void onComplete() {
                System.out.println("onComplete");
            }

            @Override
            public void onError(Throwable arg0) {
                System.out.println("onError");
            }

            @Override
            public void onNext(Integer arg0) {
                System.out.println("Number received: " + arg0);
                sub.request(1);
            }

            @Override
            public void onSubscribe(Subscription arg0) {
                sub = arg0;
                sub.request(1L);
                System.out.println("onSubscribe" + sub);
            }
        };

        CompletionStage<Void> cs = data.via(filter).to(console).run();
        cs.toCompletableFuture().get();
    }

    /**
     * A simple test that checks that user code can
     *
     * @Inject a ReactiveStreamsEngine
     */
    @Test
    public void injectReactiveStreamsEngineTest() {
        assertTrue("Reactive Streams Engine has been injected as null", engine1 != null);
    }

    /**
     * A simple test that checks that user code can
     * SericeLoader.load a ReactiveStreamsEngine
     */
    @Test
    public void serviceLoadReactiveStreamsEngineTest() {
        Iterator<ReactiveStreamsEngine> engines = ServiceLoader.load(ReactiveStreamsEngine.class).iterator();
        assertTrue("Reactive Streams Engine has been injected as null", engines.hasNext());
    }

    private ProcessorBuilder<Integer, Integer> builder() {
        return ReactiveStreams.<Integer> builder().map(Function.identity());
    }

    private <S extends Stage> S getAddedStage(Class<S> clazz, Graph graph) {
        assertEquals("Graph does not have two stages", graph.getStages().size(), 2);
        Iterator<Stage> stages = graph.getStages().iterator();
        Stage first = stages.next();
        assertTrue("First stage " + first + " is not a " + Stage.Map.class, first instanceof Stage.Map);
        Stage second = stages.next();
        assertTrue("Second stage " + second + " is not a " + clazz, clazz.isInstance(second));
        return clazz.cast(second);
    }

    protected Graph graphFor(PublisherBuilder<?> pb) {
        return objGraphFor(pb);
    }

    protected Graph graphFor(SubscriberBuilder<?, ?> sb) {
        return objGraphFor(sb);
    }

    protected Graph graphFor(ProcessorBuilder<?, ?> pb) {
        return objGraphFor(pb);
    }

    protected Graph graphFor(CompletionRunner<?> cr) {
        return objGraphFor(cr);
    }

    private Graph objGraphFor(Object o) {
        return ((ToGraphable) o).toGraph();
    }

    protected void assertEmptyStage(Stage stage) {
        assertTrue(stage instanceof Stage.Of);
        assertEquals(((Stage.Of) stage).getElements(), Collections.emptyList());
    }
}
