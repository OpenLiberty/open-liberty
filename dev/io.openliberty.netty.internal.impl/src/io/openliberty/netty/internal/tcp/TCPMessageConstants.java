/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

/**
 * Constants used by the TCP channel for various user-seen messages.
 * 
 * Taken from {@link com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants}
 */
public interface TCPMessageConstants {

    /** RAS trace bundle for NLS */
    String TCP_BUNDLE = "com.ibm.ws.tcpchannel.internal.resources.TCPChannelMessages";
    /** RAS trace group name */
    String NETTY_TRACE_NAME = "Netty";
    // -------------------------------------------------------------------------
    // NLS Messages
    // -------------------------------------------------------------------------

    /** Config option for max concurrent conns is invalid */
    String MAX_OPEN_CONNECTIONS_INVALID = "MAX_OPEN_CONNECTIONS_INVALID";
    /** Config option is not recognized */
    String UNRECOGNIZED_CUSTOM_PROPERTY = "UNRECOGNIZED_CUSTOM_PROPERTY";
    /** Config option is not correct */
    String NOT_VALID_CUSTOM_PROPERTY = "NOT_VALID_CUSTOM_PROPERTY";
    /** Config is missing the endpoint name */
    String NO_ENDPOINT_NAME = "NO_ENDPOINT_NAME";

    /** Config has invalid inactivity timeout */
    String INACTIVITY_TIMEOUT_INVALID = "INACTIVITY_TIMEOUT_INVALID";
    /** Config has an invalid IP exclude list */
    String ADDRESS_EXCLUDE_LIST_INVALID = "ADDRESS_EXCLUDE_LIST_INVALID";
    /** Config has an invalid IP include list */
    String ADDRESS_INCLUDE_LIST_INVALID = "ADDRESS_INCLUDE_LIST_INVALID";
    /** Config has an invalid hostname exclue list */
    String HOST_NAME_EXCLUDE_LIST_INVALID = "HOST_NAME_EXCLUDE_LIST_INVALID";
    /** Config has an invalid hostname include list */
    String HOST_NAME_INCLUDE_LIST_INVALID = "HOST_NAME_INCLUDE_LIST_INVALID";

    /** Msg that the channel has started */
    String TCP_CHANNEL_STARTED = "TCP_CHANNEL_STARTED";
    /** Msg that the channel has stopped */
    String TCP_CHANNEL_STOPPED = "TCP_CHANNEL_STOPPED";
    /** Msg when the max concurrent conns has been reached */
    String MAX_CONNS_EXCEEDED = "MAX_CONNS_EXCEEDED";
    /** Msg when a thread dispatch attempt failed */
    String THREAD_DISPATCH_FAILED = "THREAD_DISPATCH_FAILED";
    /** Msg when a TCP port bind failed */
    String BIND_ERROR = "BIND_ERROR";
    /** Msg when a TCP host resolve failed during start */
    String LOCAL_HOST_UNRESOLVED = "LOCAL_HOST_UNRESOLVED";
    /** Msg when a port has stopped accepting traffic due to an error */
    String PORT_NOT_ACCEPTING = "PORT_NOT_ACCEPTING";

    /** Config key is not valid */
    String CONFIG_KEY_NOT_VALID = "CONFIG_KEY_NOT_VALID";
    /** Config value is not a proper number */
    String CONFIG_VALUE_NUMBER_EXCEPTION = "CONFIG_VALUE_NUMBER_EXCEPTION";
    /** Config value is not a proper string */
    String CONFIG_VALUE_NOT_VALID_STRING = "CONFIG_VALUE_NOT_VALID_STRING";
    /** Config value is an incorrect null string */
    String CONFIG_VALUE_NOT_VALID_NULL_STRING = "CONFIG_VALUE_NOT_VALID_NULL_STRING";
    /** Config value is an incorrect boolean flag */
    String CONFIG_VALUE_NOT_VALID_BOOLEAN = "CONFIG_VALUE_NOT_VALID_BOOLEAN";
    /** Config value is an incorrect int */
    String CONFIG_VALUE_NOT_VALID_INT = "CONFIG_VALUE_NOT_VALID_INT";

    /** Config update attempted to change an immutable setting */
    String NEW_CONFIG_VALUE_NOT_EQUAL = "NEW_CONFIG_VALUE_NOT_EQUAL";
    /** Config update failed to be processed */
    String UPDATED_CONFIG_NOT_IMPLEMENTED = "UPDATED_CONFIG_NOT_IMPLEMENTED";
}
