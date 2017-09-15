/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * This class contains all constants relating to JFap and JFap flows on the wire. It is used by the
 * JFapChannel component as well as its users.
 */
public class JFapChannelConstants {
    public final static String MSG_GROUP = com.ibm.ws.sib.utils.TraceGroups.TRGRP_JFAP;
    public final static String MSG_BUNDLE = "com.ibm.ws.sib.jfapchannel.CWSIJMessages"; // F195445.8 // D217340

    // The class name of the client connection manager
    public final static String CLIENT_MANAGER_CLASS =
                    "com.ibm.ws.sib.jfapchannel.impl.ClientConnectionManagerImpl";

    // The class name of the server connection manager
    public final static String SERVER_MANAGER_CLASS =
                    "com.ibm.ws.sib.jfapchannel.server.impl.ServerConnectionManagerImpl";

    // The class name of the Rich client framework
    public final static String RICH_CLIENT_FRAMEWORK_CLASS =
                    "com.ibm.ws.sib.jfapchannel.richclient.framework.impl.RichClientFramework";

    // The class name of the Thin client framework
    public final static String THIN_CLIENT_FRAMEWORK_CLASS =
                    "com.ibm.ws.sib.jfapchannel.framework.impl.ThinClientFramework";

    // The class name of the Rich client byte buffer manager
    public final static String RICH_CLIENT_BUFFER_MANAGER_CLASS =
                    "com.ibm.ws.sib.jfapchannel.richclient.buffer.impl.RichByteBufferPool";

    // The class name of the Thin client byte buffer manager
    public final static String THIN_CLIENT_BUFFER_MANAGER_CLASS =
                    "com.ibm.ws.sib.jfapchannel.buffer.impl.ThinByteBufferPool";

    // The class name of the Rich client thread pool
    public final static String RICH_CLIENT_THREADPOOL_CLASS =
                    "com.ibm.ws.sib.jfapchannel.richclient.threadpool.impl.RichThreadPoolImpl";

    // The class name of the Thin client thread pool
    public final static String THIN_CLIENT_THREADPOOL_CLASS =
                    "com.ibm.ws.sib.jfapchannel.threadpool.impl.ThinThreadPoolImpl";

    // The class name of the Rich client approximate time keeper
    public final static String RICH_CLIENT_APPROXTIME_CLASS =
                    "com.ibm.ws.sib.jfapchannel.richclient.approxtime.impl.RichQuickApproxTimeImpl";

    // The class name of the Thin client approximate time keeper
    public final static String THIN_CLIENT_APPROXTIME_CLASS =
                    "com.ibm.ws.sib.jfapchannel.approxtime.impl.ThinQuickApproxTimeImpl";

    // The class name of the Rich client alarm manager
    public final static String RICH_CLIENT_ALARMMGR_CLASS =
                    "com.ibm.ws.sib.jfapchannel.am.impl.RichAlarmManagerImpl";

    // The class name of the Thin client alarm manager
    public final static String THIN_CLIENT_ALARMMGR_CLASS =
                    "com.ibm.ws.sib.jfapchannel.am.impl.ThinAlarmManagerImpl";

    // The class that provides non-thread switching receive listening
    public final static String NON_THREAD_SWITCHING_DISPATCHER_CLASS = // F201521
    "com.ibm.ws.sib.jfapchannel.impl.rldispatcher.NonThreadSwitchingDispatchableImpl"; // F201521

    // The class that provides dispatch to all receive listening
    public final static String DISPATCH_TO_ALL_NONEMPTY_DISPATCHER_CLASS = // D213108
    "com.ibm.ws.sib.jfapchannel.impl.rldispatcher.DispatchToAllNonEmptyDispatchableImpl"; // D213108

    // The name of the class that provides the inbound connection implementation
    public final static String INBOUND_CHANNEL_CLASS =
                    "com.ibm.ws.sib.jfapchannel.server.impl.JFapChannelInbound";

    public final static String CLIENT_TCP_CHANNEL_THREADPOOL_NAME = "SIBFAPThreadPool";
    public final static String CLIENT_TCP_CHANNEL_THREADPOOL_MIN_SIZE_PROPERTY =
                    "com.ibm.ws.sib.jfapchannel.impl.MinimumClientTCPThreadPoolSize";
    public final static int CLIENT_TCP_CHANNEL_THREADPOOL_MIN_SIZE_DEFAULT = 1;
    public final static String CLIENT_TCP_CHANNEL_THREADPOOL_MAX_SIZE_PROPERTY =
                    "com.ibm.ws.sib.jfapchannel.impl.MaximumClientTCPThreadPoolSize";
    public final static int CLIENT_TCP_CHANNEL_THREADPOOL_MAX_SIZE_DEFAULT = 40;

    public final static String LOG_IO_TO_FFDC_EVENTLOG_PROPERTY =
                    "com.ibm.ws.sib.jfapchannel.impl.LOG_IO_TO_FFDC_EVENLOG";
    public final static String CONNECTION_FFDC_EVENTLOG_SIZE_PROPERTY =
                    "com.ibm.ws.sib.jfapchannel.impl.CONNECTION_FFDC_EVENTLOG_SIZE";
    public final static String CONVERSATION_FFDC_EVENTLOG_SIZE_PROPERTY =
                    "com.ibm.ws.sib.jfapchannel.impl.CONVERSATION_FFDC_EVENTLOG_SIZE";

    // (Service) property that determines how long a thread will stay with an empty
    // RLD queue before being repooled.
    public final static String RLD_REPOOL_THREAD_DELAY_PROPERTY =
                    "com.ibm.ws.sib.jfapchannel.impl.rldRepoolThreadDelayProperty";
    public final static int RLD_REPOOL_THREAD_DELAY_DEFAULT = 10000; // Time in milliseconds.

    // Sib property used to control the thin client TCP_NODELAY socket setting
    public final static String THIN_CLIENT_TCP_NODELAY_PROPERTY = "sib.jfapchannel.client.thin.TCP_NODELAY";
    public final static String THIN_CLIENT_TCP_NODELAY_DEFAULT = "true";

    // Default amount of time to wait before sending a hearbeat (seconds).
    public final static int DEFAULT_HEARTBEAT_INTERVAL = 300; // F175658

    // Default amount of time to wait for a heartbeat response (seconds).
    public final static int DEFAULT_HEARTBEAT_TIMEOUT = 7; // F175658

    // Maximum size for an unsegmented transmission.
    public final static int DEFAULT_MAX_TRANSMISSION_SIZE = 1024 * 1024; // F181603.2

    // Default size to use for buffer when reading data from a socket
    public final static int DEFAULT_READ_BUFFER_SIZE = 32 * 1024; // F181603.2

    // Default size to use for buffer when writing data to a socket.
    public final static int DEFAULT_WRITE_BUFFER_SIZE = 32 * 1024; // F181603.2

    // The default number of conversations to multiplex over a single socket connection
    public final static int DEFAULT_CONVERSATIONS_PER_CONN = 5; // SIB0116.com.1, D258248

    // Constants used for heartbeat values in channel configuration map
    public final static String JFAP_CHANNEL_CONFIG_HEARTBEAT_INTERVAL = "heartbeatInterval";
    public final static String JFAP_CHANNEL_CONFIG_HEARTBEAT_TIMEOUT = "heartbeatTimeout";

