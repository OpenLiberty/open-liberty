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
 * This interface describes the constants used for configuring a TCP Channel.
 * 
 * Adapted from {@link com.ibm.wsspi.tcpchannel.TCPConfigConstants}
 * 
 */
public interface TCPConfigConstants {

    /**
     * The external name used for this TCP channel
     */
    String EXTERNAL_NAME = "externalName";

    /**
     * Host name that an inbound channel will use for listening
     */
    String HOST_NAME = "hostname";

    /**
     * port on which the inbound channel will listen
     */
    String PORT = "port";

    /**
     * The maximum number of concurrent connections allowed for an inbound channel
     */
    String MAX_CONNS = "maxOpenConnections";

    /**
     * An IPv4/IPv6 address exclusion list that an inbound channel will use when
     * accepting new connections
     */
    String ADDR_EXC_LIST = "addressExcludeList";

    /**
     * A host name exclusion list that an inbound channel will use when accepting
     * new connections
     */
    String NAME_EXC_LIST = "hostNameExcludeList";

    /**
     * An IPv4/IPv6 address inclusion list that an inbound channel will use when
     * accepting new connections
     */
    String ADDR_INC_LIST = "addressIncludeList";

    /**
     * A host name inclusion list that an inbound channel will use when accepting
     * new connections
     */
    String NAME_INC_LIST = "hostNameIncludeList";

    /**
     * The default time out for TCP operations for this channel
     */
    String INACTIVITY_TIMEOUT = "inactivityTimeout";

    /**
     * The size of the tcp socket sending buffer
     */
    String SEND_BUFF_SIZE = "sendBufferSize";

    /**
     * The size of the tcp socket receiving buffer
     */
    String RCV_BUFF_SIZE = "receiveBufferSize";

    /**
     * Minimum Port Value
     */
    int PORT_MIN = 0;

    /**
     * Maximum Port Value
     */
    int PORT_MAX = 65535;

    /**
     * Minimum number of the maximum allowable concurrent connections
     */
    int MAX_CONNECTIONS_MIN = 1;

    /**
     * Maximum number of the maximum allowable concurrent connections
     */
    int MAX_CONNECTIONS_MAX = 1280000;

    /**
     * Maximum number of the maximum allowable concurrent connections
     */
    int MAX_CONNECTIONS_DEFAULT = 128000;

    /**
     * Minimum TCP socket recceve buffer size
     */
    int RECEIVE_BUFFER_SIZE_MIN = 4;

    /**
     * Maximum TCP socket receive buffer size
     */
    int RECEIVE_BUFFER_SIZE_MAX = 16777216; // 16 Meg

    /**
     * Minimum TCP socket send buffer size
     */
    int SEND_BUFFER_SIZE_MIN = 4;

    /**
     * Maximum TCP socket send buffer size
     */
    int SEND_BUFFER_SIZE_MAX = 16777216; // 16 Meg

    /**
     * Minimum timeout for TCP operations. A value of 0 means No Timeout
     */
    int INACTIVITY_TIMEOUT_MIN = 0; // value given in milliseconds

    /**
     * Maximum timeout for TCP operations
     */
    int INACTIVITY_TIMEOUT_MAX = 3600000; // value given in milliseconds

    /**
     * Default timeout for TCP operations in seconds
     */
    int INACTIVITY_TIMEOUT_DEFAULT_SECONDS = 60;

    /**
     * Default timeout for TCP operations in milliseconds
     */
    int INACTIVITY_TIMEOUT_DEFAULT_MSECS = 60000;

    /**
     * Port that the TCPChannel is actually listening on. This property is
     * put back in the property bag after the TCPChannel has started. It should
     * NOT be passed as part of the input configuration.
     */
    String LISTENING_PORT = "listeningPort";

    /**
     * Determines whether Include/Exclude Access Lists are to be considered as
     * case insensitive.
     */
    String CASE_INSENSITIVE_HOSTNAMES = "caseInsensitiveHostnames";

}
