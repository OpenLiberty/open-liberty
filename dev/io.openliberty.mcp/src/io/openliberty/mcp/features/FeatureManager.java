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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/FeatureManager.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.features;

import java.time.Instant;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.mcpjava.server.Cancellation;
import org.mcpjava.server.McpRequest;
import org.mcpjava.server.MetaCarrier;
import org.mcpjava.server.progress.Progress;

import io.openliberty.mcp.features.FeatureManager.FeatureInfo;
import io.openliberty.mcp.tools.ToolManager;

/**
 * A manager of MCP features, e.g. {@linkplain ToolManager tools}.
 *
 * @param <INFO> the type the features managed by this manager
 */
public interface FeatureManager<INFO extends FeatureInfo> extends Iterable<INFO> {

    /**
     * Provides information about an MCP feature, e.g. a {@linkplain ToolManager.ToolInfo tool}.
     */
    interface FeatureInfo extends Comparable<FeatureInfo>, MetaCarrier {

        /**
         * The name, which is unique within the manager holding this feature.
         *
         * @return the name
         */
        String name();

        /**
         * The description of the feature
         *
         * @return the description
         */
        String description();

        /**
         * Whether this feature is implemented by an annotated method.
         *
         * @return {@code true} if backed by a business method of a CDI bean, {@code false} otherwise
         */
        boolean isMethod();

        /**
         * The time that the feature was created
         *
         * @return the timestamp this feature was registered
         */
        Instant createdAt();

        @Override
        default int compareTo(FeatureInfo o) {
            // Sort by timestamp and name asc
            int result = createdAt().compareTo(o.createdAt());
            return result == 0 ? name().compareTo(o.name()) : result;
        }
    }

    /**
     * A builder for registering a new feature with the manager.
     *
     * @param <INFO> the feature type
     * @param <ARGUMENTS> the argument type accepted by handlers for this feature
     * @param <RESPONSE> the response type returned by handlers for this feature
     * @param <THIS> this builder
     */
    interface FeatureDefinition<INFO extends FeatureInfo, ARGUMENTS extends FeatureArguments, RESPONSE, THIS extends FeatureDefinition<INFO, ARGUMENTS, RESPONSE, THIS>> extends MetaCarrier.Builder<THIS> {

        /**
         * Sets the description for the feature
         *
         * @param description the description
         * @return self this builder
         */
        THIS setDescription(String description);

        /**
         * Sets the function which handles calls to this feature.
         * <p>
         * Only one of {@link #setHandler(Function)} and {@link #setAsyncHandler(Function)} may be called.
         *
         * @param fun the handler function
         * @return self this builder
         */
        THIS setHandler(Function<ARGUMENTS, RESPONSE> fun);

        /**
         * Sets the function which handles calls to this feature asynchronously.
         * <p>
         * Only one of {@link #setHandler(Function)} and {@link #setAsyncHandler(Function)} may be called.
         *
         * @param fun the asynchronous handler function
         * @return self this builder
         */
        THIS setAsyncHandler(Function<ARGUMENTS, CompletionStage<RESPONSE>> fun);

        /**
         * Creates the feature and registers it with the manager.
         *
         * @return the newly registered feature
         */
        INFO register();
    }

    /**
     * The input to a handler function, providing information about the request.
     */
    interface RequestFeatureArguments extends FeatureArguments {

        /**
         * Provides common information about the request from the client
         *
         * @return information about the request
         */
        McpRequest request();

        /**
         * Allows the handler to report its progress in handling the request.
         * <p>
         * Progress reporting is not currently supported and this method returns a no-op object.
         *
         * @return an object to use to report progress
         */
        Progress progress();

        /**
         * Allows the handler to detect if the user cancels the request.
         *
         * @return the cancellation object
         */
        Cancellation cancellation();

    }

    interface FeatureArguments {

    }

}
