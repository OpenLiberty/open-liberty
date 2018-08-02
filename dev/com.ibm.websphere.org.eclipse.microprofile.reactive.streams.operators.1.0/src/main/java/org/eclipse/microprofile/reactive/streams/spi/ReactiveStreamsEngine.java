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

package org.eclipse.microprofile.reactive.streams.spi;


import org.eclipse.microprofile.reactive.streams.CompletionSubscriber;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletionStage;

/**
 * An engine for turning reactive streams graphs into Reactive Streams publishers/subscribers.
 * <p>
 * The zero argument {@code build} and {@code run} methods on subclasses of this will use
 * the {@link java.util.ServiceLoader} to load an engine for the current context classloader. It does not cache
 * engines between invocations. If instantiating an engine is expensive (eg, it creates threads), then it is
 * recommended that the implementation does its own caching by providing the engine using a static provider method.
 */
public interface ReactiveStreamsEngine {

    /**
     * Build a {@link Publisher} from the given stages.
     * <p>
     * The passed in graph will have an outlet and no inlet.
     *
     * @param graph The stages to build the publisher from. Will not be empty.
     * @param <T>   The type of elements that the publisher publishes.
     * @return A publisher that implements the passed in graph of stages.
     * @throws UnsupportedStageException If a stage in the stages is not supported by this Reactive Streams engine.
     */
    <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException;

    /**
     * Build a {@link org.reactivestreams.Subscriber} from the given stages.
     * <p>
     * The passed in graph will have an inlet and no outlet.
     *
     * @param graph The graph to build the subscriber from. Will not be empty.
     * @param <T>   The type of elements that the subscriber subscribes to.
     * @param <R>   The result of subscribing to the stages.
     * @return A subscriber that implements the passed in graph of stages.
     * @throws UnsupportedStageException If a stage in the stages is not supported by this Reactive Streams engine.
     */
    <T, R> CompletionSubscriber<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException;

    /**
     * Build a {@link Processor} from the given stages.
     * <p>
     * The passed in graph will have an inlet and an outlet.
     *
     * @param graph The graph to build the processor from. If empty, then the processor should be an identity processor.
     * @param <T>   The type of elements that the processor subscribes to.
     * @param <R>   The type of elements that the processor publishes.
     * @return A processor that implements the passed in graph of stages.
     * @throws UnsupportedStageException If a stage in the stages is not supported by this Reactive Streams engine.
     */
    <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException;

    /**
     * Build a closed graph from the given stages.
     * <p>
     * The passed in graph will have no inlet and no outlet.
     *
     * @param graph The graph to build the closed graph from. Will not be empty.
     * @param <T>   The type of the result of running the closed graph.
     * @return A CompletionStage of the result of running the graph.
     * @throws UnsupportedStageException If a stage in the stages is not supported by this reactive streams engine.
     */
    <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException;

}
