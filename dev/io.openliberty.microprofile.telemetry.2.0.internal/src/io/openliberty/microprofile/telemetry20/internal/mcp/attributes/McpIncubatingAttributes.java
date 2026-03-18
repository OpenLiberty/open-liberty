/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.mcp.attributes;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

// DO NOT EDIT, this is an Auto-generated file from
// buildscripts/templates/registry/incubating_java/IncubatingSemanticAttributes.java.j2
@SuppressWarnings("unused")
public final class McpIncubatingAttributes {
    /** The name of the request or notification method. */
    public static final AttributeKey<String> MCP_METHOD_NAME = stringKey("mcp.method.name");

    /**
     * The <a href="https://modelcontextprotocol.io/specification/versioning">version</a> of the Model
     * Context Protocol used.
     */
    public static final AttributeKey<String> MCP_PROTOCOL_VERSION = stringKey("mcp.protocol.version");

    /**
     * The value of the resource uri.
     *
     * <p>Notes:
     *
     * <p>This is a URI of the resource provided in the following requests or notifications: {@code
     * resources/read}, {@code resources/subscribe}, {@code resources/unsubscribe}, or {@code
     * notifications/resources/updated}.
     */
    public static final AttributeKey<String> MCP_RESOURCE_URI = stringKey("mcp.resource.uri");

    /**
     * Identifies <a
     * href="https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#session-management">MCP
     * session</a>.
     */
    public static final AttributeKey<String> MCP_SESSION_ID = stringKey("mcp.session.id");

    // Enum definitions

    /** Values for {@link #MCP_METHOD_NAME}. */
    public static final class McpMethodNameIncubatingValues {
        /** Notification cancelling a previously-issued request. */
        public static final String NOTIFICATIONS_CANCELLED = "notifications/cancelled";

        /** Request to initialize the MCP client. */
        public static final String INITIALIZE = "initialize";

        /** Notification indicating that the MCP client has been initialized. */
        public static final String NOTIFICATIONS_INITIALIZED = "notifications/initialized";

        /** Notification indicating the progress for a long-running operation. */
        public static final String NOTIFICATIONS_PROGRESS = "notifications/progress";

        /** Request to check that the other party is still alive. */
        public static final String PING = "ping";

        /** Request to list resources available on server. */
        public static final String RESOURCES_LIST = "resources/list";

        /** Request to list resource templates available on server. */
        public static final String RESOURCES_TEMPLATES_LIST = "resources/templates/list";

        /** Request to read a resource. */
        public static final String RESOURCES_READ = "resources/read";

        /** Notification indicating that the list of resources has changed. */
        public static final String NOTIFICATIONS_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

        /** Request to subscribe to a resource. */
        public static final String RESOURCES_SUBSCRIBE = "resources/subscribe";

        /** Request to unsubscribe from resource updates. */
        public static final String RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

        /** Notification indicating that a resource has been updated. */
        public static final String NOTIFICATIONS_RESOURCES_UPDATED = "notifications/resources/updated";

        /** Request to list prompts available on server. */
        public static final String PROMPTS_LIST = "prompts/list";

        /** Request to get a prompt. */
        public static final String PROMPTS_GET = "prompts/get";

        /** Notification indicating that the list of prompts has changed. */
        public static final String NOTIFICATIONS_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

        /** Request to list tools available on server. */
        public static final String TOOLS_LIST = "tools/list";

        /** Request to call a tool. */
        public static final String TOOLS_CALL = "tools/call";

        /** Notification indicating that the list of tools has changed. */
        public static final String NOTIFICATIONS_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

        /** Request to set the logging level. */
        public static final String LOGGING_SET_LEVEL = "logging/setLevel";

        /** Notification indicating that a message has been received. */
        public static final String NOTIFICATIONS_MESSAGE = "notifications/message";

        /** Request to create a sampling message. */
        public static final String SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

        /** Request to complete a prompt. */
        public static final String COMPLETION_COMPLETE = "completion/complete";

        /** Request to list roots available on server. */
        public static final String ROOTS_LIST = "roots/list";

        /** Notification indicating that the list of roots has changed. */
        public static final String NOTIFICATIONS_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

        /** Request from the server to elicit additional information from the user via the client */
        public static final String ELICITATION_CREATE = "elicitation/create";

        private McpMethodNameIncubatingValues() {
        }
    }

    private McpIncubatingAttributes() {
    }
}
