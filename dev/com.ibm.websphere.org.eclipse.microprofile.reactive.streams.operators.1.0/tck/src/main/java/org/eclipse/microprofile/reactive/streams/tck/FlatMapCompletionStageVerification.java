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
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.testng.Assert.assertEquals;

public class FlatMapCompletionStageVerification extends AbstractStageVerification {
    FlatMapCompletionStageVerification(ReactiveStreamsTck.VerificationDeps deps) {
        super(deps);
    }

    @Test
    public void flatMapCsStageShouldMapFutures() throws Exception {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        CompletableFuture<Integer> two = new CompletableFuture<>();
        CompletableFuture<Integer> three = new CompletableFuture<>();

        CompletionStage<List<Integer>> result = ReactiveStreams.of(one, two, three)
            .flatMapCompletionStage(Function.identity())
            .toList()
            .run(getEngine());

        Thread.sleep(100);

        one.complete(1);
        two.complete(2);
        three.complete(3);

        assertEquals(await(result), Arrays.asList(1, 2, 3));
    }

    @Test
    public void flatMapCsStageShouldMaintainOrderOfFutures() throws Exception {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        CompletableFuture<Integer> two = new CompletableFuture<>();
        CompletableFuture<Integer> three = new CompletableFuture<>();

        CompletionStage<List<Integer>> result = ReactiveStreams.of(one, two, three)
            .flatMapCompletionStage(Function.identity())
            .toList()
            .run(getEngine());

        three.complete(3);
        Thread.sleep(100);
        two.complete(2);
        Thread.sleep(100);
        one.complete(1);

        assertEquals(await(result), Arrays.asList(1, 2, 3));
    }

    @Test
    public void flatMapCsStageShouldOnlyMapOneElementAtATime() throws Exception {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        CompletableFuture<Integer> two = new CompletableFuture<>();
        CompletableFuture<Integer> three = new CompletableFuture<>();

        AtomicInteger concurrentMaps = new AtomicInteger(0);

        CompletionStage<List<Integer>> result = ReactiveStreams.of(one, two, three)
            .flatMapCompletionStage(i -> {
                assertEquals(1, concurrentMaps.incrementAndGet());
                return i;
            })
            .toList()
            .run(getEngine());

        Thread.sleep(100);
        concurrentMaps.decrementAndGet();
        one.complete(1);
        Thread.sleep(100);
        concurrentMaps.decrementAndGet();
        two.complete(2);
        Thread.sleep(100);
        concurrentMaps.decrementAndGet();
        three.complete(3);

        assertEquals(await(result), Arrays.asList(1, 2, 3));
    }

    @Override
    List<Object> reactiveStreamsTckVerifiers() {
        return Collections.singletonList(new ProcessorVerification());
    }

    public class ProcessorVerification extends StageProcessorVerification<Integer> {
        @Override
        public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
            return ReactiveStreams.<Integer>builder()
                .flatMapCompletionStage(CompletableFuture::completedFuture)
                .buildRs(getEngine());
        }

        @Override
        public Integer createElement(int element) {
            return element;
        }
    }
}
