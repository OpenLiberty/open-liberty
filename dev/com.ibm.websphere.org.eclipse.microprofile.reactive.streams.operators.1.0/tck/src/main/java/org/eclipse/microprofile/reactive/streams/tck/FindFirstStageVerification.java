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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class FindFirstStageVerification extends AbstractStageVerification {

    FindFirstStageVerification(ReactiveStreamsTck.VerificationDeps deps) {
        super(deps);
    }

    @Test
    public void findFirstStageShouldFindTheFirstElement() {
        assertEquals(await(ReactiveStreams.of(1, 2, 3)
            .findFirst().run(getEngine())), Optional.of(1));
    }

    @Test
    public void findFirstStageShouldReturnEmpty() {
        assertEquals(await(ReactiveStreams.of()
            .findFirst().run(getEngine())), Optional.empty());
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void findFirstStageShouldPropagateErrors() {
        await(ReactiveStreams.failed(new RuntimeException("failed"))
            .findFirst().run(getEngine()));
    }

    @Override
    List<Object> reactiveStreamsTckVerifiers() {
        return Collections.singletonList(new SubscriberVerification());
    }

    class SubscriberVerification extends StageSubscriberBlackboxVerification<Integer> {
        @Override
        public Subscriber<Integer> createSubscriber() {
            return ReactiveStreams.<Integer>builder().findFirst().build(getEngine());
        }

        @Override
        public Integer createElement(int element) {
            return element;
        }
    }
}
