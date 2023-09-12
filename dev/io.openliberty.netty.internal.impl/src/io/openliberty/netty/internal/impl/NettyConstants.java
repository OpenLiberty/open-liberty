/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
    /** RAS trace group name */
    String NETTY_TRACE_NAME = "Netty";
    /** default trace string */
    String NETTY_TRACE_STRING = "io.netty*=all:io.openliberty.netty*=all"; 
    /** INADDR_ANY host  */
    String INADDR_ANY = "0.0.0.0";

    /** TCP Logging Handler Name  */
    public final String TCP_LOGGING_HANDLER_NAME = "tcpLoggingHandler";
    /** Inactivity Timeout Handler Name  */
    public final String INACTIVITY_TIMEOUT_HANDLER_NAME = "inactivityTimeoutHandler";
    /** Max Connections Handler Name  */
    public final String MAX_OPEN_CONNECTIONS_HANDLER_NAME = "maxConnectionHandler";
    /** Max Connections Handler Name  */
    public final String ACCESSLIST_HANDLER_NAME = "accessListHandler";
	/** Netty enablement */
    String USE_NETTY = "useNettyTransport";

}
