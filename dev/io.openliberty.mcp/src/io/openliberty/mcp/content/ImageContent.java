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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/ImageContent.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.content;

import java.util.Map;

import io.openliberty.mcp.meta.MetaKey;

/**
 * An image content provided to or from an LLM.
 *
 * @param data a base64-encoded string representing the image data (must not be {@code null})
 * @param mimeType the mime type of the image (must not be {@code null})
 * @param _meta the optional metadata
 */
public record ImageContent(String data, String mimeType, Map<MetaKey, Object> _meta) implements Content {

    public ImageContent(String data, String mimeType) {
        this(data, mimeType, null);
    }

    public ImageContent {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType must not be null");
        }
    }

    @Override
    public Type type() {
        return Type.IMAGE;
    }

    @Override
    public ImageContent asImage() {
        return this;
    }

}
