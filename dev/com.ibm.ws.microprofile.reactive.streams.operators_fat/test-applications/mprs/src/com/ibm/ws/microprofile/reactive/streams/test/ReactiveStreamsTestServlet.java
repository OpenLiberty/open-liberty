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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.function.Function;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.reactive.streams.GraphAccessor;
import org.eclipse.microprofile.reactive.streams.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.spi.Graph;
import org.eclipse.microprofile.reactive.streams.spi.Stage;
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

    String value = "v";
    String expectedValue = "v";

    @Test
    public void builderShouldBeImmutable() {
        ProcessorBuilder<Integer, Integer> builder = builder();
        ProcessorBuilder<Integer, Integer> mapped = builder.map(Function.identity());
        ProcessorBuilder<Integer, Integer> distinct = builder.distinct();
        SubscriberBuilder<Integer, Void> cancelled = builder.cancel();
        getAddedStage(Stage.Map.class, GraphAccessor.buildGraphFor(mapped));
        getAddedStage(Stage.Distinct.class, GraphAccessor.buildGraphFor(distinct));
        getAddedStage(Stage.Cancel.class, GraphAccessor.buildGraphFor(cancelled));
    }

    @Test
    public void helloReactiveWorld() {

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
                System.out.println("onSubscribe" + sub);
            }
        };

        data.via(filter).to(console).run();

    }

    private ProcessorBuilder<Integer, Integer> builder() {
        return ReactiveStreams.<Integer> builder().map(Function.identity());
    }

    private <S extends Stage> S getAddedStage(Class<S> clazz, Graph graph) {
        assertTrue("Graph doesn't have inlet but should because it's meant to be a processor: " + graph, graph.hasInlet());
        assertEquals("Graph does not have two stages", graph.getStages().size(), 2);
        Iterator<Stage> stages = graph.getStages().iterator();
        Stage first = stages.next();
        assertTrue("First stage " + first + " is not a " + Stage.Map.class, first instanceof Stage.Map);
        Stage second = stages.next();
        assertTrue("Second stage " + second + " is not a " + clazz, clazz.isInstance(second));
        return clazz.cast(second);
    }
}
