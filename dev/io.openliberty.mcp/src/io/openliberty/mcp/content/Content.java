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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/Content.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.content;

/**
 * A content provided to or from an LLM.
 */
public sealed interface Content permits TextContent, ImageContent, AudioContent {

    /**
     *
     * @return the type of the content
     */
    Type type();

    /**
     * @return the optional annotations
     */
    Annotations annotations();

    /**
     * Casts and returns this object as a text content, or throws an {@link IllegalArgumentException} if the content object does
     * not represent a {@link TextContent}.
     *
     * @return the text content
     */
    default TextContent asText() {
        throw new IllegalArgumentException("Not a text");
    }

    /**
     * Casts and returns this object as an image content, or throws an {@link IllegalArgumentException} if the content object
     * does not represent a {@link ImageContent}.
     *
     * @return the image content
     */
    default ImageContent asImage() {
        throw new IllegalArgumentException("Not an image");
    }

//    /**
//     * Casts and returns this object as an embedded resource content, or throws an {@link IllegalArgumentException} if the
//     * content object does not represent a {@link EmbeddedResource}.
//     *
//     * @return the resource
//     */
//    default EmbeddedResource asResource() {
//        throw new IllegalArgumentException("Not a resource");
//    }

    /**
     * Casts and returns this object as an audio content, or throws an {@link IllegalArgumentException} if the content object
     * does not represent a {@link AudioContent}.
     *
     * @return the audio content
     */
    default AudioContent asAudio() {
        throw new IllegalArgumentException("Not an audio");
    }

//    /**
//     * Casts and returns this object as a resource link, or throws an {@link IllegalArgumentException} if the content object
//     * does not represent a {@link ResourceLink}.
//     *
//     * @return the audio content
//     */
//    default ResourceLink asResourceLink() {
//        throw new IllegalArgumentException("Not a resource link");
//    }

    default String getType() {
        return type().toString().toLowerCase();
    }

    /**
     * @param audience (may be {@code null})
     * @param lastModified (may be {@code null})
     * @param priority (may be {@code null})
     */
    public record Annotations(Role audience, String lastModified, Double priority) {}

    enum Type {
        TEXT,
        IMAGE,
        RESOURCE,
        AUDIO,
        RESOURCE_LINK
    }

}
