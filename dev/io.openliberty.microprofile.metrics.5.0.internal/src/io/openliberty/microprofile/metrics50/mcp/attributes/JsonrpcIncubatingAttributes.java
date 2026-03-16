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
package io.openliberty.microprofile.metrics50.mcp.attributes;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

// DO NOT EDIT, this is an Auto-generated file from
// buildscripts/templates/registry/incubating_java/IncubatingSemanticAttributes.java.j2
@SuppressWarnings("unused")
public final class JsonrpcIncubatingAttributes {
    /**
     * Protocol version, as specified in the {@code jsonrpc} property of the request and its
     * corresponding response.
     */
    public static final AttributeKey<String> JSONRPC_PROTOCOL_VERSION = stringKey("jsonrpc.protocol.version");

    /**
     * A string representation of the {@code id} property of the request and its corresponding
     * response.
     *
     * <p>Notes:
     *
     * <p>Under the <a href="https://www.jsonrpc.org/specification">JSON-RPC specification</a>, the
     * {@code id} property may be a string, number, null, or omitted entirely. When omitted, the
     * request is treated as a notification. Using {@code null} is not equivalent to omitting the
     * {@code id}, but it is discouraged. Instrumentations SHOULD NOT capture this attribute when the
     * {@code id} is {@code null} or omitted.
     */
    public static final AttributeKey<String> JSONRPC_REQUEST_ID = stringKey("jsonrpc.request.id");

    // Enum definitions

    private JsonrpcIncubatingAttributes() {
    }
}
