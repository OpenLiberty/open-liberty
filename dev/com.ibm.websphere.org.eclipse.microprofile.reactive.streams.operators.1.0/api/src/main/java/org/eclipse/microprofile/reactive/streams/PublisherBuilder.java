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
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A builder for a {@link Publisher}.
 *
 * @param <T> The type of the elements that the publisher emits.
 * @see ReactiveStreams
 */
public final class PublisherBuilder<T> {

    private final ReactiveStreamsGraphBuilder graphBuilder;

    PublisherBuilder(ReactiveStreamsGraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }

    PublisherBuilder(Stage stage) {
        this.graphBuilder = new ReactiveStreamsGraphBuilder(stage);
    }

    /**
     * Map the elements emitted by this publisher using the {@code mapper} function.
     *
     * @param mapper The function to use to map the elements.
     * @param <R>    The type of elements that the {@code mapper} function emits.
     * @return A new publisher builder that emits the mapped elements.
     */
    public <R> PublisherBuilder<R> map(Function<? super T, ? extends R> mapper) {
        return addStage(new Stage.Map(mapper));
    }

    /**
     * Returns a stream containing all the elements from this stream, additionally performing the provided action on each
     * element.
     *
     * @param consumer The function called for every element.
     * @return A new processor builder that consumes elements of type <code>T</code> and emits the same elements. In between,
     * the given function is called for each element.
     */
    public PublisherBuilder<T> peek(Consumer<? super T> consumer) {
        return addStage(new Stage.Peek(consumer));
    }

    /**
     * Filter elements emitted by this publisher using the given {@link Predicate}.
     * <p>
     * Any elements that return {@code true} when passed to the {@link Predicate} will be emitted, all other
     * elements will be dropped.
     *
     * @param predicate The predicate to apply to each element.
     * @return A new publisher builder.
     */
    public PublisherBuilder<T> filter(Predicate<? super T> predicate) {
        return addStage(new Stage.Filter(predicate));
    }

    /**
     * Creates a stream consisting of the distinct elements (according to {@link Object#equals(Object)}) of this stream.
     *
     * @return A new publisher builder emitting the distinct elements from this stream.
     */
    public PublisherBuilder<T> distinct() {
        return addStage(Stage.Distinct.INSTANCE);
    }

    /**
     * Map the elements to publishers, and flatten so that the elements emitted by publishers produced by the
     * {@code mapper} function are emitted from this stream.
     * <p>
     * This method operates on one publisher at a time. The result is a concatenation of elements emitted from all the
     * publishers produced by the mapper function.
     * <p>
     * Unlike {@link #flatMapPublisher(Function)}}, the mapper function returns a {@link PublisherBuilder} instead of a
     * {@link Publisher}.
     *
     * @param mapper The mapper function.
     * @param <S>    The type of the elements emitted from the new publisher.
     * @return A new publisher builder.
     */
    public <S> PublisherBuilder<S> flatMap(Function<? super T, PublisherBuilder<? extends S>> mapper) {
        return addStage(new Stage.FlatMap(mapper.andThen(PublisherBuilder::toGraph)));
    }

    /**
     * Map the elements to publishers, and flatten so that the elements emitted by publishers produced by the
     * {@code mapper} function are emitted from this stream.
     * <p>
     * This method operates on one publisher at a time. The result is a concatenation of elements emitted from all the
     * publishers produced by the mapper function.
     * <p>
     * Unlike {@link #flatMap(Function)}, the mapper function returns a {@link Publisher} instead of a
     * {@link PublisherBuilder}.
     *
     * @param mapper The mapper function.
     * @param <S>    The type of the elements emitted from the new publisher.
     * @return A new publisher builder.
     */
    public <S> PublisherBuilder<S> flatMapPublisher(Function<? super T, Publisher<? extends S>> mapper) {
        return addStage(new Stage.FlatMap(mapper
            .andThen(ReactiveStreams::fromPublisher)
            .andThen(PublisherBuilder::toGraph)));
    }

