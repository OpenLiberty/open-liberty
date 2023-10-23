/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.operators30.spi.impl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.spi.ExecutionModel;

/**
 * ExecutionModel for running in liberty
 * <p>
 * Service-loaded by mutiny, this ensures that reactive streams are run asynchronously on the managed executor service
 */
public class LibertyExecutionModel implements ExecutionModel {

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("rawtypes") // Interface uses raw types
    public Multi apply(Multi multi) {
        return multi.runSubscriptionOn(RsoContextExecutor.getInstance());
    }
}
