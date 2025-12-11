/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

/**
 * Tests if CDI bean method with Observes Startup can access an injected
 * Concurrency resource.
 */
@ApplicationScoped
public class OnStartup {
    private final CompletableFuture<String> result = new CompletableFuture<>();

    @Inject
    private ManagedScheduledExecutorService scheduler;

    private void complete() {
        result.complete("SUCCESS");
    }

    String getResult(long timeout, TimeUnit unit) throws ExecutionException, //
                    InterruptedException, TimeoutException {
        return result.get(timeout, unit);
    }

    public void init(@Observes Startup event) {
        System.out.println("Startup: scheduling to run 3 seconds from now");
        try {
            ScheduledFuture<?> future = scheduler.schedule(this::complete,
                                                           3,
                                                           TimeUnit.SECONDS);
            System.out.println("scheduled " + future);
        } catch (RuntimeException | Error x) {
            x.printStackTrace(System.out);
            throw x;
        }
    }
}