    // begin F196678.10
    // begin D213929
    // Names for default outbound chains -- used for client bootstrap.
    public final static String CHAIN_NAME_DEFAULT_OUTBOUND_JFAP_TCP = "BootstrapBasicMessaging";
    public final static String CHAIN_NAME_DEFAULT_OUTBOUND_JFAP_SSL_TCP = "BootstrapSecureMessaging";
    public final static String CHAIN_NAME_DEFAULT_OUTBOUND_JFAP_HTTPT_TCP = "BootstrapTunneledMessaging";
    public final static String CHAIN_NAME_DEFAULT_OUTBOUND_JFAP_HTTPT_SSL_TCP = "BootstrapTunneledSecureMessaging";

    // Names for default outbound channels -- used for client bootstrap.
    public final static String CHANNEL_NAME_OUTBOUND_JFAP = "BootstrapMessagingJFAPChannel";
    public final static String CHANNEL_NAME_OUTBOUND_TCP = "BootstrapMessagingTCPChannel";
    public final static String CHANNEL_NAME_OUTBOUND_SSL = "BootstrapMessagingSSLChannel";
    public final static String CHANNEL_NAME_OUTBOUND_HTTP = "BootstrapMessagingHTTPChannel";
    public final static String CHANNEL_NAME_OUTBOUND_HTTPT = "BootstrapMessagingHTTPTChannel";
    // end D213929

    // begin F244595
    public final static String CHANNEL_NAME_OUTBOUND_JFAP_TCPPROXY = "OutboundJFAPChannelTCPProxy";
    public final static String CHANNEL_NAME_OUTBOUND_TCPPROXY = "OutboundTCPProxyChannelTCP";

    public final static String CHANNEL_NAME_INBOUND_JFAP_TCPPROXY = "InboundJFAPChannelTCPProxy";
    public final static String CHANNEL_NAME_INBOUND_TCPPROXY = "InboundTCPProxyChannelTCP";

    // (Programatic) name given to the TCP Bridge Service Proxy Chains (currently z/OS specific)
    public final static String CHAIN_NAME_TCPPROXYBRIDGESERVICE_INBOUND = "_InboundTCPProxyBridgeService";
    public final static String CHAIN_NAME_TCPPROXYBRIDGESERVICE_OUTBOUND = "_OutboundTCPProxyBridgeService";
    // end F244595

    // Name of properties file used to determine SSL configuration for clients.
    public final static String CLIENT_SSL_PROPERTIES_FILE = "sib.client.ssl.properties";

    // Name of System Property that can specify the sib.client.ssl.properties file
    public final static String SYSTEM_SIB_CLIENT_SSL_PROPERTIES = "com.ibm.ws.sib.client.ssl.properties";

    // Custom property name/values
    public final static String CUSTOM_PROPERTY_TRUSTED = "trusted"; // D229336
    public final static String CUSTOM_PROPERTY_TRUSTED_VALUE_TRUE = "true"; // D229336

    // Names of factory classes
    public final static String CLASS_JFAP_CHANNEL_FACTORY = "com.ibm.ws.sib.jfapchannel.impl.WSJFapChannelFactory";
    public final static String CLASS_TCP_CHANNEL_FACTORY = "com.ibm.ws.tcp.channel.impl.WSTCPChannelFactory";
    public final static String CLASS_SSL_CHANNEL_FACTORY = "com.ibm.ws.ssl.channel.impl.WSSSLChannelFactory";
    public final static String CLASS_HTTPT_CHANNEL_FACTORY = "com.ibm.ws.httptunnel.channel.WSHttpTunnelOutboundChannelFactory";
    public final static String CLASS_HTTP_CHANNEL_FACTORY = "com.ibm.ws.http.channel.outbound.impl.WSHttpOutboundChannelFactory";
    public final static String CLASS_TCPPROXY_CHANNEL_FACTORY = "com.ibm.ws.tcpchannelproxy.jfap.impl.TCPProxyChannelFactory"; // F244595

    private final static int SECONDS_PER_YEAR = 365 * 24 * 60 * 60;

    // Minimum and maximum permissible values for heartbeating.
    public final static int MIN_HEARTBEAT_INTERVAL = 0;
    public final static int MAX_HEARTBEAT_INTERVAL = SECONDS_PER_YEAR;
    public final static int MIN_HEARTBEAT_TIMEOUT = 0;
    public final static int MAX_HEARTBEAT_TIMEOUT = SECONDS_PER_YEAR;

    // THESE ARE NOT THE CUSTOM PROPERTIES TO USE TO CONFIGURE HEARTBEAT BEHAVIOUR
    // Property keys used for storing heartbeating values in the channel configuration map.
    public final static String CHANNEL_CONFIG_HEARTBEAT_INTERVAL_PROPERTY = "com.ibm.ws.sib.jfapchannel.HEARTBEAT_INTERVAL_PROPERTY";
    public final static String CHANNEL_CONFIG_HEARTBEAT_TIMEOUT_PROPERTY = "com.ibm.ws.sib.jfapchannel.HEARTBEAT_TIMEOUT_PROPERTY";

    // Properties (suitable for use in the sib.properties file) that override the defaults
    // for heartbeating.
    public final static String RUNTIMEINFO_KEY_HEARTBEAT_INTERVAL = "com.ibm.ws.sib.jfapchannel.HEARTBEAT_INTERVAL";
    public final static String RUNTIMEINFO_KEY_HEARTBEAT_TIMEOUT = "com.ibm.ws.sib.jfapchannel.HEARTBEAT_TIMEOUT";

    // Properties (runtimeinfo) that override the default channel framework threadpool size.
    // Both must be specified to have any effect.
    public final static String RUNTIMEINFO_KEY_CF_THREADPOOL_MIN = "com.ibm.ws.sib.jfapchannel.MIN_CF_THREADPOOL_SIZE";
    public final static String RUNTIMEINFO_KEY_CF_THREADPOOL_MAX = "com.ibm.ws.sib.jfapchannel.MAX_CF_THREADPOOL_SIZE";
    // end F196678.10

    // begin D192359
    // Maximum depth and size (in bytes) for messages we will queue for transmission
    // at each priority level.
    public final static String RUNTIMEINFO_KEY_MAX_PRIORITY_QUEUE_DEPTH = "com.ibm.ws.sib.jfapchannel.maxPriorityQueueDepth";
    public final static String RUNTIMEINFO_KEY_MAX_PRIORITY_QUEUE_BYTES = "com.ibm.ws.sib.jfapchannel.maxPriorityQueueBytes";
    // end D192359

    // The name of the property in the SIB Client SSL file that specifies the JSSE provider. This
    // is only needed in the JFap thin client
    public final static String RUNTIMEINFO_JSSE_PROVIDER = "com.ibm.ws.sib.jsseProvider";

    /**
     * Property name which, when set in sib.properties, can be used to override the maximum
     * size of the thread pool used by the ReceiveListenerDispatcher.
     */
    public final static String MAX_CONCURRENT_DISPATCHES = "com.ibm.ws.sib.jfapchannel.MAX_CONCURRENT_DISPATCHES";

