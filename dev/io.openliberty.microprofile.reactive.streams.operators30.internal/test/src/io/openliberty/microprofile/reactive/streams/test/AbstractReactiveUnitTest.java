/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.test;

import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import io.openliberty.microprofile.reactive.streams.operators30.spi.impl.LibertyReactiveStreamsEngineImpl;

/**
 * This is just a simple base class to put common function in.
 */
public abstract class AbstractReactiveUnitTest {

    /**
     * Runs before any tests in classes that inherit from us
     */
    @BeforeClass
    public static void setup() {
        ReactiveStreamsEngineResolver.setInstance(new LibertyReactiveStreamsEngineImpl());
        ReactiveStreamsFactoryResolver.setInstance(new ReactiveStreamsFactoryImpl());
    }

    /**
     * Runs after any tests in classes that inherit from us
     */
    @AfterClass
    public static void tearDown() {
        ReactiveStreamsEngineResolver.setInstance(null);
        ReactiveStreamsFactoryResolver.setInstance(null);
    }

}
