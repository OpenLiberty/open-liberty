/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The initial set of unit test material was heavily derived from
 * tests at https://github.com/eclipse/microprofile-reactive
 * by James Roper.
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test;

/**
 * RuntimeException with no stack trace for expected failures, to make logging not so noisy.
 */
public class QuietRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QuietRuntimeException() {
        this(null, null);
    }

    public QuietRuntimeException(String message) {
        this(message, null);
    }

    public QuietRuntimeException(String message, Throwable cause) {
        super(message, cause, true, false);
    }

    public QuietRuntimeException(Throwable cause) {
        this(null, cause);
    }
}