    /**
     * Property name which, when set in sib.properties, can be used to override the maximum
     * size of the thread pool used by the client side ReceiveListenerDispatcher.
     */
    public final static String MAX_CONCURRENT_DISPATCHES_CLIENT = "com.ibm.ws.sib.jfapchannel.MAX_CONCURRENT_DISPATCHES_CLIENT";

    /**
     * Property name which, when set in sib.properties, can be used to override the minimum
     * size of the thread pool used by the ReceiveListenerDispatcher.
     */
    public final static String MIN_CONCURRENT_DISPATCHES = "com.ibm.ws.sib.jfapchannel.MIN_CONCURRENT_DISPATCHES";

    /**
     * Property name which, when set in sib.properties, can be used to override the default
     * keep alive time of the thread pool used by the ReceiveListenerDispatcher.
     */
    public final static String RLD_KEEP_ALIVE_TIME = "com.ibm.ws.sib.jfapchannel.RLD_KEEP_ALIVE_TIME";

    //PM51216
    /**
     * Hastable to contain the integer codes and their corresponding segment names .
     */
    private static HashMap<Integer, String> segValues;
    // begin F174772

    // Constants for "reserved for JFAP Channel use only" segment IDs
    // REMEMBER: add any new constants to the isReservedSegmentId method!
    public static final int SEGMENT_HEARTBEAT = 0x01;
    public static final int SEGMENT_HEARTBEAT_RESPONSE = 0x02;
    public static final int SEGMENT_SEGMENTED_FLOW_START = 0x03;
    public static final int SEGMENT_SEGMENTED_FLOW_MIDDLE = 0x04;
    public static final int SEGMENT_SEGMENTED_FLOW_END = 0x05;
    public static final int SEGMENT_LOGICAL_CLOSE = 0x0A; // F177889
    public static final int SEGMENT_PING = 0x0E; // F177889
    public static final int SEGMENT_PING_RESPONSE = 0x0F; // F177889
    public static final int SEGMENT_PHYSICAL_CLOSE = 0xFF;

    // Constants for use with SEGMENT_LOGICAL_CLOSE
    public static final int LOGICAL_CLOSE_IMMEDIATE_BIT = 0x01;

    // Constants for use with SEGMENT_PHYSICAL_CLOSE
    public static final int PHYSICAL_CLOSE_IMMEDIATE_BIT = 0x01;

    // Default setting for whether or not to give close signals the same priority as other traffic
    public static final boolean DEFAULT_INCREASE_CLOSE_PRIORITY = true;

    // Default setting for whether or not to always dispatch closes to another thread rather than running in-line
    public static final boolean DEFAULT_ALWAYS_DISPATCH_CLOSE = true;

    // Default setting for whether or not to always dispatch stops to another thread rather than running in-line
    public static final boolean DEFAULT_ALWAYS_DISPATCH_STOP = true;

    // Default setting for whether or not to always dispatch unlockAlls to another thread rather than running in-line
    public static final boolean DEFAULT_ALWAYS_DISPATCH_UNLOCKALL = true;

    /**
     * Determines if a segment ID is one of the "reserved" set.
     * A "reserved" ID is one which cannot be specified by the user
     * of the JFAP Channel - it is used internally.
     * <p>
     * <strong>
     * REMEMBER: Update this with any new "reserved" segment ids.
     * </strong>
     * 
     * @param id The ID to test.
     * @return boolean Returns True iff the ID is reserved.
     */
    public static boolean isReservedSegmentId(int id)
    {
        boolean isReserved = false;
        switch (id)
        {
            case SEGMENT_HEARTBEAT:
            case SEGMENT_HEARTBEAT_RESPONSE:
            case SEGMENT_SEGMENTED_FLOW_START:
            case SEGMENT_SEGMENTED_FLOW_MIDDLE:
            case SEGMENT_SEGMENTED_FLOW_END:
            case SEGMENT_LOGICAL_CLOSE:
            case SEGMENT_PING:
            case SEGMENT_PING_RESPONSE:
            case SEGMENT_PHYSICAL_CLOSE:
                isReserved = true;
        }
        return isReserved;
    }

    // end F174772

    // begin F181603.2
    /**
     * Enumerated type for transmission layouts
     */
    public static class TransmissionLayout
    {
        private static int globalTranmissionLayoutEnumCounter = 0;
        private final String layoutName;
        private final int enumValue;

        private TransmissionLayout(String layoutName)
        {
            this.layoutName = layoutName;
            enumValue = globalTranmissionLayoutEnumCounter++;
        }

        @Override
        public String toString()
        {
            return getClass() + "@" + System.identityHashCode(this) + ":" + layoutName;
        }

        public int enumeratedValue()
        {
            return enumValue;
        }
    }

    /** Transmission layout: unknown */
    public static final TransmissionLayout XMIT_LAYOUT_UNKNOWN = new TransmissionLayout("unknown");
    /** Transmission layout: a primary header only */
    public static final TransmissionLayout XMIT_PRIMARY_ONLY = new TransmissionLayout("primary only");
    /** Transmission layout: transmission has a primary header followed by conversation header */
    public static final TransmissionLayout XMIT_CONVERSATION = new TransmissionLayout("conversation");
    /** Transmission layout: primary, conversation and start segment headers */
    public static final TransmissionLayout XMIT_SEGMENT_START = new TransmissionLayout("segment start");
    /** Transmission layout: primary, conversation and middle segment headers */
    public static final TransmissionLayout XMIT_SEGMENT_MIDDLE = new TransmissionLayout("segment middle");
    /** Transmission layout: primary, conversation and end segment headers */
    public static final TransmissionLayout XMIT_SEGMENT_END = new TransmissionLayout("segment end");

    /**
     * Converts from a segment ID to a layout for the transmission.
     * 
     * @param segment
     * @return Returns the layout for the segment.
     */
    public static TransmissionLayout segmentToLayout(int segment)
    {
        TransmissionLayout layout = XMIT_LAYOUT_UNKNOWN;
        switch (segment)
        {
            case 0x00:
                layout = XMIT_LAYOUT_UNKNOWN;
                break;
            case JFapChannelConstants.SEGMENT_HEARTBEAT:
            case JFapChannelConstants.SEGMENT_HEARTBEAT_RESPONSE:
            case JFapChannelConstants.SEGMENT_PHYSICAL_CLOSE:
                layout = XMIT_PRIMARY_ONLY;
                break;
            case JFapChannelConstants.SEGMENT_SEGMENTED_FLOW_START:
                layout = XMIT_SEGMENT_START;
                break;
            case JFapChannelConstants.SEGMENT_SEGMENTED_FLOW_MIDDLE:
                layout = XMIT_SEGMENT_MIDDLE;
                break;
            case JFapChannelConstants.SEGMENT_SEGMENTED_FLOW_END:
                layout = XMIT_SEGMENT_END;
                break;
            default:
                // Assume a conversation layout by default.
                layout = XMIT_CONVERSATION;
                break;
        }
        return layout;
    }

    // end F181603.2

    // begin F181603.2
    /** Size of JFAP primary header in bytes */
    public static final int SIZEOF_PRIMARY_HEADER = 10;

