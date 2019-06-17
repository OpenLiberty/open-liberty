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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

@ApplicationScoped
public class ConcurrencyConfigBean {
    @Produces
    @ApplicationScoped
    @Named("securityAndAppContextExecutor")
    ManagedExecutor createExecutor(@ConfigProperty(name = "AppProducedExecutor.maxAsync", defaultValue = "1") Integer a, // Not defined in MP Config, so maxAsync=1
                                   @ConfigProperty(name = "AppProducedExecutor.maxQueued", defaultValue = "4") Integer q) { // MP Config sets maxQueued=2
        return ManagedExecutor.builder().maxAsync(a).maxQueued(q).propagated(ThreadContext.SECURITY, ThreadContext.APPLICATION).build();
    }

    // MicroProfile Context Propagation automatically shuts down ManagedExecutors when the application stops.
    // But even if the application writes its own disposer, it shouldn't get an error.
    void disposeExecutor(@Disposes @Named("securityAndAppContextExecutor") ManagedExecutor exec) {
        exec.shutdownNow();
    }

    @Produces
    @ApplicationScoped
    @Named("maxQueued3Executor")
    ManagedExecutor exec = ManagedExecutor.builder().maxAsync(1).build(); // MP Config defaults maxQueued to 3

    @Produces
    @ApplicationScoped
    @Named("incompleteStageForSecurityContextTests")
    CompletionStage<LinkedBlockingQueue<String>> getIncompleteFuture(@Named("securityAndAppContextExecutor") ManagedExecutor exec) {
        return exec.newIncompleteFuture();
    }
}
