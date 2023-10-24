/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.test;

import java.util.concurrent.ForkJoinPool;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.spi.ExecutionModel;

/**
 * The concurrency execution model for tests which uses the common ForkJoin pool
 */
public class TestExecutionModel implements ExecutionModel {

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public Multi apply(Multi t) {
        return t.runSubscriptionOn(ForkJoinPool.commonPool());
    }

}
