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
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.testng.Assert.assertEquals;

public class LimitStageVerification extends AbstractStageVerification {

    LimitStageVerification(ReactiveStreamsTck.VerificationDeps deps) {
        super(deps);
    }

    @Test
    public void limitStageShouldLimitTheOutputElements() {
        assertEquals(await(ReactiveStreams.of(1, 2, 3, 4, 5)
            .limit(3)
            .toList()
            .run(getEngine())), Arrays.asList(1, 2, 3));
    }

    @Test
    public void limitStageShouldAllowLimitingToZero() {
        assertEquals(await(ReactiveStreams.of(1, 2, 3, 4, 5, 6)
            .limit(0)
            .toList()
            .run(getEngine())), Collections.emptyList());
    }

    @Test
    public void limitStageToZeroShouldCompleteStreamEvenWhenNoElementsAreReceived() {
        assertEquals(await(ReactiveStreams.fromPublisher(subscriber ->
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            })
        ).limit(0)
            .toList()
            .run(getEngine())), Collections.emptyList());
    }

    @Test
    public void limitShouldCancelUpStreamWhenDone() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        ReactiveStreams.<Integer>fromPublisher(subscriber ->
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    subscriber.onNext(1);
                }

                @Override
                public void cancel() {
                    cancelled.complete(null);
                }
            })
        ).limit(1)
            .toList()
            .run(getEngine());
        await(cancelled);
    }

    @Test
    public void limitShouldIgnoreSubsequentErrorsWhenDone() {
        assertEquals(await(
            ReactiveStreams.of(1, 2, 3, 4)
                .flatMap(i -> {
                    if (i == 4) {
                        return ReactiveStreams.failed(new RuntimeException("failed"));
                    }
                    else {
                        return ReactiveStreams.of(i);
                    }
                })
                .limit(3)
                .toList()
                .run(getEngine())
        ), Arrays.asList(1, 2, 3));
    }

    @Override
    List<Object> reactiveStreamsTckVerifiers() {
        return Collections.singletonList(new ProcessorVerification());
    }

    public class ProcessorVerification extends StageProcessorVerification<Integer> {
        @Override
        public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
            return ReactiveStreams.<Integer>builder()
                .limit(Long.MAX_VALUE)
                .buildRs(getEngine());
        }

        @Override
        public Integer createElement(int element) {
            return element;
        }

        @Override
        public Publisher<Integer> createFailedPublisher() {
            return ReactiveStreams.<Integer>failed(new RuntimeException("failed"))
                .limit(Long.MAX_VALUE)
                .buildRs(getEngine());
        }
    }
}
