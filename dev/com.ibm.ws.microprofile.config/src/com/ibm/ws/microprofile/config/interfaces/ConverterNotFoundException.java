/*******************************************************************************
 * Copyright (c) 2016 IBM Corp. and others
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
 * Thrown when a converter is not found for a specified type
 */
public class ConverterNotFoundException extends ConfigException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public ConverterNotFoundException(String message) {
        super(message);
    }

    /**
     * @param throwable
     */
    public ConverterNotFoundException(Throwable throwable) {
        super(throwable);
    }

    /**
     * @param message
     * @param throwable
     */
    public ConverterNotFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