    /**
     * Map the elements to {@link CompletionStage}, and flatten so that the elements the values redeemed by each
     * {@link CompletionStage} are emitted from this publisher.
     * <p>
     * This method only works with one element at a time. When an element is received, the {@code mapper} function is
     * executed, and the next element is not consumed or passed to the {@code mapper} function until the previous
     * {@link CompletionStage} is redeemed. Hence this method also guarantees that ordering of the stream is maintained.
     *
     * @param mapper The mapper function.
     * @param <S>    The type of the elements emitted from the new publisher.
     * @return A new publisher builder.
     */
    public <S> PublisherBuilder<S> flatMapCompletionStage(Function<? super T, ? extends CompletionStage<? extends S>> mapper) {
        return addStage(new Stage.FlatMapCompletionStage((Function) mapper));
    }

    /**
     * Map the elements to {@link Iterable}'s, and flatten so that the elements contained in each iterable are
     * emitted by this stream.
     * <p>
     * This method operates on one iterable at a time. The result is a concatenation of elements contain in all the
     * iterables returned by the {@code mapper} function.
     *
     * @param mapper The mapper function.
     * @param <S>    The type of the elements emitted from the new publisher.
     * @return A new publisher builder.
     */
    public <S> PublisherBuilder<S> flatMapIterable(Function<? super T, ? extends Iterable<? extends S>> mapper) {
        return addStage(new Stage.FlatMapIterable((Function) mapper));
    }

