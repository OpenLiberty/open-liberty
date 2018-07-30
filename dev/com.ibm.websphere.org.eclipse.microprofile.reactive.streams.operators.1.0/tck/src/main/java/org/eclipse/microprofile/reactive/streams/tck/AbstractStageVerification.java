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
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.IdentityProcessorVerification;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract class AbstractStageVerification {

    private final ReactiveStreamsEngine engine;
    private final TestEnvironment environment;
    private final ScheduledExecutorService executorService;

    AbstractStageVerification(ReactiveStreamsTck.VerificationDeps deps) {
        this.engine = deps.engine();
        this.environment = deps.testEnvironment();
        this.executorService = deps.executorService();
    }

    ReactiveStreamsEngine getEngine() {
        return engine;
    }

    ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    abstract List<Object> reactiveStreamsTckVerifiers();

    <T> T await(CompletionStage<T> future) {
        try {
            return future.toCompletableFuture().get(environment.defaultTimeoutMillis(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            else {
                throw new RuntimeException(e.getCause());
            }
        }
        catch (TimeoutException e) {
            throw new RuntimeException("Future timed out after " + environment.defaultTimeoutMillis() + "ms", e);
        }
    }


    abstract class StagePublisherVerification<T> extends PublisherVerification<T> {

        StagePublisherVerification() {
            super(AbstractStageVerification.this.environment);
        }

        @Override
        public Publisher<T> createFailedPublisher() {
            return ReactiveStreams.<T>failed(new RuntimeException("failed")).buildRs(engine);
        }
    }

    abstract class StageProcessorVerification<T> extends IdentityProcessorVerification<T> {
        StageProcessorVerification() {
            super(AbstractStageVerification.this.environment);
        }

        @Override
        public ExecutorService publisherExecutorService() {
            return executorService;
        }

        @Override
        public Publisher<T> createFailedPublisher() {
            return ReactiveStreams.<T>failed(new RuntimeException("failed")).buildRs(engine);
        }

        @Override
        public long maxSupportedSubscribers() {
            return 1;
        }
    }

    abstract class StageSubscriberWhiteboxVerification<T> extends SubscriberWhiteboxVerification<T> {
        StageSubscriberWhiteboxVerification() {
            super(AbstractStageVerification.this.environment);
        }
    }

    abstract class StageSubscriberBlackboxVerification<T> extends SubscriberBlackboxVerification<T> {
        StageSubscriberBlackboxVerification() {
            super(AbstractStageVerification.this.environment);
        }
    }
}
