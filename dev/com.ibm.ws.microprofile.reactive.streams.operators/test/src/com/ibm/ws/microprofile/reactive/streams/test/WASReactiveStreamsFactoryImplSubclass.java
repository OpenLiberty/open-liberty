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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * This is a simple subclass that merely records if its methods have been called
 * before delegating to the superclass
 */
public class WASReactiveStreamsFactoryImplSubclass extends ReactiveStreamsFactoryImpl
        implements ReactiveStreamsFactory {

    private boolean builderCalled;
    private boolean ofCalled;
    private boolean fromIterableCalled;

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> fromPublisher(Publisher<? extends T> publisher) {
        return super.fromPublisher(publisher);
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> of(T t) {
        return super.of(t);
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> of(T... ts) {
        ofCalled = true;
        return super.of(ts);
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> empty() {
        return super.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> ofNullable(T t) {
        return super.ofNullable(t);
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> fromIterable(Iterable<? extends T> ts) {
        fromIterableCalled = true;
        return super.fromIterable(ts);
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> failed(Throwable t) {
        return super.failed(t);
    }

    /** {@inheritDoc} */
    @Override
    public <T> ProcessorBuilder<T, T> builder() {
        builderCalled = true;
        return super.builder();
    }

    /** {@inheritDoc} */
    @Override
    public <T, R> ProcessorBuilder<T, R> fromProcessor(Processor<? super T, ? extends R> processor) {
        return super.fromProcessor(processor);
    }

    /** {@inheritDoc} */
    @Override
    public <T> SubscriberBuilder<T, Void> fromSubscriber(Subscriber<? extends T> subscriber) {
        return super.fromSubscriber(subscriber);
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> iterate(T seed, UnaryOperator<T> f) {
        return super.iterate(seed, f);
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> generate(Supplier<? extends T> s) {
        return super.generate(s);

    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> concat(PublisherBuilder<? extends T> a, PublisherBuilder<? extends T> b) {
        return super.concat(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> fromCompletionStage(CompletionStage<? extends T> completionStage) {
        return super.fromCompletionStage(completionStage);
    }

    /** {@inheritDoc} */
    @Override
    public <T> PublisherBuilder<T> fromCompletionStageNullable(CompletionStage<? extends T> completionStage) {
        return super.fromCompletionStageNullable(completionStage);
    }

    /** {@inheritDoc} */
    @Override
    public <T, R> ProcessorBuilder<T, R> coupled(SubscriberBuilder<? super T, ?> subscriber,
            PublisherBuilder<? extends R> publisher) {
        return super.coupled(subscriber, publisher);
    }

    /** {@inheritDoc} */
    @Override
    public <T, R> ProcessorBuilder<T, R> coupled(Subscriber<? super T> subscriber, Publisher<? extends R> publisher) {
        return super.coupled(subscriber, publisher);
    }

    /**
     * @return
     */
    public String check() {
        String check = "BitString:" + builderCalled + ofCalled + fromIterableCalled;
        return check;
    }

}
