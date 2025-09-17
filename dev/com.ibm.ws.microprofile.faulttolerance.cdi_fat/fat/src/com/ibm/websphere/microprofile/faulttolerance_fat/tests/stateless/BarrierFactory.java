/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * <pre>
 * <code>
 * try (BarrierFactory bf = new BarrierFactory()) {
 *     Barrier b = bf.create();
 *     Future f = runAsync(bean.run(b));
 *     assertNotComplete(f);
 *     b.complete();
 *     assertCompletes(f);
 * }
 * </code>
 * </pre>
 *
 */
public class BarrierFactory implements AutoCloseable {

    /** Timeout for awaiting barriers, should be longer than any test requires */
    private static final int TIMEOUT = 20000;
    /** Timeout for waiting for await() to be called on a barrier */
    private static final int COMPLETION_TIMEOUT = 5000;

    private final List<Barrier> barriers = new ArrayList<>();
    private final List<Throwable> barrierErrors = new ArrayList<>();

    @Override
    public void close() {
        for (Barrier barrier : barriers) {
            barrier.complete();
        }
        assertThat("Barriers encountered exceptions while waiting, test is invalid", barrierErrors, empty());
    }

    public Barrier create() {
        Barrier barrier = new Barrier();
        barriers.add(barrier);
        return barrier;
    }

    private void addFailure(Throwable t) {
        barrierErrors.add(t);
    }

    public class Barrier {

        /** Completes when complete() is called */
        private final CompletableFuture<Void> future = new CompletableFuture<>();
        /** Completes when await() is first called */
        private final CompletableFuture<Void> awaited = new CompletableFuture<>();

        private Barrier() {
        }

        public void await() {
            try {
                awaited.complete(null);
                future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (Error t) {
                addFailure(t);
                throw t;
            } catch (RuntimeException e) {
                addFailure(e);
                throw e;
            } catch (Throwable t) {
                addFailure(t);
                throw new RuntimeException(t);
            }
        }

        public void assertAwaited() {
            try {
                awaited.get(COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (Error t) {
                throw t;
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void complete() {
            future.complete(null);
        }
    }

}