    /** Size of JFAP conversation header in bytes */
    public static final int SIZEOF_CONVERSATION_HEADER = 4;

    /** Size of JFAP start segmented transmission header in bytes */
    public static final int SIZEOF_SEGMENT_START_HEADER = 12;

    /** Size of JFAP eyecatcher in bytes */
    public static final int SIZEOF_EYECATCHER = 2;

    /** JFAP eyecatcher represented as an array of bytes */
    public static final byte[] EYECATCHER_AS_BYTES = { (byte) 0xBE, (byte) 0xEF };

    /** JFAP eyecatcher represented as a short */
    public static final short EYECATCHER_AS_SHORT = (short) 0xBEEF;

    /** Number of priority levels supported by the JFap Channel */
    public static final int MAX_PRIORITY_LEVELS = 16;
    // end F181603.2

    /** Acceptor ID used to start channel chains which end in the JFAP Channel */
    public static final String JFAP_CHANNEL_ACCEPTOR_ID = "JFapChannelAcceptorID"; // begin F189000

    // Start D217342
    /** Constant denoting FAP version 1 */
    public static final short FAP_VERSION_1 = 1;
    /** Constant denoting FAP version 2 */
    public static final short FAP_VERSION_2 = 2; // F247975
    /** Constant denoting FAP version 3 */
    public static final short FAP_VERSION_3 = 3; // SIB0009.comms.1
    /** Constant denoting FAP version 4 - reserved for service use */
    public static final short FAP_VERSION_4 = 4;
    /** Constant denoting FAP version 5 */
    public static final short FAP_VERSION_5 = 5; // D350111.1
    /** Constant denoting FAP version 6 */
    public static final short FAP_VERSION_6 = 6; // D377093.1
    /** Constant denoting FAP version 7 : enabled under PK73713 */
    public static final short FAP_VERSION_7 = 7; // SIB0112c.com.1
    /** Constant denoting FAP version 8 - reserved for service use */
    public static final short FAP_VERSION_8 = 8; // SIB0112c.com.1
    /** Constant denoting FAP version 9 */
    public static final short FAP_VERSION_9 = 9; // SIB0112c.com.1

    /** Constant denoting FAP version 10 */
    public static final short FAP_VERSION_10 = 10;

    /** Constant denoting FAP version 11 - reserved for service use */
    public static final short FAP_VERSION_11 = 11;

    /** Constant denoting FAP version 12 - reserved for service use */
    public static final short FAP_VERSION_12 = 12;

    /** Constant denoting FAP version 13 */
    public static final short FAP_VERSION_13 = 13;

    /** Constant denoting FAP version 14 : enabled under F011127 */
    public static final short FAP_VERSION_14 = 14; //F011127

    /** Constant denoting FAP version 15 : enabled under F013661 */
    public static final short FAP_VERSION_15 = 15; //F011127

    /** Version of JFAP for future 8* releases - reserved for service use */
    public static final short FAP_VERSION_16 = 16; //F013661 

    /** Constant denoting FAP version 17 - reserved for service use */
    public static final short FAP_VERSION_17 = 17;

    /** Constant denoting FAP version 18 - reserved for service use */
    public static final short FAP_VERSION_18 = 18;

    /** Constant denoting FAP version 19 - reserved for service use */
    public static final short FAP_VERSION_19 = 19;

    /** Constant denoting FAP version 20 enabled for JMS 2.0 */
    public static final short FAP_VERSION_20 = 20;
    /***************************************************************************/
    /* These are the constants used to denote the user data that is transmitted */
    /* across the network. Adding them to hashmap - PM51216 */
    /***************************************************************************/

    static {
        segValues = new HashMap<Integer, String>();

        Class constants = JFapChannelConstants.class;
        Field[] allSegTypes = constants.getDeclaredFields();

        for (int x = 0; x < allSegTypes.length; x++)
        {
            try
            {
                int intValue = allSegTypes[x].getInt(null);

                if ((allSegTypes[x].getName().startsWith("SEG_")) || (allSegTypes[x].getName().startsWith("SEGMENT_")))
                    segValues.put(intValue, allSegTypes[x].getName());

            }
            // Skip over private fields
            catch (IllegalAccessException e)
            {
                // No FFDC code needed
            }
            // Skip over non-int fields
            catch (IllegalArgumentException e)
            {
                // No FFDC code needed
            }
        }
    }

    /***************************************************************************/
    /* These are constants used to denote the user data that is transmitted */
    /* across the network. */
    /***************************************************************************/
    public static final int SEG_HANDSHAKE = 0x06;
    public static final int SEG_USER_IDENTITY = 0x07;
    public static final int SEG_TOPOLOGY = 0x08;
    public static final int SEG_MESSAGE_FORMAT_INFO = 0x09;

    public static final int SEG_EXCEPTION = 0x0B;
    public static final int SEG_DIRECT_CONNECT = 0x0C;
    public static final int SEG_DIRECT_CONNECT_R = 0x0D;

    public static final int SEG_XAOPEN = 0x30;
    public static final int SEG_XAOPEN_R = 0xB0;
    public static final int SEG_XASTART = 0x31;
    public static final int SEG_XASTART_R = 0xB1;
    public static final int SEG_XAEND = 0x32;
    public static final int SEG_XAEND_R = 0xB2;
    public static final int SEG_XAPREPARE = 0x33;
    public static final int SEG_XAPREPARE_R = 0xB3;
    public static final int SEG_XACOMMIT = 0x34;
    public static final int SEG_XACOMMIT_R = 0xB4;
    public static final int SEG_XAROLLBACK = 0x35;
    public static final int SEG_XAROLLBACK_R = 0xB5;
    public static final int SEG_XARECOVER = 0x36;
    public static final int SEG_XARECOVER_R = 0xB6;
    public static final int SEG_XAFORGET = 0x37;
    public static final int SEG_XAFORGET_R = 0xB7;

    public static final int SEG_XA_GETTXTIMEOUT = 0x38;
    public static final int SEG_XA_GETTXTIMEOUT_R = 0xB8;
    public static final int SEG_XA_SETTXTIMEOUT = 0x39;
    public static final int SEG_XA_SETTXTIMEOUT_R = 0xB9;

    public static final int SEG_CLOSE_CONNECTION = 0x40;
    public static final int SEG_CLOSE_CONNECTION_R = 0xC0;

    public static final int SEG_CREATE_TEMP_DESTINATION = 0x41;
    public static final int SEG_CREATE_TEMP_DESTINATION_R = 0xC1;
    public static final int SEG_DELETE_TEMP_DESTINATION = 0x42;
    public static final int SEG_DELETE_TEMP_DESTINATION_R = 0xC2;

    public static final int SEG_CREATE_DURABLE_SUB = 0x43;
    public static final int SEG_CREATE_DURABLE_SUB_R = 0xC3;
    public static final int SEG_DELETE_DURABLE_SUB = 0x44;
    public static final int SEG_DELETE_DURABLE_SUB_R = 0xC4;

