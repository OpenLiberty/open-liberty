/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.netty.handler.codec.http;

/** Identifies an expected validation rejection while decoding an HTTP request. */
public final class HttpRequestValidationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    HttpRequestValidationException(IllegalArgumentException cause) {
        super(cause.getMessage(), cause);
    }
}
