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
package io.openliberty.mcp.internal.features;

import java.time.Instant;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.openliberty.mcp.internal.features.FeatureManager.FeatureInfo;
import io.openliberty.mcp.messaging.Cancellation;
import io.openliberty.mcp.meta.Meta;
import jakarta.json.JsonObject;

/**
 *
 * @param <INFO>
 */
public interface FeatureManager<INFO extends FeatureInfo> extends Iterable<INFO> {

    /**
     *
     */
    interface FeatureInfo extends Comparable<FeatureInfo> {

        /**
         * It is guaranteed that the name is unique for a specific feature.
         *
         * @return the name
         */
        String name();

        String description();

// Not yet implemented:
//        /**
//         * @return the name of the respective server configuration
//         * @see McpServer
//         */
//        String serverName();

        /**
         * @return {@code true} if backed by a business method of a CDI bean, {@code false} otherwise
         */
        boolean isMethod();

        /**
         * @return the timestamp this feature was registered
         */
        Instant createdAt();

        @Override
        default int compareTo(FeatureInfo o) {
            // Sort by timestamp and name asc
            int result = createdAt().compareTo(o.createdAt());
            return result == 0 ? name().compareTo(o.name()) : result;
        }

        JsonObject asJson();

    }

    interface FeatureDefinition<INFO extends FeatureInfo, ARGUMENTS extends FeatureArguments, RESPONSE, THIS extends FeatureDefinition<INFO, ARGUMENTS, RESPONSE, THIS>> {

        /**
         *
         * @param description
         * @return self
         */
        THIS setDescription(String description);

// Not yet implemented
//        /**
//         *
//         * @param serverName
//         * @return self
//         * @see McpServer
//         */
//        THIS setServerName(String serverName);

        /**
         *
         * @param fun
         * @return self
         */
        THIS setHandler(Function<ARGUMENTS, RESPONSE> fun);

        /**
         *
         * @param fun
         * @return self
         */
        THIS setAsyncHandler(Function<ARGUMENTS, CompletionStage<RESPONSE>> fun);

        /**
         * Registers the resulting info and sends notifications to all connected clients.
         *
         * @return the info
         */
        INFO register();
    }

    interface RequestFeatureArguments extends FeatureArguments {

// Not yet implemented:
//        RequestId requestId();
//
//        Progress progress();

        Cancellation cancellation();

    }

    interface FeatureArguments {

// Not yet implemented:
//        McpConnection connection();
//
//        McpLog log();
//
//        Roots roots();
//
//        Sampling sampling();
//
//        Elicitation elicitation();
//
//        RawMessage rawMessage();
//
        Meta meta();

    }

}
