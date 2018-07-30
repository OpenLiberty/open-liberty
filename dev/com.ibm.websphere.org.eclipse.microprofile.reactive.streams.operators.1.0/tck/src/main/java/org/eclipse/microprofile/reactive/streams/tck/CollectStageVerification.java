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
import org.reactivestreams.Subscriber;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

public class CollectStageVerification extends AbstractStageVerification {

    CollectStageVerification(ReactiveStreamsTck.VerificationDeps deps) {
        super(deps);
    }

    @Test
    public void toListStageShouldReturnAList() {
        assertEquals(await(ReactiveStreams.of(1, 2, 3)
            .toList().run(getEngine())), Arrays.asList(1, 2, 3));
    }

    @Test
    public void toListStageShouldReturnEmpty() {
        assertEquals(await(ReactiveStreams.of()
            .toList().run(getEngine())), Collections.emptyList());
    }

    @Test
    public void collectShouldAccumulateResult() {
        assertEquals(await(ReactiveStreams.of(1, 2, 3)
            .collect(
                () -> new AtomicInteger(0),
                AtomicInteger::addAndGet
            ).run(getEngine())).get(), 6);
    }

    @Test
    public void collectShouldSupportEmptyStreams() {
        assertEquals(await(ReactiveStreams.<Integer>empty()
            .collect(
                () -> new AtomicInteger(42),
                AtomicInteger::addAndGet
            ).run(getEngine())).get(), 42);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void collectShouldPropagateErrors() {
        await(ReactiveStreams.<Integer>failed(new RuntimeException("failed"))
            .collect(
                () -> new AtomicInteger(0),
                AtomicInteger::addAndGet
            ).run(getEngine()));
    }


    @Test
    public void finisherFunctionShouldBeInvoked() {
        assertEquals(await(ReactiveStreams.of("1", "2", "3")
            .collect(Collectors.joining(", ")).run(getEngine())), "1, 2, 3");
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void toListStageShouldPropagateErrors() {
        await(ReactiveStreams.failed(new RuntimeException("failed"))
            .toList().run(getEngine()));
    }

    @Override
    List<Object> reactiveStreamsTckVerifiers() {
        return Arrays.asList(new ToListSubscriberVerification(), new CollectSubscriberVerification());
    }

    class ToListSubscriberVerification extends StageSubscriberBlackboxVerification<Integer> {
        @Override
        public Subscriber<Integer> createSubscriber() {
            return ReactiveStreams.<Integer>builder().toList().build(getEngine());
        }

        @Override
        public Integer createElement(int element) {
            return element;
        }
    }

    class CollectSubscriberVerification extends StageSubscriberBlackboxVerification<Integer> {
        @Override
        public Subscriber<Integer> createSubscriber() {
            return ReactiveStreams.<Integer>builder()
                .collect(
                    () -> new AtomicInteger(0),
                    AtomicInteger::addAndGet)
                .build(getEngine());
        }

        @Override
        public Integer createElement(int element) {
            return element;
        }
    }
}