    public static final int SEG_SEND_CONN_MSG = 0x45;
    public static final int SEG_SEND_CONN_MSG_R = 0xC5;
    public static final int SEG_SEND_CONN_MSG_NOREPLY = 0x46;
    public static final int SEG_RECEIVE_CONN_MSG = 0x47;
    public static final int SEG_RECEIVE_CONN_MSG_R = 0xC7;

    public static final int SEG_CREATE_PRODUCER_SESS = 0x48;
    public static final int SEG_CREATE_PRODUCER_SESS_R = 0xC8;
    public static final int SEG_CLOSE_CONSUMER_SESS = 0x49;
    public static final int SEG_CLOSE_CONSUMER_SESS_R = 0xC9;
    public static final int SEG_CLOSE_PRODUCER_SESS = 0x4A;
    public static final int SEG_CLOSE_PRODUCER_SESS_R = 0xCA;

    public static final int SEG_SEND_SESS_MSG = 0x4B;
    public static final int SEG_SEND_SESS_MSG_R = 0xCB;
    public static final int SEG_SEND_SESS_MSG_NOREPLY = 0x4C;
    public static final int SEG_CREATE_CONSUMER_SESS = 0x4D;
    public static final int SEG_CREATE_CONSUMER_SESS_R = 0xCD;
    public static final int SEG_RECEIVE_SESS_MSG = 0x4E;
    public static final int SEG_RECEIVE_SESS_MSG_R = 0xCE;
    public static final int SEG_REQUEST_MSGS = 0x4F;

    public static final int SEG_CREATE_UCTRANSACTION = 0x50;
    public static final int SEG_COMMIT_TRANSACTION = 0x51;
    public static final int SEG_COMMIT_TRANSACTION_R = 0xD1;
    public static final int SEG_ROLLBACK_TRANSACTION = 0x52;
    public static final int SEG_ROLLBACK_TRANSACTION_R = 0xD2;

    public static final int SEG_REGISTER_ASYNC_CONSUMER = 0x53;
    public static final int SEG_REGISTER_ASYNC_CONSUMER_R = 0xD3;
    public static final int SEG_DEREGISTER_ASYNC_CONSUMER = 0x54;
    public static final int SEG_DEREGISTER_ASYNC_CONSUMER_R = 0xD4;

    public static final int SEG_START_SESS = 0x55;
    public static final int SEG_STOP_SESS = 0x56;
    public static final int SEG_STOP_SESS_R = 0xD6;
    public static final int SEG_FLUSH_SESS = 0x57;
    public static final int SEG_FLUSH_SESS_R = 0xD7;
    public static final int SEG_UNLOCK_ALL = 0x58;
    public static final int SEG_UNLOCK_ALL_R = 0xD8;
    public static final int SEG_UNLOCK_SET_NOREPLY = 0x59;
    public static final int SEG_DELETE_SET_NOREPLY = 0x5A;
    public static final int SEG_DELETE_SET = 0x5B;
    public static final int SEG_DELETE_SET_R = 0xDB;

    public static final int SEG_PROXY_MESSAGE = 0xDC;
    public static final int SEG_ASYNC_MESSAGE = 0xDD;
    public static final int SEG_BROWSE_MESSAGE = 0xDE;

    public static final int SEG_CREATE_BROWSER_SESS = 0x5F;
    public static final int SEG_CREATE_BROWSER_SESS_R = 0xDF;
    public static final int SEG_RESET_BROWSE = 0x60;
    public static final int SEG_RESET_BROWSE_R = 0xE0;

    public static final int SEG_EVENT_OCCURRED = 0xE1;
    public static final int SEG_CONNECTION_INFO = 0xE2;

    public static final int SEG_CREATE_CLONE_CONNECTION = 0x63;
    public static final int SEG_CREATE_CLONE_CONNECTION_R = 0xE3;

    public static final int SEG_GET_UNIQUE_ID = 0x64;
    public static final int SEG_GET_UNIQUE_ID_R = 0xE4;

    public static final int SEG_GET_DESTINATION_CONFIGURATION = 0x65;
    public static final int SEG_GET_DESTINATION_CONFIGURATION_R = 0xE5;

    public static final int SEG_SEND_SCHEMA_NOREPLY = 0xE6;

    public static final int SEG_CREATE_CONS_FOR_DURABLE_SUB = 0x66;
    public static final int SEG_CREATE_CONS_FOR_DURABLE_SUB_R = 0xE7;

    public static final int SEG_CREATE_BIFURCATED_SESSION = 0x67;
    public static final int SEG_CREATE_BIFURCATED_SESSION_R = 0xE8;
    public static final int SEG_UNLOCK_SET = 0x68;
    public static final int SEG_UNLOCK_SET_R = 0xE9;
    public static final int SEG_READ_SET = 0x69;
    public static final int SEG_READ_SET_R = 0xEA;
    public static final int SEG_READ_AND_DELETE_SET = 0x6A;
    public static final int SEG_READ_AND_DELETE_SET_R = 0xEB;

    public static final int SEG_CREATE_ORDER_CONTEXT = 0x6B;
    public static final int SEG_CREATE_ORDER_CONTEXT_R = 0xEC;

    public static final int SEG_SEND_TO_EXCEPTION_DESTINATION = 0x6C;
    public static final int SEG_SEND_TO_EXCEPTION_DESTINATION_R = 0xED;

    public static final int SEG_CLOSE_ORDER_CONTEXT = 0x6D; // D241156
    public static final int SEG_CLOSE_ORDER_CONTEXT_R = 0xEE; // D241156

    public static final int SEG_REQUEST_SCHEMA = 0x6E; // F247845
    public static final int SEG_REQUEST_SCHEMA_R = 0xF0; // F247845

    public static final int SEG_MULTICAST_MESSAGE = 0xF1; // F247845

    public static final int SEG_CHECK_MESSAGING_REQUIRED = 0x6F; // LIDB3684.11.1.3
    public static final int SEG_CHECK_MESSAGING_REQUIRED_R = 0xF2; // LIDB3684.11.1.3

    public static final int SEG_INVOKE_COMMAND = 0x70; // SIB0009.comms.1
    public static final int SEG_INVOKE_COMMAND_R = 0xF3; // SIB0009.comms.1

    public static final int SEG_START_SESS_R = 0xF4; // D347591

    public static final int SEG_INVOKE_COMMAND_WITH_TX = 0x71; // D377093.1

    public static final int SEG_SEND_CHUNKED_SESS_MSG = 0x72; // SIB0112c.com.1
    public static final int SEG_SEND_CHUNKED_SESS_MSG_NOREPLY = 0x73; // SIB0112c.com.1
    public static final int SEG_SEND_CHUNKED_SESS_MSG_R = 0xF5; // SIB0112c.com.1
    public static final int SEG_SEND_CHUNKED_CONN_MSG = 0x74; // SIB0112c.com.1
    public static final int SEG_SEND_CHUNKED_CONN_MSG_NOREPLY = 0x75; // SIB0112c.com.1
    public static final int SEG_SEND_CHUNKED_CONN_MSG_R = 0xF6; // SIB0112c.com.1

