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
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;

public class SubscriberStageVerification extends AbstractStageVerification {
    SubscriberStageVerification(ReactiveStreamsTck.VerificationDeps deps) {
        super(deps);
    }

    @Test
    public void subscriberStageShouldRedeemCompletionStageWhenCompleted() {
        CompletionStage<Void> result = ReactiveStreams.of().to(
            ReactiveStreams.builder().ignore().build(getEngine())
        ).run(getEngine());
        await(result);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void subscriberStageShouldRedeemCompletionStageWhenFailed() {
        CompletionStage<Void> result = ReactiveStreams.failed(new RuntimeException("failed")).to(
            ReactiveStreams.builder().ignore().build(getEngine())
        ).run(getEngine());
        await(result);
    }

    @Test(expectedExceptions = CancellationException.class)
    public void subscriberStageShouldRedeemCompletionStageWithCancellationExceptionWhenCancelled() {
        CompletionStage<Void> result = ReactiveStreams.fromPublisher(subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        })).to(
            ReactiveStreams.builder().cancel().build(getEngine())
        ).run(getEngine());
        await(result);
    }

    @Override
    List<Object> reactiveStreamsTckVerifiers() {
        return Collections.emptyList();
    }
}
