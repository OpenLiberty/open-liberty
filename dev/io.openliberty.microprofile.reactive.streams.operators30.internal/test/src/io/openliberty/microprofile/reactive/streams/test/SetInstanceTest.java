/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * The initial set of unit test material was heavily derived from
 * tests at https://github.com/eclipse/microprofile-reactive
 * by James Roper.
 ******************************************************************************/

package io.openliberty.microprofile.reactive.streams.test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintStream;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.junit.Test;

import io.openliberty.microprofile.reactive.streams.test.utils.TestReactiveStreamsEngine;
import io.openliberty.microprofile.reactive.streams.test.utils.TestReactiveStreamsFactory;

/**
 * Tests that we can call the serviceLoading relacement 'setInstance' methods
 */
public class SetInstanceTest extends AbstractReactiveUnitTest {

    /**
     * Test setEngine does not affect call with Engine parameter
     * AND that we make use of a user's custom reactive streams engine for building
     * Completions.
     */
    @Test
    public void testSetEngineDoesNotAffectCallWithEngineParameter() {

        PrintStream out = System.out;
        TestReactiveStreamsEngine secondaryEngine = new TestReactiveStreamsEngine();

        ProcessorBuilder<String, String> filterOut2 = ReactiveStreams.<String> builder()
                        .filter(s -> !s.equalsIgnoreCase("2"));

        assertEquals(await(
                           ReactiveStreams.of("1", "2", "3")
                                           .peek(e -> out.println("Original Element:>" + e + "<"))
                                           .via(filterOut2)
                                           .peek(e -> out.println("Filtered value:>" + e + "<"))
                                           .collect(Collectors.joining(", ")).run(secondaryEngine)),
                     "1, 3");

        assertFalse("primary buildPublisherCalled was called", getEngine().buildPublisherCalled());
        assertFalse("primary buildSubscriberCalled was called", getEngine().buildSubscriberCalled());
        assertFalse("primary buildProcessorCalled was called", getEngine().buildProcessorCalled());
        assertFalse("primary buildCompletionCalled was called", getEngine().buildCompletionCalled());

        assertFalse("secondary buildPublisherCalled was called", secondaryEngine.buildPublisherCalled());
        assertFalse("secondary buildSubscriberCalled was called", secondaryEngine.buildSubscriberCalled());
        assertFalse("secondary buildProcessorCalled was called", secondaryEngine.buildProcessorCalled());
        assertTrue("secondary buildCompletionCalled was not called", secondaryEngine.buildCompletionCalled());
    }

    /**
     * Test user can set a custom ReactiveStreamsFactory
     */
    @Test
    public void testSetFactoryWorks() {

        PrintStream out = System.out;
        TestReactiveStreamsFactory factory = new TestReactiveStreamsFactory();
        try {
            ReactiveStreamsFactoryResolver.setInstance(factory);

            ProcessorBuilder<String, String> filterOut2 = ReactiveStreams.<String> builder()
                            .filter(s -> !s.equalsIgnoreCase("2"));

            assertEquals(await(
                               ReactiveStreams.of("1", "2", "3")
                                               .peek(e -> out.println("Original Element:>" + e + "<"))
                                               .via(filterOut2)
                                               .peek(e -> out.println("Filtered value:>" + e + "<"))
                                               .collect(Collectors.joining(", ")).run()),
                         "1, 3");

            assertTrue("builderCalled was not called", factory.builderCalled());
            assertTrue("fromIterableCalled was not called", factory.fromIterableCalled());
            assertTrue("ofCalled was not called", factory.ofCalled());
        } finally {
            ReactiveStreamsFactoryResolver.setInstance(null);
        }
    }

}
