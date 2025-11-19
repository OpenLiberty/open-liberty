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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/Tool.java
 * Modifications have been made.
 *******************************************************************************/

package io.openliberty.mcp.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.CompletionStage;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.tools.ToolResponse;

/**
 * Annotates a business method of a CDI bean as an exposed tool.
 * <p>
 * A result of a "tool call" operation is always represented as a {@link ToolResponse}. However, the annotated method can also
 * return other types that are converted according to the following rules.
 * <p>
 * <ul>
 * <li>If it returns {@link String} then the response is {@code success} and contains a single {@link TextContent}.</li>
 * <li>If it returns an implementation of {@link Content} then the response is {@code success} and contains a single
 * content object.</li>
 * <li>If it returns a {@link List} of {@link Content} implementations or strings then the response is
 * {@code success} and contains a list of relevant content objects.</li>
 * <li>It may also return a {@link CompletionStage} that wraps any of the type mentioned above.</li>
 * </ul>
 *
 * <p>
 * There is a default content encoder registered; it encodes the returned value as JSON using JSON-B.
 *
 * @see ToolResponse
 * @see ToolArg
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Tool {

    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * Each tool must have a unique name.
     * <p>
     * Intended for programmatic or logical use, but used for UI in past specs or as fallback if title isn't present.
     * <p>
     * By default, the name is derived from the name of the annotated method.
     */
    String name() default ELEMENT_NAME;

    /**
     * A human-readable title for the tool.
     */
    String title() default "";

    /**
     * A human-readable description of the tool. A hint to the model.
     */
    String description() default "";

    /**
     * Additional hints for clients.
     * <p>
     * Note that the default value of this annotation member is ignored. In other words, the annotations have to be declared
     * explicitly in order to be included in Tool metadata.
     */
    Annotations annotations() default @Annotations;

    /**
     * If set to {@code true} and the method returns a type {@code X} which is not specifically treated (see the conversion
     * rules), then the return value is converted to JSON and used as a {@code structuredContent} of the result.
     * <p>
     * Also the output schema is generated automatically from the return type.
     */
    boolean structuredContent() default false;

    @Retention(RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    public @interface Annotations {

        /**
         * A human-readable title for the tool.
         */
        String title() default "";

        /**
         * If true, the tool does not modify its environment.
         */
        boolean readOnlyHint() default false;

        /**
         * If true, the tool may perform destructive updates to its environment. If false, the tool performs only additive
         * updates.
         */
        boolean destructiveHint() default true;

        /**
         * If true, calling the tool repeatedly with the same arguments will have no additional effect on the its environment.
         */
        boolean idempotentHint() default false;

        /**
         * If true, this tool may interact with an "open world" of external entities. If false, the tool's domain of interaction
         * is closed.
         */
        boolean openWorldHint() default true;

    }

}
