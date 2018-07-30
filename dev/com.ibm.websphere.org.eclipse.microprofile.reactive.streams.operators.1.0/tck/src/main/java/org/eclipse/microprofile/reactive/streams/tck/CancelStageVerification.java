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
import org.reactivestreams.Subscription;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CancelStageVerification extends AbstractStageVerification {
    CancelStageVerification(ReactiveStreamsTck.VerificationDeps deps) {
        super(deps);
    }

    @Test
    public void cancelStageShouldCancelTheStage() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<Void> result = ReactiveStreams.fromPublisher(s -> s.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
                cancelled.complete(null);
            }
        })).cancel().run(getEngine());
        await(cancelled);
        await(result);
    }

    @Override
    List<Object> reactiveStreamsTckVerifiers() {
        return Collections.singletonList(new SubscriberVerification());
    }

    public class SubscriberVerification extends StageSubscriberBlackboxVerification {
        @Override
        public Subscriber createSubscriber() {
            return ReactiveStreams.builder().cancel().build(getEngine());
        }

        @Override
        public Object createElement(int element) {
            return element;
        }

        @Override
        public void required_spec201_blackbox_mustSignalDemandViaSubscriptionRequest() throws Throwable {
            throw new SkipException("Cancel subscriber does not need to signal demand.");
        }

        @Override
        public void required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable {
            throw new SkipException("Cancel subscriber does not need to signal demand.");
        }

        @Override
        public void required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable {
            throw new SkipException("Cancel subscriber does not need to signal demand.");
        }
    }
}
