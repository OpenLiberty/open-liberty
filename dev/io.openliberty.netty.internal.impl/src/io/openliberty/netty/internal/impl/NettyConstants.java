/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.impl;

public interface NettyConstants {

    /** RAS trace bundle for NLS */
    String BASE_BUNDLE = "io.openliberty.netty.internal.impl.resources.NettyFrameworkMessages";
    String CF_BUNDLE = "com.ibm.ws.channelfw.internal.resources.ChannelfwMessages";
    /** RAS trace group name */
    String NETTY_TRACE_NAME = "Netty";
    /** default trace string */
    String NETTY_TRACE_STRING = "io.netty*=all:io.openliberty.netty*=all";
    /** INADDR_ANY host */
    String INADDR_ANY = "0.0.0.0";

    /** TCP Logging Handler Name */
    public final String TCP_LOGGING_HANDLER_NAME = "tcpLoggingHandler";
    /** Inactivity Timeout Handler Name */
    public final String INACTIVITY_TIMEOUT_HANDLER_NAME = "inactivityTimeoutHandler";
    /** Max Connections Handler Name */
    public final String MAX_OPEN_CONNECTIONS_HANDLER_NAME = "maxConnectionHandler";
    /** Max Connections Handler Name */
    public final String ACCESSLIST_HANDLER_NAME = "accessListHandler";
    /** Netty enablement */
    String USE_NETTY = "useNettyTransport";
    /** Netty scaler and metrics properties */
    public static final String SCALER_MIN_THREADS_PROPERTY = "scalerMinThreads";
    public static final String SCALER_MAX_THREADS_PROPERTY = "scalerMaxThreads";
    public static final String SCALER_WINDOW_PROPERTY = "scalerWindowSize";
    public static final String SCALER_DOWN_THRESHOLD_PROPERTY = "scalerDownThreshold";
    public static final String SCALER_UP_THRESHOLD_PROPERTY = "scalerUpThreshold";
    public static final String SCALER_DOWN_STEP_PROPERTY = "scalerDownStep";
    public static final String SCALER_UP_STEP_PROPERTY = "scalerUpStep";
    public static final String SCALER_CYCLES_PROPERTY = "scalerCycles";
    public static final String SCALER_METRICS_WINDOW_PROPERTY = "scalerMetricsWindowSize";
    /** Netty scaler and metrics defaults */
    public static final int SCALER_MIN_THREADS = 1;
    public static final int SCALER_MAX_THREADS = 4;
    public static final long SCALER_WINDOW = 1500;
    public static final double SCALER_DOWN_THRESHOLD = 0.15;
    public static final double SCALER_UP_THRESHOLD = 0.85;
    public static final int SCALER_DOWN_STEP = 1;
    public static final int SCALER_UP_STEP = 1;
    public static final int SCALER_CYCLES = 3;
    public static final long SCALER_METRICS_WINDOW = 0;
}
