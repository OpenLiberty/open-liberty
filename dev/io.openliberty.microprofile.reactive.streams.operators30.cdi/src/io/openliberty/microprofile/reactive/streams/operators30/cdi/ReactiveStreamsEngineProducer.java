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

import java.util.Iterator;
import java.util.ServiceLoader;

import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

/*
 * This class is used for creating the Injected ReactiveStreamEngine instances.
 */
@Dependent
public class ReactiveStreamsEngineProducer {

    private final static TraceComponent tc = Tr.register(ReactiveStreamsEngineProducer.class);

    @Produces
    @ApplicationScoped
    public ReactiveStreamsEngine getEngine() {
        Iterator<ReactiveStreamsEngine> engines = ServiceLoader.load(ReactiveStreamsEngine.class).iterator();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Engines has next came out at " + engines.hasNext());
        }

        if (engines.hasNext()) {
            return engines.next();
        } else {
            //TODO NLS enable message
            throw new IllegalStateException(Tr.formatMessage(tc, "Unable to ServiceLoad ReactiveStreamsEngine from Producer."));
        }

    }

    /**
     * The CDI container will call this method once the injected
     * ReactiveStreamsEngine goes out of scope
     */
    public void closeReactiveStreamsEngine(@Disposes ReactiveStreamsEngine context) {
        // Left for now
    }

}
