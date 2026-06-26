/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.mcp.monitor;

import com.ibm.websphere.monitor.jmx.Counter;
import com.ibm.websphere.monitor.jmx.StatisticsMeter;

/**
 * Management interface for MCP operation statistics.
 * <p>
 * This MXBean exposes monitoring information for MCP operations, including
 * operation attributes, invocation count, and total duration.
 * <p>
 * Each MXBean instance represents statistics for a unique set of MCP operation
 * attributes, such as method name, tool name, error type, and response status.
 * The values are intended to be consumed through JMX by administrators,
 * monitoring tools, and applications that need to observe MCP server behavior.
 */
public interface McpOperationStatsMXBean {

    /**
     * Returns the MCP JSON-RPC method name associated with this operation.
     * This value is always present as it is a required attribute.
     *
     * @return the MCP method name, for example {@code tools/call}
     */
    String getMcpMethodName();

    /**
     * Returns the error type associated with this operation.
     *
     * @return the error type, or {@code null} if no error type was recorded.
     */
    String getErrorType();

    /**
     * Returns the GenAI prompt name associated with this operation.
     *
     * @return the GenAI prompt name, or {@code null} if not applicable.
     */
    String getGenAiPromptName();

    /**
     * Returns the GenAI tool name associated with this operation.
     *
     * @return the GenAI tool name, or {@code null} if not applicable.
     */
    String getGenAiToolName();

    /**
     * Returns the JSON-RPC response status code associated with this operation.
     *
     * @return the JSON-RPC response status code, or {@code null} if unavailable.
     */
    String getRpcResponseStatusCode();

    /**
     * Returns the GenAI operation name associated with this operation.
     *
     * @return the GenAI operation name, or {@code null} if unavailable.
     */
    String getGenAiOperationName();

    /**
     * Returns the JSON-RPC protocol version associated with this operation.
     *
     * @return the JSON-RPC protocol version, or {@code null} if unavailable.
     */
    String getJsonrpcProtocolVersion();

    /**
     * Returns the MCP protocol version associated with this operation.
     *
     * @return the MCP protocol version, or {@code null} if unavailable.
     */
    String getMcpProtocolVersion();

    /**
     * Returns the network protocol name used for this operation.
     *
     * @return the network protocol name, or {@code null} if unavailable.
     */
    String getNetworkProtocolName();

    /**
     * Returns the network protocol version used for this operation.
     *
     * @return the network protocol version, or {@code null} if unavailable.
     */
    String getNetworkProtocolVersion();

    /**
     * Returns the network transport used for this operation.
     *
     * @return the network transport, or {@code null} if unavailable.
     */
    String getNetworkTransport();

    /**
     * Returns the MCP resource URI associated with this operation.
     *
     * @return the MCP resource URI, or {@code null} if unavailable.
     */
    String getMcpResourceUri();

    /**
     * Returns the number of times this operation has been recorded.
     *
     * @return the operation count.
     */
    long getCount();

    /**
     * Returns more detail about the count of recorded operation executions
     *
     * @return operation count details
     */
    Counter getCountDetails();

    /**
     * Returns the total recorded duration for this operation.
     * <p>
     * The duration is reported in nanoseconds.
     *
     * @return the total operation duration in nanoseconds.
     */
    double getDuration();

    /**
     * Returns statistical details on the duration the recorded executions of this operation.
     *
     * @return statistical details about the duration of executions of this operation
     */
    StatisticsMeter getDurationDetails();
}
