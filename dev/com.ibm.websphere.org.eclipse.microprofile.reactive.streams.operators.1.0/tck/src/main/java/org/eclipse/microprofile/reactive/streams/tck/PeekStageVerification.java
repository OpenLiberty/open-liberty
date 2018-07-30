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
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.testng.Assert.assertEquals;

public class PeekStageVerification extends AbstractStageVerification {

    PeekStageVerification(ReactiveStreamsTck.VerificationDeps deps) {
        super(deps);
    }

    @Test
    public void peekStageShouldNotModifyElements() {
        AtomicInteger count = new AtomicInteger();
        assertEquals(await(ReactiveStreams.of(1, 2, 3)
            .peek(i -> count.incrementAndGet())
            .toList()
            .run(getEngine())), Arrays.asList(1, 2, 3));
        assertEquals(count.get(), 3);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void peekStageShouldPropagateRuntimeExceptions() {
        await(ReactiveStreams.of("foo")
            .peek(x -> {
                throw new RuntimeException("failed");
            })
            .toList()
            .run(getEngine()));
    }

    @Override
    List<Object> reactiveStreamsTckVerifiers() {
        return Collections.singletonList(
            new ProcessorVerification()
        );
    }

    public class ProcessorVerification extends StageProcessorVerification<Integer> {

        private Consumer<Integer> noop = x -> {
        };

        @Override
        public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
            return ReactiveStreams.<Integer>builder().peek(noop).buildRs(getEngine());
        }

        @Override
        public Publisher<Integer> createFailedPublisher() {
            return ReactiveStreams.<Integer>failed(new RuntimeException("failed"))
                .peek(noop).buildRs(getEngine());
        }

        @Override
        public Integer createElement(int element) {
            return element;
        }
    }
}
