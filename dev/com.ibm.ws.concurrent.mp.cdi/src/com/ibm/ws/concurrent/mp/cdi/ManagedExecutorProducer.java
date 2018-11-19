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
package com.ibm.ws.concurrent.mp.cdi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ManagedExecutorProducer {

    private static final TraceComponent tc = Tr.register(ManagedExecutorProducer.class);

    // A new instance of this class will be constructed each time the producer is
    // called so we need to keep the config->executor map in a static variable
    // TODO: This needs to be scoped per-application
    private static final Map<ManagedExecutorConfigContainer, ManagedExecutor> configMap = new ConcurrentHashMap<>();

    @Produces
    public ManagedExecutor produceManagedExecutor(InjectionPoint point) {
        ManagedExecutorConfig configAnno = point.getAnnotated().getAnnotation(ManagedExecutorConfig.class);
        ManagedExecutorConfigContainer config = ManagedExecutorConfigContainer.get(configAnno);
        ManagedExecutor exec = configMap.computeIfAbsent(config, this::createManagedExecutor);
        return exec;
    }

    private ManagedExecutor createManagedExecutor(ManagedExecutorConfigContainer config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Building a new ManagedExecutor for the requested configuration: " + config);
        return ManagedExecutor.builder()
                        .maxAsync(config.maxAsync)
                        .maxQueued(config.maxQueued)
                        .propagated(config.propagated.toArray(new String[config.propagated.size()]))
                        .cleared(config.cleared.toArray(new String[config.cleared.size()]))
                        .build();
    }

}
