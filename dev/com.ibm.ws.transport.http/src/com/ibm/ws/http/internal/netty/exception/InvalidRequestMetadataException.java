/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.internal.netty.exception;

/**
 * Indicates that request protocol or HTTP/2 stream metadata is inconsistent.
 * This must not extend {@link IllegalArgumentException}; the dispatcher defers that
 * exception type to the decoded request.
 */
public final class InvalidRequestMetadataException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidRequestMetadataException(String message) {
        super(message);
    }

    public InvalidRequestMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
