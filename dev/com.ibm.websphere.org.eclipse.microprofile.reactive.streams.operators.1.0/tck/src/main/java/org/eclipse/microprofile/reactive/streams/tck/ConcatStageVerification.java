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

package org.eclipse.microprofile.reactive.streams.tck;

import org.eclipse.microprofile.reactive.streams.ReactiveStreams;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.testng.Assert.assertEquals;

public class ConcatStageVerification extends AbstractStageVerification {

    ConcatStageVerification(ReactiveStreamsTck.VerificationDeps deps) {
        super(deps);
    }

    @Test
    public void concatStageShouldConcatTwoGraphs() {
        assertEquals(await(
            ReactiveStreams.concat(
                ReactiveStreams.of(1, 2, 3),
                ReactiveStreams.of(4, 5, 6)
            )
                .toList()
                .run(getEngine())
        ), Arrays.asList(1, 2, 3, 4, 5, 6));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void concatStageShouldCancelSecondStageIfFirstFails() {
        CancelCapturingPublisher<Integer> cancelCapture = new CancelCapturingPublisher<>();

        CompletionStage<Void> completion = ReactiveStreams.concat(
            ReactiveStreams.failed(new RuntimeException("failed")),
            ReactiveStreams.fromPublisher(cancelCapture)
        )
            .ignore()
            .run(getEngine());

        await(cancelCapture.getCancelled());
        await(completion);
    }

    @Test
    public void concatStageShouldCancelSecondStageIfFirstCancellationOccursDuringFirst() {
        CancelCapturingPublisher<Integer> cancelCapture = new CancelCapturingPublisher<>();

        CompletionStage<List<Integer>> result = ReactiveStreams.concat(
            ReactiveStreams.fromIterable(() -> IntStream.range(1, 1000000).boxed().iterator()),
            ReactiveStreams.fromPublisher(cancelCapture)
        )
            .limit(5)
            .toList()
            .run(getEngine());

        await(cancelCapture.getCancelled());
        assertEquals(await(result), Arrays.asList(1, 2, 3, 4, 5));
    }

    @Override
    List<Object> reactiveStreamsTckVerifiers() {
        return Collections.singletonList(new PublisherVerification());
    }

    private static class CancelCapturingPublisher<T> implements Publisher<T> {
        private final CompletableFuture<T> cancelled = new CompletableFuture<>();

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                    cancelled.complete(null);
                }
            });
        }

        public CompletableFuture<T> getCancelled() {
            return cancelled;
        }
    }

    class PublisherVerification extends StagePublisherVerification<Long> {
        @Override
        public Publisher<Long> createPublisher(long elements) {
            long toEmitFromFirst = elements / 2;

            return ReactiveStreams.concat(
                ReactiveStreams.fromIterable(
                    () -> LongStream.rangeClosed(1, toEmitFromFirst).boxed().iterator()
                ),
                ReactiveStreams.fromIterable(
                    () -> LongStream.rangeClosed(toEmitFromFirst + 1, elements).boxed().iterator()
                )
            ).buildRs(getEngine());
        }
    }

}
