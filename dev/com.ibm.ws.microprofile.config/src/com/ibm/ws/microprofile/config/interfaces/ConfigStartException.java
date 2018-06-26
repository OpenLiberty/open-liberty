/*******************************************************************************
 * Copyright (c) 2018 IBM Corp. and others
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
 *******************************************************************************/
package com.ibm.ws.microprofile.config.interfaces;

/**
 * A very specific exception to support the mp config 1.3 and mpMetrics/mpOpenAPI issue #3997
 * Thrown to indicate that there was a problem starting an internal config process
 */
public class ConfigStartException extends RuntimeException {

    /**
     * The exception was thrown when unable to process some config.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public ConfigStartException(String message) {
        super(message);
    }

    /**
     * @param throwable
     */
    public ConfigStartException(Throwable throwable) {
        super(throwable);
    }

    /**
     * @param message
     * @param throwable
     */
    public ConfigStartException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
