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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/Encoder.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.encoders;

/**
 * @param <TYPE> The type to be encoded
 * @param <ENCODED> The resulting type of encoding
 */
public interface Encoder<TYPE, ENCODED> {

    /**
     *
     * @param runtimeType The runtime class of an object that should be encoded, must not be {@code null}
     * @return {@code true} if this encoder can encode the provided type, {@code false} otherwise
     */
    boolean supports(Class<?> runtimeType);

    /**
     *
     * @param value
     * @return the encoded value
     */
    ENCODED encode(TYPE value);

}
