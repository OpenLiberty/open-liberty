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
 * Management interface for MCP session statistics.
 * <p>
 * This MXBean exposes monitoring information for MCP sessions, including
 * protocol and network attributes, session count, and total session duration.
 * <p>
 * The values are intended to be consumed through JMX by administrators,
 * monitoring tools, and applications that need to observe MCP server behavior.
 */
public interface McpSessionStatsMXBean {

    /**
     * Returns the error type associated with the session.
     *
     * @return the error type, or {@code null} if no error type was recorded.
     */
    String getErrorType();

    /**
     * Returns the JSON-RPC protocol version associated with the session.
     *
     * @return the JSON-RPC protocol version, or {@code null} if unavailable.
     */
    String getJsonrpcProtocolVersion();

    /**
     * Returns the MCP protocol version associated with the session.
     *
     * @return the MCP protocol version, or {@code null} if unavailable.
     */
    String getMcpProtocolVersion();

    /**
     * Returns the network protocol name used for the session.
     *
     * @return the network protocol name, or {@code null} if unavailable.
     */
    String getNetworkProtocolName();

    /**
     * Returns the network protocol version used for the session.
     *
     * @return the network protocol version, or {@code null} if unavailable.
     */
    String getNetworkProtocolVersion();

    /**
     * Returns the network transport used for the session.
     *
     * @return the network transport, or {@code null} if unavailable.
     */
    String getNetworkTransport();

    /**
     * Returns the number of sessions recorded for this set of attributes.
     *
     * @return the session count.
     */
    long getCount();

    /**
     * Returns more detail about the session count
     *
     * @return session count details
     */
    Counter getCountDetails();

    /**
     * Returns the total recorded session duration.
     * <p>
     * The duration is reported in nanoseconds.
     *
     * @return the total session duration in nanoseconds.
     */
    double getDuration();

    /**
     * Returns statistical details on the duration of sessions.
     *
     * @return statistical details about the duration sessions
     */
    public StatisticsMeter getDurationDetails();
}