    public static final int SEG_CHUNKED_PROXY_MESSAGE = 0xF7; // SIB0112c.com.1
    public static final int SEG_CHUNKED_ASYNC_MESSAGE = 0xF8; // SIB0112c.com.1
    public static final int SEG_CHUNKED_BROWSE_MESSAGE = 0xF9; // SIB0112c.com.1
    public static final int SEG_CHUNKED_SYNC_CONN_MESSAGE = 0xFA; // SIB0112c.com.1
    public static final int SEG_CHUNKED_SYNC_SESS_MESSAGE = 0xFB; // SIB0112c.com.1

    public static final int SEG_SEND_CHUNKED_TO_EXCEPTION_DESTINATION = 0x76; // SIB0112c.com.1
    public static final int SEG_SEND_CHUNKED_TO_EXCEPTION_DESTINATION_R = 0xFC; // SIB0112c.com.1

    public static final int SEG_ADD_DESTINATION_LISTENER = 0x77; //SIB0137.comms.2
    public static final int SEG_ADD_DESTINATION_LISTENER_R = 0xFD; //SIB0137.comms.2
    public static final int SEG_DESTINATION_LISTENER_CALLBACK_NOREPLY = 0x78; //SIB0137.comms.3

    public static final int SEG_REGISTER_STOPPABLE_ASYNC_CONSUMER = 0x79; //SIB0115d.comms
    public static final int SEG_DEREGISTER_STOPPABLE_ASYNC_CONSUMER = 0x7A; //SIB0115d.comms
    public static final int SEG_ASYNC_SESSION_STOPPED_NOREPLY = 0x7B; //SIB0115d.comms
    public static final int SEG_RESTART_SESS = 0x7C; //471642

    /**
     * Used on z/OS only to indicate to the CRA that a message has been processed
     * by the SR.
     */
    public static final int SEG_WMQRA_MSG_DELIVERED = 0x80;

    /**
     * Used only on z/OS to activate/deactivate a MEP.
     */
    public static final int SEG_MEP_COMMAND = 0x81;

    /**
     * Used only on z/OS as a response from SEG_MEP_COMMAND.
     */
    public static final int SEG_MEP_COMMAND_R = 0x82;

    public static final int SEG_REGISTER_CONSUMER_SET_MONITOR = 0x83; //F011127
    public static final int SEG_REGISTER_CONSUMER_SET_MONITOR_R = 0x84; //F011127
    public static final int SEG_REGISTER_CONSUMER_SET_MONITOR_CALLBACK_NOREPLY = 0x85; //F011127

    public static final int SEG_UNLOCK_ALL_NO_INC_LOCK_COUNT = 0x86; //F013661
    public static final int SEG_UNLOCK_ALL_NO_INC_LOCK_COUNT_R = 0x87; //F013661

    /***************************************************************************/
    /* These are constants used to denote the priorities of messages */
    /* transmitted by the JFap channel. */
    /***************************************************************************/
    public static final short PRIORITY_HIGH = Conversation.PRIORITY_HIGH;
    public static final short PRIORITY_MEDIUM = Conversation.PRIORITY_MEDIUM;
    public static final short PRIORITY_LOW = Conversation.PRIORITY_LOW;
    public static final short PRIORITY_HIGHEST = Conversation.PRIORITY_HIGHEST;
    public static final short PRIORITY_LOWEST = Conversation.PRIORITY_LOWEST;
    public static final short PRIORITY_HANDSHAKE = PRIORITY_MEDIUM;

    /**
     * Converts a SIBusMessage priority to a JFAP message priority
     * so that the message can be sent with an equivalent priority.
     * <p>
     * If the priority is outside the range of the JFAP priority we
     * will return either the highest (if it is above) or the lowest
     * (if it is below).
     * 
     * @param siBusMessagePriority The message priority to convert.
     * @return Returns the JFAP message priority
     */
    public static short getJFAPPriority(Integer siBusMessagePriority)
    {
        short jfapPriority = (short) (siBusMessagePriority.intValue() + 2);

        // If the priority is higher than what we allow, return
        // the highest we can
        if (jfapPriority > PRIORITY_HIGH)
        {
            return PRIORITY_HIGH;
        }
        // If the priority is lower than what we allow, return
        // the lowest we can
        else if (jfapPriority < PRIORITY_LOW)
        {
            return PRIORITY_LOW;
        }
        // Otherwise return it
        else
        {
            return jfapPriority;
        }
    }

    /**
     * This method can be used to return the actual segment
     * name for a given segment type.
     * 
     * @param segmentType The segment number.
     * @return Returns the String name of this segment.
     */
    public static String getSegmentName(int segmentType)
    {
        String segmentName = "(Unknown segment type)";

        segmentName = segValues.get(segmentType);

        if (segmentName == null)
            segmentName = "(Unknown segment type)";

        return segmentName;
    }

    // End D217342

    // Connect timout constants                                                                                   PK58698
    public static final int CONNECT_TIMEOUT_DEFAULT = 60; // seconds                       PK58698
    public static final String CONNECT_TIMEOUT_JFAP_KEY = "sib.comms.connect.timeout.jfap"; //PK58698

    // begin D181601
    // ------------------------------------------------------------
    // What follows are the unique constants used for FFDC probe
    // ID's.  Their names are composed as follows:
    //    CLASSNAME_METHODNAME_NN = "XXXXYYYY"
    // Where:
    //    CLASSNAME  = (Possibly truncated) name of class the probe
    //                 ID occurres in.
    //    METHODNAME = (Possibly truncated) method name for method
    //                 that probe ID occurres in.
    //    NN         = Numeric component (starting at 01) that makes
    //                 probe ID unique for the classname / method
    //                 name combination.
    //    XXXX       = Class unique identifier (simply counts up
    //                 but makes it easy to insert new IDs into an
    //                 existing range by making the space sparcely
    //                 populated).
    //    YYYY       = Class unique probe ID.
    //

    // ClientConnectionManager
    public static final String CLNTCONNMGR_STATICCONS_01 = "00000001";
    public static final String CLNTCONNMGR_INITIALISE_01 = "00000002";
    // ServerConnectionManager
    public static final String SRVRCONNMGR_INITIALISE_01 = "00010001";
    public static final String SRVRCONNMGR_STATICCONS_01 = "00010002";
    public static final String SRVRCONNMGR_INITIALISE_ALF_01 = "00010003";

