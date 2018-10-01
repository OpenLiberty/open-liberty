/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.cdi;

import java.util.Iterator;
import java.util.ServiceLoader;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

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
