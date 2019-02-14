/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The initial set of unit test material was heavily derived from
 * tests at https://github.com/eclipse/microprofile-reactive
 * by James Roper.
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.streams.test;

import static junit.framework.Assert.assertEquals;

import java.io.PrintStream;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.junit.Test;

public class SetInstanceTest extends WASReactiveUT {

    @Test
    public void testSetEngineDoesNotAffectCallWithEngineParameter() {

        PrintStream out = System.out;
        WASReactiveStreamsEngineImplSubclass engine = new WASReactiveStreamsEngineImplSubclass();
        ReactiveStreamsEngineResolver.setInstance(engine);

        ProcessorBuilder<String, String> filterOut2 = ReactiveStreams.<String>builder()
                .filter(s -> !s.equalsIgnoreCase("2"));

        assertEquals(await(
                ReactiveStreams.of("1", "2", "3")
                        .peek(e -> out.println("Original Element:>" + e + "<"))
                        .via(filterOut2)
                        .peek(e -> out.println("Filtered value:>" + e + "<"))
                        .collect(Collectors.joining(", ")).run(getEngine())),
                "1, 3");

        assertEquals("BitString:falsefalsefalsefalse", engine.check());
    }

    @Test
    public void testCustomBuildCompletion() {

        PrintStream out = System.out;
        WASReactiveStreamsEngineImplSubclass engine = new WASReactiveStreamsEngineImplSubclass();
        ReactiveStreamsEngineResolver.setInstance(engine);

        ProcessorBuilder<String, String> filterOut2 = ReactiveStreams.<String>builder()
                .filter(s -> !s.equalsIgnoreCase("2"));

        assertEquals(await(
                ReactiveStreams.of("1", "2", "3")
                        .peek(e -> out.println("Original Element:>" + e + "<"))
                        .via(filterOut2)
                        .peek(e -> out.println("Filtered value:>" + e + "<"))
                        .collect(Collectors.joining(", ")).run()),
                "1, 3");

        assertEquals("BitString:falsefalsefalsetrue", engine.check());
    }

    @Test
    public void testSetFactoryWorks() {

        PrintStream out = System.out;
        WASReactiveStreamsFactoryImplSubclass factory = new WASReactiveStreamsFactoryImplSubclass();
        ReactiveStreamsFactoryResolver.setInstance(factory);

        ProcessorBuilder<String, String> filterOut2 = ReactiveStreams.<String>builder()
                .filter(s -> !s.equalsIgnoreCase("2"));

        assertEquals(await(
                ReactiveStreams.of("1", "2", "3")
                        .peek(e -> out.println("Original Element:>" + e + "<"))
                        .via(filterOut2)
                        .peek(e -> out.println("Filtered value:>" + e + "<"))
                        .collect(Collectors.joining(", ")).run()),
                "1, 3");

        assertEquals("BitString:truetruetrue", factory.check());
    }

}