    // ChannelFrameworkReference
    public static final String CHFWREF_LOOKUPFROMSERVICESNAMESPACE_01 = "00020001";
    public static final String CHFWREF_FINDINSTANDALONE_01 = "00020002";
    public static final String CHFWREF_LOADSSLPROPERTIES_01 = "00020003"; // F196678.10
    public static final String CHFWREF_CREATE_OUTBOUND_CHAINS_01 = "00020004"; // F196678.10
    public static final String CHFWREF_CREATE_OUTBOUND_CHAINS_02 = "00020005"; // F196678.10
    public static final String CHFWREF_CREATE_OUTBOUND_CHAINS_03 = "00020006"; // F196678.10
    public static final String CHFWREF_CREATE_OUTBOUND_CHAINS_04 = "00020007"; // F196678.10
    public static final String CHFWREF_GETCLASSFORNAME_01 = "00020008"; // F196678.10
    public static final String CHFWREF_FINDCLIENTCONT_01 = "00020009"; // D221079
    public static final String CHFWREF_FINDCLIENTCONT_02 = "00020010"; // D221079
//   public static final String CHFWREF_CREATE_OUTBOUND_CHAINS_05           = "00020011";  // D191832.1
    public static final String CHFWREF_FINDORCREATE_THREADPOOL_01 = "00020012"; // F244595
    public static final String CHFWREF_DEF_OUTBOUND_BRIDGE_01 = "00020013"; // F244595
    public static final String CHFWREF_DEF_INBOUND_BRIDGE_01 = "00020014"; // F244595
    public static final String CHFWREF_ZOSSTARTUPRUNNABLE_01 = "00020015"; // D255410
    public static final String CHFWREF_PERFORMCRASTARTUP = "00020016"; // D255410
    public static final String CHFWREF_FINDCREATETHREADPOOL_01 = "00020017";
    public static final String CHFWREF_STARTINBOUNDCHAINS_01 = "00020018";
    public static final String CHFWREF_START_TCP_PBSIC_01 = "000200198";
    public static final String CHFWREF_INIT_TCP_PB_01 = "00020020";
    public static final String CHFWREF_FINDINSTANDALONE_02 = "00020021"; // PK62789
    public static final String CHFWREF_STARTINBOUNDCHAINS_02 = "00020022";

    // ClientConnectionManagerImpl
    public static final String CLNTCONNMGRIMPL_INITIALISE_01 = "00030001";
    public static final String CLNTCONNMGRIMPL_INITIALISE_02 = "00030002";
    public static final String CLNTCONNMGRIMPL_INITIALISE_03 = "00030003";
    // Connection
    public static final String CONNECTION_PHYSICALCLOSE_01 = "00040001";
    public static final String CONNECTION_PROCESSHEARTBEAT_01 = "00040002"; // F181603.2
    public static final String CONNECTION_PROCESSHEARTBEAT_02 = "00040003"; // F181603.2
    public static final String CONNECTION_PHYSICALCLOSE_02 = "00040004"; // D210978
    public static final String CONNECTION_INVALIDATE_01 = "00040005"; // D210978
    public static final String CONNECTION_PHYSICALCLOSE_03 = "00040006";
    // ConnectionReadCompletedCallback
    public static final String CONNREADCOMPCALLBACK_INVOKECALLBACK_01 = "00050001";
    public static final String CONNREADCOMPCALLBACK_INVOKECALLBACK_02 = "00050002";
    public static final String CONNREADCOMPCALLBACK_INVOKECALLBACK_03 = "00050003";
    public static final String CONNREADCOMPCALLBACK_INVOKECALLBACK_04 = "00050004";
    // public static final String CONNREADCOMPCALLBACK_ERROR_01               = "00050005";
    public static final String CONNREADCOMPCALLBACK_ERROR_02 = "00050006";
    public static final String CONNREADCOMPCALLBACK_ERROR_03 = "00050007";
    public static final String CONNREADCOMPCALLBACK_ERROR_04 = "00050008";
    public static final String CONNREADCOMPCALLBACK_PROCESSLOGICALCLOSE_01 = "00050009";
    public static final String CONNREADCOMPCALLBACK_PROCESSLOGICALCLOSE_02 = "00050010";
    public static final String CONNREADCOMPCALLBACK_PROCESSLOGICALCLOSE_03 = "00050011";
    public static final String CONNREADCOMPCALLBACK_PROCESSLOGICALCLOSE_04 = "00050012";
    public static final String CONNREADCOMPCALLBACK_COMPLETE_01 = "00050013";
    public static final String CONNREADCOMPCALLBACK_ERROR_05 = "00050014";
    public static final String CONNREADCOMPCALLBACK_COMPLETE_02 = "00050015";
    public static final String CONNREADCOMPCALLBACK_ERROR_06 = "00050016";
    // ConnectionWriteCompletedCallback
    public static final String CONNWRITECOMPCALLBACK_PRODDLE_01 = "00060001";
    public static final String CONNWRITECOMPCALLBACK_COMPLETE_01 = "00060002";
    public static final String CONNWRITECOMPCALLBACK_COMPLETE_02 = "00060003";
    public static final String CONNWRITECOMPCALLBACK_COMPLETE_03 = "00060004";
    public static final String CONNWRITECOMPCALLBACK_ERROR_01 = "00060005";
    public static final String CONNWRITECOMPCALLBACK_COMPLETE_04 = "00060006";
    public static final String CONNWRITECOMPCALLBACK_COMPLETE_05 = "00060007";

    // ConversationImpl
    public static final String CONVIMPL_CLOSE_01 = "00070001";
    public static final String CONVIMPL_CLOSE_02 = "00070002";
    public static final String CONVIMPL_WAKEUPALLWITHEXCP_01 = "00070003";
    public static final String CONVIMPL_WAKEUPALLWITHEXCP_02 = "00070004";
    public static final String CONVIMPL_WAKEUPALLWITHEXCP_03 = "00070005";
    public static final String CONVIMPL_PROCESSLOGICALCLOSE_01 = "00070006";
    public static final String CONVIMPL_PROCESSLOGICALCLOSE_02 = "00070007";
    public static final String CONVIMPL_PROCESSLOGICALCLOSE_03 = "00070008";
    public static final String CONVIMPL_PROCESSLOGICALCLOSE_04 = "00070009";
    public static final String CONVIMPL_PROCESSPING_01 = "00070010";
    public static final String CONVIMPL_PROCESSPING_02 = "00070011";
    public static final String CONVIMPL_PROCESSLOGICALREQUEST_01 = "00070012"; // D273932.1
    public static final String CONVIMPL_PROCESSLOGICALRESPONSE_01 = "00070013"; // D273932.1
    public static final String CONVIMPL_SEND_01 = "00070014";
    public static final String CONVIMPL_SEND_02 = "00070015";
    public static final String CONVIMPL_SEND_03 = "00070016";

    // JFapChannelFactory
    public static final String JFAPCHANNELFACT_GETDEVINTERFACE_01 = "00080001";
    public static final String JFAPCHANNELFACT_CREATECHANNEL_01 = "00080002";
    // JFapInboundConnLink
    public static final String JFAPINBOUNDCONNLINK_READY_01 = "00090001";
    public static final String JFAPINBOUNDCONNLINK_READY_02 = "00090002";
    public static final String JFAPINBOUNDCONNLINK_INIT_01 = "00090003"; // D196678.10.1
    public static final String JFAPINBOUNDCONNLINK_READY_03 = "00090004";
    // ListenerPortImpl
    public static final String LISTENERPORTIMPL_CLOSE_01 = "00100001";
    public static final String LISTENERPORTIMPL_CLOSE_02 = "00100002";
    // OutboundConnectionTracker
    public static final String OUTBOUNDCONNTRACKER_CLOSECONV_01 = "00110001";
    // ServerConnectionManagerImpl
    public static final String SRVRCONNMGRIMPL_LISTEN_01 = "00120001";
    public static final String SRVRCONNMGRIMPL_LISTEN_02 = "00120002";
    public static final String SRVRCONNMGRIMPL_LISTEN_03 = "00120003";
    public static final String SRVRCONNMGRIMPL_INITIALISE_01 = "00120004";
    public static final String SRVRCONNMGRIMPL_INITIALISE_02 = "00120005";
    public static final String SRVRCONNMGRIMPL_INITIALISE_03 = "00120006";
    public static final String SRVRCONNMGRIMPL_INITIALISE_04 = "00120007"; // F189000
    // end D181601

