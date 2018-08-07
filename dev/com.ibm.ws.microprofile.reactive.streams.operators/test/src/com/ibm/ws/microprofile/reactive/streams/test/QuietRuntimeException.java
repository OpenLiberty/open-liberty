/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.streams.test;

/**
 * RuntimeException with no stack trace for expected failures, to make logging not so noisy.
 */
public class QuietRuntimeException extends RuntimeException {
    /**  */
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
