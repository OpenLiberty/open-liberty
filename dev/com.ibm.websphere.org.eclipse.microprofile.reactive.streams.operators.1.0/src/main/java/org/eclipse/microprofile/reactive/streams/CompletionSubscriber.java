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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * A subscriber that redeems a completion stage when it completes.
 * <p>
 * The result is provided through a {@link CompletionStage}, which is redeemed when the subscriber receives a
 * completion or error signal, or otherwise cancels the stream.
 * <p>
 * The way to instantiate one of these is using the {@link CompletionSubscriber#of} factory method.
 *
 * @param <T> The type of the elements that the subscriber consumes.
 * @param <R> The type of the result that the subscriber emits.
 */
public final class CompletionSubscriber<T, R> implements Subscriber<T> {

    private final Subscriber<T> subscriber;
    private final CompletionStage<R> completion;

    private CompletionSubscriber(Subscriber<T> subscriber, CompletionStage<R> completion) {
        this.subscriber = Objects.requireNonNull(subscriber, "Subscriber must not be null");
        this.completion = Objects.requireNonNull(completion, "CompletionStage must not be null");
    }

    /**
     * Create a {@link CompletionSubscriber} by combining the given subscriber and completion stage.
     *
     * @param subscriber The subscriber.
     * @param completion The completion stage.
     * @return A completion subscriber.
     */
    public static <T, R> CompletionSubscriber<T, R> of(Subscriber<T> subscriber, CompletionStage<R> completion) {
        return new CompletionSubscriber<>(subscriber, completion);
    }

    /**
     * Get the completion stage.
     * <p>
     * This should be redeemed by the subscriber either when it cancels, or when it receives an
     * {@link Subscriber#onComplete} signal or an {@link Subscriber#onError(Throwable)} signal. Generally, the redeemed
     * value or error should be the result of consuming the stream.
     *
     * @return The completion stage.
     */
    public CompletionStage<R> getCompletion() {
        return completion;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(T t) {
        subscriber.onNext(t);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }

    @Override
    public String toString() {
        return "CompletionSubscriber(" + subscriber + ", " + completion + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompletionSubscriber<?, ?> that = (CompletionSubscriber<?, ?>) o;
        return Objects.equals(subscriber, that.subscriber) &&
            Objects.equals(completion, that.completion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriber, completion);
    }
}
