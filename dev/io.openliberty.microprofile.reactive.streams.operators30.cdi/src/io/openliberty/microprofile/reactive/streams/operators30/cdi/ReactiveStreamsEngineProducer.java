/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.operators30.cdi;

import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

/*
 * This class is used for creating the Injected ReactiveStreamEngine instances.
 */
@Dependent
public class ReactiveStreamsEngineProducer {

    @Produces
    @ApplicationScoped
    public ReactiveStreamsEngine getEngine() {
        ReactiveStreamsEngine engine = ReactiveStreamsEngineResolver.instance();
        return engine;
    }

}
