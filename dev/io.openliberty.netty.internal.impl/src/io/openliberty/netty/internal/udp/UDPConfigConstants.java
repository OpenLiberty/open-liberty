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
package io.openliberty.netty.internal.udp;

/**
 * This interface describes the constants used for configuring a UDP Channel.
 * 
 * @see com.ibm.wsspi.udpchannel.UDPConfigConstants
 */
public interface UDPConfigConstants {

    /**
     * Host name that an inbound channel will use for listening
     */
    String HOST_NAME = "hostname";

    /**
     * port on which the inbound channel will listen
     */
    String PORT = "port";

    String ADDR_EXC_LIST = "addressExcludeList";

    String ADDR_INC_LIST = "addressIncludeList";

    /**
     * The size of the udp socket sending buffer
     */
    String SEND_BUFF_SIZE = "sendBufferSizeSocket";

    /**
     * The size of the udp socket receiving buffer
     */
    String RCV_BUFF_SIZE = "receiveBufferSizeSocket";

    /**
     * The size of the UDP buffer to be sent up the channel chain.
     */
    String CHANNEL_RCV_BUFF_SIZE = "receiveBufferSizeChannel";

    /**
     * Value used to determine if every conn link gets its own worker thread instance.
     */
    String CHANNEL_FACTORY_UNIQUE_WORKER_THREADS = "uniqueWorkerThreads";

    /**
     * Value used to store the name of the WAS end point in the channel properties.
     */
    String ENDPOINT_NAME = "endPointName";

    /**
     * Minimum Port Value
     */
    int PORT_MIN = 0;

    /**
     * Maximum Port Value
     */
    int PORT_MAX = 65535;

    /**
     * Maximum UDP Packet Size
     */
    int MAX_UDP_PACKET_SIZE = 65535;

    /**
     * Minimum UDP receive buffer size
     */
    int RECEIVE_BUFFER_SIZE_MIN = 4;

    /**
     * Maximum UDP receive buffer size
     */
    int RECEIVE_BUFFER_SIZE_MAX = 16777216; // 16 Meg

    /**
     * Minimum UDP send buffer size
     */
    int SEND_BUFFER_SIZE_MIN = 4;

    /**
     * Maximum UDP send buffer size
     */
    int SEND_BUFFER_SIZE_MAX = 16777216; // 16 Meg

    /**
     * The configured host interface. This is used in the connection ready callback to
     * identify which configured UDP channel chain this is. This is retrieved from the vc statemap.
     */
    String CONFIGURED_HOST_INTERFACE_VC_MAP = "UDPConfiguredListeningHost";

    /**
     * The configured port. This is used in the connection ready callback to
     * identify which configured UDP channel chain this is. This is retrieved from the vc statemap.
     */
    String CONFIGURED_PORT_VC_MAP = "UDPConfiguredListeningPort";

}