    /**
     * Truncate this stream, ensuring the stream is no longer than {@code maxSize} elements in length.
     * <p>
     * If {@code maxSize} is reached, the stream will be completed, and upstream will be cancelled. Completion of the
     * stream will occur immediately when the element that satisfies the {@code maxSize} is received.
     *
     * @param maxSize The maximum size of the returned stream.
     * @return A new publisher builder.
     * @throws IllegalArgumentException If {@code maxSize} is less than zero.
     */
    public PublisherBuilder<T> limit(long maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("Cannot limit a stream to less than zero elements.");
        }
        else {
            return addStage(new Stage.Limit(maxSize));
        }
    }

    /**
     * Discard the first {@code n} of this stream. If this stream contains fewer than {@code n} elements, this stream will
     * effectively be an empty stream.
     *
     * @param n The number of elements to discard.
     * @return A new publisher builder.
     * @throws IllegalArgumentException If {@code n} is less than zero.
     */
    public PublisherBuilder<T> skip(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("Cannot skip less than zero elements");
        }
        return addStage(new Stage.Skip(n));
    }

    /**
     * Take the longest prefix of elements from this stream that satisfy the given {@code predicate}.
     * <p>
     * When the {@code predicate} returns false, the stream will be completed, and upstream will be cancelled.
     *
     * @param predicate The predicate.
     * @return A new publisher builder.
     */
    public PublisherBuilder<T> takeWhile(Predicate<? super T> predicate) {
        return addStage(new Stage.TakeWhile(predicate));
    }

    /**
     * Drop the longest prefix of elements from this stream that satisfy the given {@code predicate}.
     * <p>
     * As long as the {@code predicate} returns true, no elements will be emitted from this stream. Once the first element
     * is encountered for which the {@code predicate} returns false, all subsequent elements will be emitted, and the
     * {@code predicate} will no longer be invoked.
     *
     * @param predicate The predicate.
     * @return A new publisher builder.
     */
    public PublisherBuilder<T> dropWhile(Predicate<? super T> predicate) {
        return addStage(new Stage.DropWhile(predicate));
    }

    /**
     * Performs an action for each element on this stream.
     * <p>
     * The returned {@link CompletionStage} will be redeemed when the stream completes, either successfully if the stream
     * completes normally, or with an error if the stream completes with an error or if the action throws an exception.
     *
     * @param action The action.
     * @return A new completion builder.
     */
    public CompletionRunner<Void> forEach(Consumer<? super T> action) {
        return collect(Collector.<T, Void, Void>of(
            () -> null,
            (n, t) -> action.accept(t),
            (v1, v2) -> null,
            v -> null
        ));
    }

    /**
     * Ignores each element of this stream.
     * <p>
     * The returned {@link CompletionStage} will be redeemed when the stream completes, either successfully if the
     * stream completes normally, or with an error if the stream completes with an error or if the action throws an
     * exception.
     *
     * @return A new completion builder.
     */
    public CompletionRunner<Void> ignore() {
        return forEach(r -> {
        });
    }

    /**
     * Cancels the stream as soon as it starts.
     * <p>
     * The returned {@link CompletionStage} will be immediately redeemed as soon as the stream starts.
     *
     * @return A new completion builder.
     */
    public CompletionRunner<Void> cancel() {
        return addTerminalStage(Stage.Cancel.INSTANCE);
    }

    /**
     * Perform a reduction on the elements of this stream, using the provided identity value and the accumulation
     * function.
     * <p>
     * The result of the reduction is returned in the {@link CompletionStage}.
     *
     * @param identity    The identity value.
     * @param accumulator The accumulator function.
     * @return A new completion builder.
     */
    public CompletionRunner<T> reduce(T identity, BinaryOperator<T> accumulator) {
        return addTerminalStage(new Stage.Collect(Reductions.reduce(identity, accumulator)));
    }

    /**
     * Perform a reduction on the elements of this stream, using provided the accumulation function.
     * <p>
     * The result of the reduction is returned in the {@link CompletionStage}. If there are no elements in this stream,
     * empty will be returned.
     *
     * @param accumulator The accumulator function.
     * @return A new completion builder.
     */
    public CompletionRunner<Optional<T>> reduce(BinaryOperator<T> accumulator) {
        return addTerminalStage(new Stage.Collect(Reductions.reduce(accumulator)));
    }

    /**
     * Perform a reduction on the elements of this stream, using the provided identity value, accumulation function and
     * combiner function.
     * <p>
     * The result of the reduction is returned in the {@link CompletionStage}.
     *
     * @param identity    The identity value.
     * @param accumulator The accumulator function.
     * @param combiner    The combiner function.
     * @return A new completion builder.
     */
    public <S> CompletionRunner<S> reduce(S identity,
                                          BiFunction<S, ? super T, S> accumulator,
                                          BinaryOperator<S> combiner) {

        return addTerminalStage(new Stage.Collect(Reductions.reduce(identity, accumulator, combiner)));
    }

    /**
     * Find the first element emitted by the {@link Publisher}, and return it in a
     * {@link CompletionStage}.
     * <p>
     * If the stream is completed before a single element is emitted, then {@link Optional#empty()} will be emitted.
     *
     * @return A {@link CompletionRunner} that emits the element when found.
     */
    public CompletionRunner<Optional<T>> findFirst() {
        return addTerminalStage(Stage.FindFirst.INSTANCE);
    }

    /**
     * Collect the elements emitted by this publisher builder using the given {@link Collector}.
     * <p>
     * Since Reactive Streams are intrinsically sequential, only the accumulator of the collector will be used, the
     * combiner will not be used.
     *
     * @param collector The collector to collect the elements.
     * @param <R>       The result of the collector.
     * @param <A>       The accumulator type.
     * @return A {@link CompletionRunner} that emits the collected result.
     */
    public <R, A> CompletionRunner<R> collect(Collector<? super T, A, R> collector) {
        return addTerminalStage(new Stage.Collect(collector));
    }

    /**
     * Collect the elements emitted by this processor builder using a {@link Collector} built from the given
     * {@link Supplier supplier} and {@link BiConsumer accumulator}.
     * <p>
     * Since Reactive Streams are intrinsically sequential, the combiner will not be used. This is why this method does not
     * accept a <em>combiner</em> method.
     *
     * @param supplier    a function that creates a new result container. It creates objects of type {@code <A>}.
     * @param accumulator an associative, non-interfering, stateless function for incorporating an additional element into a
     *                    result
     * @param <R>         The result of the collector.
     * @return A {@link CompletionRunner} that emits the collected result.
     */
    public <R> CompletionRunner<R> collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator) {
        // The combiner is not used, so the used, but should not be null
        return addTerminalStage(new Stage.Collect(Collector.of(supplier, accumulator, (a, b) -> a)));
    }

    /**
     * Collect the elements emitted by this publisher builder into a {@link List}
     *
     * @return A {@link CompletionRunner} that emits the list.
     */
    public CompletionRunner<List<T>> toList() {
        return collect(Collectors.toList());
    }

    /**
     * Connect the outlet of the {@link Publisher} built by this builder to the given {@link Subscriber}.
     *
     * @param subscriber The subscriber to connect.
     * @return A {@link CompletionRunner} that completes when the stream completes.
     */
    public CompletionRunner<Void> to(Subscriber<T> subscriber) {
        return addTerminalStage(new Stage.SubscriberStage(subscriber));
    }

    /**
     * Connect the outlet of this publisher builder to the given {@link SubscriberBuilder} graph.
     *
     * @param subscriber The subscriber builder to connect.
     * @return A {@link CompletionRunner} that completes when the stream completes.
     */
    public <R> CompletionRunner<R> to(SubscriberBuilder<T, R> subscriber) {
        return addTerminalStage(new InternalStages.Nested(subscriber.getGraphBuilder()));
    }

    /**
     * Connect the outlet of the {@link Publisher} built by this builder to the given {@link Processor}.
     *
     * @param processor The processor to connect.
     * @return A {@link PublisherBuilder} that represents the passed in processors outlet.
     */
    public <R> PublisherBuilder<R> via(ProcessorBuilder<T, R> processor) {
        return addStage(new InternalStages.Nested(processor.getGraphBuilder()));
    }

    /**
     * Connect the outlet of this publisher builder to the given {@link ProcessorBuilder} graph.
     *
     * @param processor The processor builder to connect.
     * @return A {@link PublisherBuilder} that represents the passed in processor builders outlet.
     */
    public <R> PublisherBuilder<R> via(Processor<T, R> processor) {
        return addStage(new Stage.ProcessorStage(processor));
    }

    /**
     * Returns a stream containing all the elements from this stream, additionally performing the provided action if this
     * stream conveys an error. The given consumer is called with the failure.
     *
     * @param errorHandler The function called with the failure.
     * @return A new processor builder that consumes elements of type <code>T</code> and emits the same elements. If the
     * stream conveys a failure, the given error handler is called.
     */
    public PublisherBuilder<T> onError(Consumer<Throwable> errorHandler) {
        return addStage(new Stage.OnError(errorHandler));
    }

    /**
     * Returns a stream containing all the elements from this stream. Additionally, in the case of failure, rather than
     * invoking {@link #onError(Consumer)}, it invokes the given method and emits the result as final event of the stream.
     *
     * By default, when a stream encounters an error that prevents it from emitting the expected item to its subscriber,
     * the stream (publisher) invokes its subscriber's <code>onError</code> method, and then terminate without invoking
     * any more of its subscriber's methods. This operator changes this behavior. If the current stream encounters an
     * error, instead of invoking its subscriber's <code>onError</code> method, it will instead emit the return value of
     * the passed function. This operator prevents errors from propagating or to supply fallback data should errors be
     * encountered.
     *
     * @param errorHandler the function returning the value that need to be emitting instead of the error.
     *                     The function must not return {@code null}
     * @return The new publisher
     */
    public PublisherBuilder<T> onErrorResume(Function<Throwable, T> errorHandler) {
        return addStage(new Stage.OnErrorResume(errorHandler));
    }

    /**
     * Returns a stream containing all the elements from this stream. Additionally, in the case of failure, rather than
     * invoking {@link #onError(Consumer)}, it invokes the given method and emits the returned {@link PublisherBuilder}
     * instead.
     *
     * By default, when a stream encounters an error that prevents it from emitting the expected item to its subscriber,
     * the stream (publisher) invokes its subscriber's <code>onError</code> method, and then terminate without invoking
     * any more of its subscriber's methods. This operator changes this behavior. If the current stream encounters an
     * error, instead of invoking its subscriber's <code>onError</code> method, it will instead relinquish control to the
     * {@link PublisherBuilder} returned from given function, which invoke the subscriber's <code>onNext</code> method if
     * it is able to do so. In such a case, because no publisher necessarily invokes <code>onError</code>, the subscriber
     * may never know that an error happened.
     *
     * @param errorHandler the function returning the stream that need to be emitting instead of the error.
     *                     The function must not return {@code null}
     * @return The new publisher
     */
    public PublisherBuilder<T> onErrorResumeWith(Function<Throwable, PublisherBuilder<T>> errorHandler) {
        return addStage(new Stage.OnErrorResumeWith(errorHandler.andThen(PublisherBuilder::toGraph)));
    }

    /**
     * Returns a stream containing all the elements from this stream. Additionally, in the case of failure, rather than
     * invoking {@link #onError(Consumer)}, it invokes the given method and emits the returned {@link PublisherBuilder}
     * instead.
     *
     * By default, when a stream encounters an error that prevents it from emitting the expected item to its subscriber,
     * the stream (publisher) invokes its subscriber's <code>onError</code> method, and then terminate without invoking
     * any more of its subscriber's methods. This operator changes this behavior. If the current stream encounters an
     * error, instead of invoking its subscriber's <code>onError</code> method, it will instead relinquish control to the
     * {@link PublisherBuilder} returned from given function, which invoke the subscriber's <code>onNext</code> method if
     * it is able to do so. In such a case, because no publisher necessarily invokes <code>onError</code>, the subscriber
     * may never know that an error happened.
     *
     * @param errorHandler the function returning the stream that need to be emitting instead of the error.
     *                     The function must not return {@code null}
     * @return The new publisher
     */
    public <S> PublisherBuilder<S> onErrorResumeWithPublisher(Function<Throwable, Publisher<? extends S>> errorHandler) {
        return addStage(new Stage.OnErrorResumeWith(
            errorHandler
                .andThen(ReactiveStreams::fromPublisher)
                .andThen(PublisherBuilder::toGraph))
        );
    }

    /**
     * Returns a stream containing all the elements from this stream, additionally performing the provided action when this
     * stream completes or failed. The given action does not know if the stream failed or completed. If you need to
     * distinguish use {@link #onError(Consumer)} and {@link #onComplete(Runnable)}. In addition, the action is called if
     * the stream is cancelled downstream.
     *
     * @param action The function called when the stream completes or failed.
     * @return A new processor builder that consumes elements of type <code>T</code> and emits the same elements. The given
     * action is called when the stream completes or fails.
     */
    public PublisherBuilder<T> onTerminate(Runnable action) {
        return addStage(new Stage.OnTerminate(action));
    }

    /**
     * Returns a stream containing all the elements from this stream, additionally performing the provided action when this
     * stream completes.
     *
     * @param action The function called when the stream completes.
     * @return A new processor builder that consumes elements of type <code>T</code> and emits the same elements. The given
     * action is called when the stream completes.
     */
    public PublisherBuilder<T> onComplete(Runnable action) {
        return addStage(new Stage.OnComplete(action));
    }

    Graph toGraph() {
        return graphBuilder.build(false, true);
    }

    /**
     * Build this stream, using the first {@link ReactiveStreamsEngine} found by the {@link java.util.ServiceLoader}.
     *
     * @return A {@link Processor} that will run this stream.
     */
    public Publisher<T> buildRs() {
        return buildRs(ReactiveStreamsGraphBuilder.defaultEngine());
    }

    /**
     * Build this stream, using the supplied {@link ReactiveStreamsEngine}.
     *
     * @param engine The engine to run the stream with.
     * @return A {@link Publisher} that will run this stream.
     */
    public Publisher<T> buildRs(ReactiveStreamsEngine engine) {
        return engine.buildPublisher(toGraph());
    }

    private <R> PublisherBuilder<R> addStage(Stage stage) {
        return new PublisherBuilder<>(graphBuilder.addStage(stage));
    }

    private <R> CompletionRunner<R> addTerminalStage(Stage stage) {
        return new CompletionRunner<>(graphBuilder.addStage(stage));
    }
}
