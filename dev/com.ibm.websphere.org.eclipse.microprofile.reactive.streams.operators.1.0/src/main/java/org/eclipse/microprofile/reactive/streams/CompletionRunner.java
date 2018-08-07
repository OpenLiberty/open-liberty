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

import java.util.concurrent.CompletionStage;

/**
 * A builder for a closed reactive streams graph.
 * <p>
 * When built, this builder returns a {@link CompletionStage} that will be redeemed with the result produced by the
 * subscriber of the stream when the stream completes normally, or will be redeemed with an error if the stream
 * encounters an error.
 *
 * @param <T> The result of the stream.
 * @see ReactiveStreams
 */
public final class CompletionRunner<T> {

    private final ReactiveStreamsGraphBuilder graphBuilder;

    CompletionRunner(ReactiveStreamsGraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }


    /**
     * Run this stream, using the first {@link ReactiveStreamsEngine} found by the {@link java.util.ServiceLoader}.
     *
     * @return A completion stage that will be redeemed with the result of the stream, or an error if the stream fails.
     */
    public CompletionStage<T> run() {
        return run(ReactiveStreamsGraphBuilder.defaultEngine());
    }

    /**
     * Run this stream, using the supplied {@link ReactiveStreamsEngine}.
     *
     * @param engine The engine to run the stream with.
     * @return A completion stage that will be redeemed with the result of the stream, or an error if the stream fails.
     */
    public CompletionStage<T> run(ReactiveStreamsEngine engine) {
        return engine.buildCompletion(toGraph());
    }

    Graph toGraph() {
        return graphBuilder.build(false, false);
    }
}
