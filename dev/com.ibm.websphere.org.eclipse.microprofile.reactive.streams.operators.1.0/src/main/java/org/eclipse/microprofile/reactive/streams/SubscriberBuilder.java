/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.eclipse.microprofile.reactive.streams;

import org.eclipse.microprofile.reactive.streams.spi.Graph;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.spi.Stage;

/**
 * A builder for a {@link org.reactivestreams.Subscriber} and its result.
 * <p>
 * When built, this builder returns a {@link CompletionSubscriber}, which encapsulates both a
 * {@link org.reactivestreams.Subscriber} and a {@link java.util.concurrent.CompletionStage} that will be redeemed
 * with the result produced by the subscriber when the stream completes normally, or will be redeemed with an error
 * if the subscriber receives an error.
 *
 * @param <T> The type of the elements that this subscriber consumes.
 * @param <R> The type of the result that this subscriber emits.
 * @see ReactiveStreams
 */
public final class SubscriberBuilder<T, R> {

    private final ReactiveStreamsGraphBuilder graphBuilder;

    SubscriberBuilder(ReactiveStreamsGraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }

    SubscriberBuilder(Stage stage) {
        this.graphBuilder = new ReactiveStreamsGraphBuilder(stage);
    }

    /**
     * Build this stream, using the first {@link ReactiveStreamsEngine} found by the {@link java.util.ServiceLoader}.
     *
     * @return A {@link CompletionSubscriber} that will run this stream.
     */
    public CompletionSubscriber<T, R> build() {
        return build(ReactiveStreamsGraphBuilder.defaultEngine());
    }

    /**
     * Build this stream, using the supplied {@link ReactiveStreamsEngine}.
     *
     * @param engine The engine to run the stream with.
     * @return A {@link CompletionSubscriber} that will run this stream.
     */
    public CompletionSubscriber<T, R> build(ReactiveStreamsEngine engine) {
        return engine.buildSubscriber(toGraph());
    }

    ReactiveStreamsGraphBuilder getGraphBuilder() {
        return graphBuilder;
    }

    Graph toGraph() {
        return graphBuilder.build(true, false);
    }
}
