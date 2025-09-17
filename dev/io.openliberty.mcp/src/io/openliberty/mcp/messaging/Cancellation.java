/*******************************************************************************
 * Copyright (c) contributors to https://github.com/quarkiverse/quarkus-mcp-server
 * Copyright (c) 2025 IBM Corporation and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/Cancellation.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.messaging;

import java.util.Optional;

/**
 * Can be used to determine if an MCP client requested a cancellation of an in-progress request.
 * <p>
 * {@link Tool}, {@link Prompt} methods can accept this class as a parameter. It will be automatically injected before the
 * method is invoked.
 *
 * @see #check()
 */
public interface Cancellation {

    /**
     * Perform the check.
     * <p>
     * A feature method should throw an {@link OperationCancellationException} if cancellation was requested by the client.
     *
     * @return the result
     * @see Result#isRequested()
     * @see OperationCancellationException
     */
    Result check();

    /**
     * Perform the check and if cancellation is requested then skip the processing, i.e. throw
     * {@link OperationCancellationException}.
     *
     * @throws OperationCancellationException
     */
    default void skipProcessingIfCancelled() {
        if (check().isRequested()) {
            throw new OperationCancellationException();
        }
    }

    /**
     *
     * @param isRequested {@code true} if a client wants to cancel an in-progress request
     * @param reason an optional reason for cancellation
     */
    record Result(boolean isRequested, Optional<String> reason) {}

    /**
     * Exception indicating that the result of an MCP request cannot be returned because the request
     * was cancelled by the client.
     */
    class OperationCancellationException extends RuntimeException {

        private static final long serialVersionUID = 1L;

    }

}
