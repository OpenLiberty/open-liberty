/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.util.Optional;

import org.mcpjava.server.Cancellation;

/**
 * Implementation of {@link Cancellation}. Code which has access to the implementation can call {@link #cancel(String)}
 * to indicate the request is cancelled.
 */
public class CancellationImpl implements Cancellation {

    private static final Result NOT_CANCELLED = new ResultImpl(false, null);

    private volatile Result result = NOT_CANCELLED;

    /**
     * Check if the Request has been cancelled
     */
    @Override
    public Result check() {
        return result;
    }

    /**
     * Cancels the request with a provided reason.
     *
     * @param reason the reason for cancellation, may be {@code null} to not provide a reason
     */
    public void cancel(String reason) {
        this.result = new ResultImpl(true, reason);
    }

    public static record ResultImpl(boolean isRequested, String reasonValue) implements Cancellation.Result {
        @Override
        public Optional<String> reason() {
            return Optional.ofNullable(reasonValue);
        }
    };

}
