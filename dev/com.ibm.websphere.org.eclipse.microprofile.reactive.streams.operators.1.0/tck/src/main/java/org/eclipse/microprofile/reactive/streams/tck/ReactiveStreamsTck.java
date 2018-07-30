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

import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * The Reactive Streams TCK.
 * <p>
 * A concrete class that extends this class is all that is needed to verify a {@link ReactiveStreamsEngine} against
 * this TCK.
 * <p>
 * It produces a number of TestNG test classes via the TestNG {@link Factory} annotated {@link #allTests()} method.
 *
 * @param <E> The type of the Reactive Streams engine.
 */
public abstract class ReactiveStreamsTck<E extends ReactiveStreamsEngine> {

    private final TestEnvironment testEnvironment;
    private E engine;
    private ScheduledExecutorService executorService;

    public ReactiveStreamsTck(TestEnvironment testEnvironment) {
        this.testEnvironment = testEnvironment;
    }

    /**
     * Override to provide the reactive streams engine.
     */
    protected abstract E createEngine();

    /**
     * Override to implement custom shutdown logic for the Reactive Streams engine.
     */
    protected void shutdownEngine(E engine) {
        // By default, do nothing.
    }

    /**
     * Override this to disable/enable tests, useful for debugging one test at a time.
     */
    protected boolean isEnabled(Object test) {
        return true;
    }

    @AfterSuite(alwaysRun = true)
    public void shutdownEngine() {
        if (engine != null) {
            shutdownEngine(engine);
        }
    }

    @Factory
    public Object[] allTests() {
        engine = createEngine();
        executorService = Executors.newScheduledThreadPool(4);

        List<Function<VerificationDeps, AbstractStageVerification>> stageVerifications = Arrays.asList(
            OfStageVerification::new,
            MapStageVerification::new,
            FlatMapStageVerification::new,
            FilterStageVerification::new,
            FindFirstStageVerification::new,
            CollectStageVerification::new,
            TakeWhileStageVerification::new,
            FlatMapPublisherStageVerification::new,
            FlatMapCompletionStageVerification::new,
            FlatMapIterableStageVerification::new,
            ConcatStageVerification::new,
            EmptyProcessorVerification::new,
            CancelStageVerification::new,
            SubscriberStageVerification::new,
            PeekStageVerification::new,
            DistinctStageVerification::new,
            OnStagesVerification::new,
            LimitStageVerification::new,
            SkipStageVerification::new,
            DropWhileStageVerification::new,
            OnErrorResumeStageVerification::new
        );

        List<Object> allTests = new ArrayList<>();
        VerificationDeps deps = new VerificationDeps();
        for (Function<VerificationDeps, AbstractStageVerification> creator : stageVerifications) {
            AbstractStageVerification stageVerification = creator.apply(deps);
            allTests.add(stageVerification);
            allTests.addAll(stageVerification.reactiveStreamsTckVerifiers());
        }

        // Add tests that aren't dependent on the dependencies.
        allTests.add(new GraphAccessorVerification());

        return allTests.stream().filter(this::isEnabled).toArray();
    }

    class VerificationDeps {
        ReactiveStreamsEngine engine() {
            return engine;
        }

        TestEnvironment testEnvironment() {
            return testEnvironment;
        }

        ScheduledExecutorService executorService() {
            return executorService;
        }
    }

}
