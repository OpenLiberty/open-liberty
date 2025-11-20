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

import io.openliberty.mcp.messaging.Cancellation;

/**
 * An implementation of the Cancellation interface which checks that the Result object has the reason field set
 * It returns the Result object indicating if a cancellation has been requested or not
 */
public class CancellationImpl implements Cancellation {

    private ExecutionRequestId requestId;
    private volatile Optional<String> reason = null;

    /**
     * Check if the Request has been cancelled
     */
    @Override
    public Result check() {
        if (requestId == null) {
            return new Result(false, Optional.empty());
        }
        if (reason == null) {
            return new Result(false, Optional.empty());
        }
        return new Result(true, reason);
    }

    public void setRequestId(ExecutionRequestId requestId) {
        this.requestId = requestId;
    }

    /**
     * Cancels the request with a provided reason.
     */
    public void cancel(Optional<String> reason) {
        this.reason = reason;
    }

}
