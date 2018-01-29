/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

/**
 * This purpose of this interface is to consolidate Strings used throughout
 * the SSL Channel to prevent future changes from rippling to all files.
 */
public interface SSLChannelConstants {

    /** Name of SSL resource bundle for NLS messages. */
    String SSL_BUNDLE = "com.ibm.ws.channel.ssl.internal.resources.SSLChannelMessages";
    /** Name associated with Trace output. */
    String SSL_TRACE_NAME = "SSLChannel";

    // Message keys in nlsprops file

    /** Invalid security properties found in config */
    String INVALID_SECURITY_PROPERTIES = "invalid.security.properties";
    /** Error occurred during a handshake */
    String HANDSHAKE_FAILURE = "handshake.failure";
    boolean DEFAULT_HANDSHAKE_FAILURE = false;
    /** Informational message that handshake error will no longer be logged */
    String HANDSHAKE_FAILURE_STOP_LOGGING = "handshake.failure.stop.logging";
    long DEFAULT_HANDSHAKE_FAILURE_STOP_LOGGING = 100;
    /** PI52696 */
    public static final String TIMEOUT_VALUE_IN_SSL_CLOSING_HANDSHAKE = "timeoutValueInSSLClosingHandshake";

    /** This setting is configured by enabling the Servlet 3.0 feature. The channel will never be enabled for HTTP/2.0. */
    public static final String NEVER_20 = "2.0_Never";
    /**
     * This setting is configured by enabling the Servlet 3.1 feature. Looks for the 'insecureUpgradeProtocol'
     * httpOptions to determine if the channel will enable HTTP/2.0
     */
    public static final String OPTIONAL_20 = "2.0_Optional";
    /** This setting is configured by enabling the Servlet 4.0 feature. The channel will always be enabled for HTTP/2.0". */
    public static final String ALWAYS_ON_20 = "2.0_AlwaysOn";

    /** If value is set to 'h2' and the Servlet feature 3.1 is enabled, this sets the channel to use HTTP/2.0 */
    public static final String PROPNAME_ALPN_PROTOCOLS = "alpnProtocols";
    public static final String H2_ALPN_PROTOCOL = "h2";
}
