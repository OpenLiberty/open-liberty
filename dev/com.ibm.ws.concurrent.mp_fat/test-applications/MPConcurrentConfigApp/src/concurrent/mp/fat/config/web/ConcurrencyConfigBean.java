/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.mp.fat.config.web;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.NamedInstance;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConcurrencyConfigBean {
    @Produces
    @ApplicationScoped
    @NamedInstance("applicationProducedExecutor")
    ManagedExecutor createExecutor(@ConfigProperty(name = "AppProducedExecutor.maxAsync", defaultValue = "1") Integer a,
                                   @ConfigProperty(name = "AppProducedExecutor.maxQueued", defaultValue = "4") Integer q) { // MP Config sets maxQueued=2
        return ManagedExecutor.builder().maxAsync(a).maxQueued(q).build();
    }

    // MicroProfile Concurrency automatically shuts down ManagedExecutors when the application stops.
    // But even if the application writes its own disposer, it shouldn't get an error.
    void disposeExecutor(@Disposes @NamedInstance("applicationProducedExecutor") ManagedExecutor exec) {
        System.out.println("### disposer");
        exec.shutdownNow();
    }

    @Produces
    @ApplicationScoped
    @NamedInstance("containerExecutorReturnedByAppProducer")
    ManagedExecutor getExecutor(@ManagedExecutorConfig(maxAsync = 1, maxQueued = 1) ManagedExecutor exec) { // MP Config sets maxQueued=3
        return exec;
    }
}
