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

    /** The channel will never be enabled for HTTP/2.0. */
    public static final String NEVER_20 = "2.0_Never";
    /** The channel will disable HTTP/2.0 by default. */
    public static final String OPTIONAL_DEFAULT_OFF_20 = "2.0_Optional_Off";
    /** The channel will be enabled for HTTP/2.0 by default". */
    public static final String OPTIONAL_DEFAULT_ON_20 = "2.0_Optional_On";
    /** HTTP/1.1 Version protocol */
    public static final String PROTOCOL_VERSION_11 = "http/1.1";
    /** HTTP/2 Version protocol */
    public static final String PROTOCOL_VERSION_2 = "http/2";
    /** Can be set to specify the http protocol version. Ex: http/1.1, http/2 */
    public static final String PROPNAME_PROTOCOL_VERSION = "protocolVersionInternal";
}
