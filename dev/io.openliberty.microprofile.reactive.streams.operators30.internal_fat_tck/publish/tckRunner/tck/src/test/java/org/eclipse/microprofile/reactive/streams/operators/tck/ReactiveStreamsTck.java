/*******************************************************************************
 * Copyright (c) 2018, 2023 Contributors to the Eclipse Foundation
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
//This file is based on https://github.com/eclipse/microprofile-reactive-streams-operators/blob/3.0/tck/src/main/java/org/eclipse/microprofile/reactive/streams/operators/tck/ReactiveStreamsTck.java
//It has been modified to allow the ScheduledExecutorService to be provided by an extending class.

package org.eclipse.microprofile.reactive.streams.operators.tck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.operators.tck.api.ReactiveStreamsApiVerification;
import org.eclipse.microprofile.reactive.streams.operators.tck.spi.ReactiveStreamsSpiVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Factory;

/**
 * The Reactive Streams TCK.
 * <p>
 * A concrete class that extends this class is all that is needed to verify a {@link ReactiveStreamsEngine} against this
 * TCK.
 * <p>
 * It produces a number of TestNG test classes via the TestNG {@link Factory} annotated {@link #allTests()} method.
 *
 * @param <E>
 *            The type of the Reactive Streams engine.
 */
public abstract class ReactiveStreamsTck<E extends ReactiveStreamsEngine> {

    private final TestEnvironment testEnvironment;
    private E engine;
    private ReactiveStreamsFactory rs;
    private ScheduledExecutorService executorService;

    public ReactiveStreamsTck(TestEnvironment testEnvironment) {
        this.testEnvironment = testEnvironment;
    }

    /**
     * Override to provide the reactive streams engine.
     */
    protected abstract E createEngine();

    /**
     * Create the reactive streams factory to use. By default, will use one backed by the ReactiveStreams static factory
     * methods, that is, using the ServiceLoader to locate one.
     */
    protected ReactiveStreamsFactory createFactory() {
        return new DefaultReactiveStreamsFactory();
    }

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
        if (executorService != null) {
            shutdownExecutorService(executorService);
        }
    }
    
    protected ScheduledExecutorService createExecutorService() {
        return Executors.newScheduledThreadPool(4);
    }
    
    protected void shutdownExecutorService(ScheduledExecutorService executorService) {
        executorService.shutdown();
    }

    @Factory
    public Object[] allTests() {
        engine = createEngine();
        rs = createFactory();
        executorService = createExecutorService();

        ReactiveStreamsApiVerification apiVerification = new ReactiveStreamsApiVerification(rs);
        ReactiveStreamsSpiVerification spiVerification =
                new ReactiveStreamsSpiVerification(testEnvironment, rs, engine, executorService);

        // Add tests that aren't dependent on the dependencies.
        List<Object> allTests = new ArrayList<>();

        allTests.addAll(apiVerification.allTests());
        allTests.addAll(spiVerification.allTests());

        return allTests.stream().filter(this::isEnabled).toArray();
    }
}