    // begin D185831
    // ConversationReceiveListenerDataReceivedInvocation
    public static final String CRLDATARECEIVEDINVOKE_INVOKE_01 = "00130001";
    public static final String CRLDATARECEIVEDINVOKE_GETTHREADCONTEXT_01 = "00130002"; // D202625
    // ConversationReceiveListenerErrorOccurredInvocation
    public static final String CRLERROROCCURREDINVOKE_INVOKE_01 = "00140001";
    // ReceiveListenerDataReceivedInvocation
    public static final String RLDATARECEIVEDINVOKE_INVOKE_01 = "00150001";
    // ReceiveListenerErrorOccurredInvocation
    public static final String RLERROROCCURREDINVOKE_INVOKE_01 = "00160001";
    // end D185831

    // begin F181603.2
    // InboundTransmissionParser
    public static final String INBOUNDXMITPARSER_PARSE_01 = "00170001";
    public static final String INBOUNDXMITPARSER_PARSEPRIMHDR_01 = "00170002";
    public static final String INBOUNDXMITPARSER_PARSEPRIMHDR_02 = "00170003";
    public static final String INBOUNDXMITPARSER_PARSEPRIMHDR_03 = "00170004";
    public static final String INBOUNDXMITPARSER_PARSEPRIMHDR_04 = "00170005";
    public static final String INBOUNDXMITPARSER_PARSEPRIMHDR_05 = "00170006";
    public static final String INBOUNDXMITPARSER_PARSEPRIMHDR_06 = "00170007";
    public static final String INBOUNDXMITPARSER_PARSECONVHDR_01 = "00170008";
    public static final String INBOUNDXMITPARSER_PARSECONVHDR_02 = "00170009";
    public static final String INBOUNDXMITPARSER_PARSECONVHDR_03 = "00170010";
    public static final String INBOUNDXMITPARSER_PARSESSHDR_01 = "00170011";
    public static final String INBOUNDXMITPARSER_PARSESSPAYLOAD_01 = "00170012";
    public static final String INBOUNDXMITPARSER_PARSESMPAYLOAD_01 = "00170013";
    public static final String INBOUNDXMITPARSER_PARSESMPAYLOAD_02 = "00170014";
    public static final String INBOUNDXMITPARSER_PARSESEPAYLOAD_01 = "00170015";
    public static final String INBOUNDXMITPARSER_PARSESEPAYLOAD_02 = "00170016";
    public static final String INBOUNDXMITPARSER_DISPCONV_01 = "00170017";
    public static final String INBOUNDXMITPARSER_DISPCONV_02 = "00170018";
    public static final String INBOUNDXMITPARSER_DISPCONV_03 = "00170019";
    public static final String INBOUNDXMITPARSER_DISPCONVLST_01 = "00170020";
    public static final String INBOUNDXMITPARSER_DISPCONVLST_02 = "00170020";
    public static final String INBOUNDXMITPARSER_ALLOCATE_01 = "00170021";
    // end F181603.2

    // BufferPoolManagerReference
    public static final String BUFFERPOOLMGRREF_CREATEINTHINCLIENT_01 = "00180001";
    public static final String BUFFERPOOLMGRREF_FINDCLIENTCONT_01 = "00180002"; // D221079
    public static final String BUFFERPOOLMGRREF_FINDCLIENTCONT_02 = "00180003"; // D221079

    // NonThreadSwitchingDispatchable
    public static final String NONTSDISPATCHABLE_STINIT_01 = "00190001"; // F201521

    // JFapOutboundConnLink
    public static final String JFAPOUTBOUNDCONNLINK_INIT_01 = "00200001"; // D196678.10.1

    // DispatchToAllNonEmptyDispatchable
    public static final String DISPATCHTOALLNONEMPTY_STINIT_01 = "00210001"; // D213108

    public static final String DISPATCHRUNNABLE_RUN_01 = "00210002";

    // ReceiveListenerDispatcher
    public static final String RLDISPATCHER_QUEUEINVOCCOMMON_01 = "00220001"; // D213108

    // ReceiveListenerDispatchQueue
    public static final String RLDISPATCHQUEUE_RUN_01 = "00230001"; // F248849

    // ConnectionDataGroup
//   public static final String CONNDATAGROUP_CONNECT_01                    = "00240001";   // D197042
    public static final String CONNDATAGROUP_CONNECT_02 = "00240002"; // D197042
    public static final String CONNDATAGROUP_CONNECT_03 = "00240003"; // D232185
    public static final String CONNDATAGROUP_CONNECT_04 = "00240004"; // D244595
//   public static final String CONNDATAGROUP_CONNECT_05                    = "00240004";   // D249096.1
//   public static final String CONNDATAGROUP_CONNECT_06                    = "00240004";   // D249096.1
    public static final String CONNDATAGROUP_CONNECT_07 = "00240004"; // D244096.1

    // Framework
    public static final String FRAMEWORK_GETINSTANCE_01 = "00250001";
    public static final String FRAMEWORK_GETINSTANCE_02 = "00250002";
    public static final String FRAMEWORK_GETTHREADPOOL_01 = "00250003";
    public static final String FRAMEWORK_GETTHREADPOOL_02 = "00250004";
    public static final String FRAMEWORK_GETAPPROXTIME_01 = "00250005";
    public static final String FRAMEWORK_GETAPPROXTIME_02 = "00250006";
    public static final String FRAMEWORK_GETALARMMGR_01 = "00250007";
    public static final String FRAMEWORK_GETALARMMGR_02 = "00250008";
    // WsByteBufferPool
    public static final String WSBYTEBUFFERPOOL_GETINSTANCE_01 = "00260001";
    public static final String WSBYTEBUFFERPOOL_GETINSTANCE_02 = "00260002";
    // CFWNetworkConnectionFactory
    public static final String CFWNETWORKCONNECTIONFACT_CREATECONN_01 = "00270001";
    public static final String CFWNETWORKCONNECTIONFACT_CREATECONN_02 = "00270002";
    // RichClientTransportFactory
    public static final String RICHCLIENTTRANSPORTFACT_GETCONNFACBYNAME_01 = "00280001";
    // RichClientFramework
    public static final String RICHCLIENTFRAMEWORK_PREPAREEP_01 = "00290001";
    public static final String RICHCLIENTFRAMEWORK_PREPAREEP_02 = "00290002";
    public static final String RICHCLIENTFRAMEWORK_PREPAREEP_03 = "00290003";
    public static final String RICHCLIENTFRAMEWORK_CLONE_01 = "00290004";
    public static final String RICHCLIENTFRAMEWORK_CLONE_02 = "00290005";

    // AlarmInfoWrapper
    public static final String ALARMINFOWRAPPER_FIRE_01 = "00300001";
    //JFapAddress                                                                                                 PK58698
    public static final String JFAPADDRESS_GETCONNECTTIMEOUT_01 = "00310001"; //PK58698
}
