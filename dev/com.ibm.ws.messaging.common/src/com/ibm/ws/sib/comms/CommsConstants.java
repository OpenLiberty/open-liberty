/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms;

import com.ibm.ws.sib.utils.RuntimeInfo;

/**
 * This class just contains component-wide constants.
 * It does not include any constants used by other components.
 */
public class CommsConstants
{
    public final static String MSG_GROUP_FAP_FLOW = com.ibm.ws.sib.utils.TraceGroups.TRGRP_COMMS_FAP_FLOW;
    public final static String MSG_GROUP = com.ibm.ws.sib.utils.TraceGroups.TRGRP_COMMS;
    public final static String MSG_BUNDLE = "com.ibm.ws.sib.comms.CWSICMessages"; // f195445.8 // D217339

    protected final static String JS_COMMS_CLIENT_CONNECTION_FACTORY_CLASS =
                    "com.ibm.ws.sib.comms.client.ClientConnectionFactoryImpl"; // D219803

    //F172397
    protected final static String JS_COMMS_ME_CONNECTION_FACTORY_CLASS =
                    "com.ibm.ws.sib.comms.server.mesupport.MEConnectionFactoryImpl"; // D219803

    //f183052.2
    public final static String JS_COMMS_MQ_LINK_MANAGER_CLASS =
                    "com.ibm.ws.sib.comms.mq.link.MQLinkManagerImpl";

    public final static String JS_COMMS_MQ_UTILITIES_CLASS =
                    "com.ibm.ws.sib.comms.mq.util.MQUtilitiesImpl";

    public final static String SERVER_DIAG_MODULE_CLASS =
                    "com.ibm.ws.sib.comms.client.ServerCommsDiagnosticModule";

    // **** Runtime configuration constants ****//

    // These constants are configuration properties that can be altered at runtime. These properties
    // are designed to be tweaked if needed, but should not need to be.
    // Start D235891
    private final static String SIBPF = RuntimeInfo.SIB_PROPERTY_PREFIX +
                                        "comms" +
                                        RuntimeInfo.SIB_PROPERTY_SEPARATOR;

    private static final String CLIENTPF = RuntimeInfo.SIB_PROPERTY_PREFIX +
                                           "client" +
                                           RuntimeInfo.SIB_PROPERTY_SEPARATOR;

    public final static String ASYNC_THREADPOOL_CONF_KEY = SIBPF + "AsyncThreadPoolSize"; // f192215
    public final static String ASYNC_THREADPOOL_SIZE = "10"; // f192215
    public final static String INLINE_ASYNC_CBACKS_KEY = SIBPF + "InlineAsyncCBacks"; // f192654
    public final static String INLINE_ASYNC_CBACKS = Boolean.toString(true); // D271523
    public final static String EXCHANGE_TX_SEND_KEY = SIBPF + "ForceTransactedSendExchange"; // f192829
    public final static String EXCHANGE_TX_SEND = "false"; // f192829
    public final static String EXCHANGE_EXPRESS_END_KEY = SIBPF + "ForceExpressSendExchange"; // f192829
    public final static String EXCHANGE_EXPRESS_SEND = "false"; // f192829
    public final static String FLUSH_RH_RECV_WAIT_KEY = SIBPF + "ForceRHRecvWithWaitFlush"; // f192829
    public final static String FLUSH_RH_RECV_WAIT = "false"; // f192829

    // When set to true - "optimized" transactions (i.e. don't send a start transaction flow and
    // don't start or end global transaction branches) will be disabled.
    public final static String DISABLE_OPTIMIZED_TX_KEY = SIBPF + "DisableOptimizedTransactions";
    public final static String DISABLE_OPTIMIZED_TX = "false";

    // These can be used to allow the sib.properties file to override the default heartbeat settings
    // that the client will negotiate with the server.
    // Note that heartbeats are negotiated upwards (to whoever has the highest value) so if you
    // want to set them to very low values you will need to ensure they are also set to low values
    // on the server
    public final static String HEARTBEAT_INTERVAL_KEY = SIBPF + "HeartbeatInterval";
    public final static String HEARTBEAT_TIMEOUT_KEY = SIBPF + "HeartbeatTimeout";

    // Start D214620
    // These parameters are needed to tune the read ahead algorithm. More information is
    // available in the ReadAheadQueue javadoc or on the JetStream Teamroom
    public final static String RA_HIGH_QUEUE_BYTES_KEY = SIBPF + "RAHighQueueBytes"; // f192759.2
    public final static String RA_HIGH_QUEUE_BYTES = "574"; // f192759.2
    public final static String RA_LOW_QUEUE_BYTES_FACTOR_KEY = SIBPF + "RALowQueueBytesFactor";
    public final static String RA_LOW_QUEUE_BYTES_FACTOR = "0.5";
    public final static String RA_HIGH_QUEUE_THRESH_KEY = SIBPF + "RAHighQueueBytesThreshold";
    public final static String RA_HIGH_QUEUE_THRESH = "0";
    public final static String RA_HIGH_QUEUE_BYTES_MAX_KEY = SIBPF + "RAHighQueueBytesMax";
    public final static String RA_HIGH_QUEUE_BYTES_MAX = "5740";
    public final static String RA_HIGH_QUEUE_BYTES_TO_KEY = SIBPF + "RAHighQueueBytesTimeOut";
    public final static String RA_HIGH_QUEUE_BYTES_TO = "2000";
    // End D214620

    // These SIB properties allow the modification of the capabilities that we inform our peer
    // about during our initial handshake phase.
    public static final String CAPABILITIY_REQUIRES_NONJAVA_BOOTSTRAP_KEY = CLIENTPF + "NonJavaBootstrap";
    public static final String CAPABILITIY_REQUIRES_NONJAVA_BOOTSTRAP_DEF = Boolean.toString(false);
    public static final String CAPABILITIY_REQUIRES_JMS_MESSAGES_KEY = CLIENTPF + "JMFOnly";
    public static final String CAPABILITIY_REQUIRES_JMS_MESSAGES_DEF = Boolean.toString(false);
    public static final String CAPABILITIY_REQUIRES_JMF_ENCODING_KEY = CLIENTPF + "JMSOnly";
    public static final String CAPABILITIY_REQUIRES_JMF_ENCODING_DEF = Boolean.toString(false);

    // These can be used to allow the sib.properties file to override the FAP version we support.
    // Different values can be specified for both the client and the server.
    public final static String SERVER_FAP_LEVEL_KEY = SIBPF + "ServerFapLevel";
    public final static String CLIENT_FAP_LEVEL_KEY = SIBPF + "ClientFapLevel";

    // Strict ordering of message redelivery (for JMS spec compliance in Session.recover)
    public final static String STRICT_REDELIVERY_KEY = SIBPF + "StrictRedeliveryOrdering";
    public final static String STRICT_REDELIVERY_DEFAULT = "false";

    public static final int SEG_REQUEST_SCHEMA = 0x6E; // F247845
    // Start D217374
    /**
     * The FAP Version of this client - please consider incrementing this in steps of
     * more than one. See description of SUPPORTED_FAP_VERSIONS for justification
     * Note that the supported fap versions table needs to be updated too...
     */
    // Venu Liberty COMMS  
    // directly assigning value .. to separate JFapChannelConstants
    //updated version for shared non durable
    public static final short CURRENT_FAP_VERSION = 20;

    /**
     * FAP versions supported. It may appear a little odd that we have both a current fap version
     * and a set of supported versions. This came about because of a problem that occured during
     * version 6.1 development where a 6.0.1 APAR fix could have required a change to the FAP.
     * Fortunately an alternative solution was found - however, this could have been a real problem.
     * This is because the 6.0.1 FAP level was 1 - and the APAR fix would have needed to bump the
     * FAP level up to 2. However 6.0.2 had already been shipped with a FAP level of 2 - so there
     * would be no way to determine if a FAP level of 2 meant support for 6.0.2 features or
     * support for the 6.0.1 APAR fix.
     * <p>
     * As of FAP version 3 (which ironically was a version reserved for service...) the party
     * wishing to establish a connection must send all the FAP versions it supports as part of
     * the handshake (this takes the form of a big-endian bitmap). The receipient will then
     * select the highest
     */
    private static final boolean SUPPORTED_FAP_VERSIONS[] =
    {
     true, // 1: supported (WAS 6.0)
     true, // 2: supported (WAS 6.0.2)
     true, // 3: supported (WAS 6.0.2.11)
     false, // 4: not supported (reserved for future 602 service)
     true, // 5: supported (WAS 6.1)
     true, // 6: supported (WAS 6.1.0.2)
     true, // 7: supported (WAS 6.1.0.23, PK73713)
     false, // 8: not supported (reserved for future 61 service)
     true, // 9: supported (WAS 7.0)
     true, //10: supported (WAS 7.0.0.3)
     false, //11: not supported (reserved for future 7* service)
     false, //12: not supported (reserved for future 7* service)
     true, //13: supported (WAS 8)
     true, //14: supported (WAS 8.0.0.2)
     true, //15: supported (WAS 8.0.0.4)
     false, //16: not supported (reserved for future 8* service)
     false,//17: not supported (reserved for future 8.5.5* service)
     false,//18: not supported (reserved for future 8.5.5* service)
     false, //19:  not supported (reserved for future 8.5.5* service)
     true //20: supported (WAS 9)
    };

    public static final boolean isFapLevelSupported(int level)
    {
        boolean result = false;
        if ((level > 0) && (level <= SUPPORTED_FAP_VERSIONS.length))
            result = SUPPORTED_FAP_VERSIONS[level - 1];
        return result;
    }

    // The first bit that comes in the handshake flow to indicate what we are
    public static final byte HANDSHAKE_CLIENT = 0x01;
    public static final byte HANDSHAKE_ME = 0x02;

    // Handshake paramters
    public static final short FIELDID_PRODUCT_VERSION = 0x0001;
    public static final short FIELDID_FAP_LEVEL = 0x0002;
    public static final short FIELDID_MAX_MESSAGE_SIZE = 0x0003;
    public static final short FIELDID_MAX_TRANSMISSION_SIZE = 0x0004;
    public static final short FIELDID_HEARTBEAT_INTERVAL = 0x0005;
    public static final short FIELDID_SOURCE_ME_NAME = 0x0006; // Not used
    public static final short FIELDID_CAPABILITIES = 0x0007;
    public static final short FIELDID_CONNECTION_OBJECT_ID = 0x0008; // Not used
    public static final short FIELDID_USERID = 0x0009; // Not used
    public static final short FIELDID_PASSWORD = 0x000A; // Not used
    public static final short FIELDID_PRODUCT_ID = 0x000B;
    public static final short FIELDID_SUPORTED_FAPS = 0x000C;
    public static final short FIELDID_HEARTBEAT_TIMEOUT = 0x000D;

    /**
     * Used to indicate how the conversation which performs the
     * handshake is to be used.
     */
    public static final short FIELDID_CONVERSATION_USAGE_TYPE = 0x000E;
    public static final short FIELDID_CELL_NAME = 0x000F;
    public static final short FIELDID_NODE_NAME = 0x0010;
    public static final short FIELDID_SERVER_NAME = 0x0011;
    public static final short FIELDID_CLUSTER_NAME = 0x0012;

    // begin F247975
    /** Bit mask for reserved capability bits */
    public static final short CAPABILITIY_RESERVED_MASK = (short) 0xFF80;
    /** Capability bit - supports transactions */
    public static final short CAPABILITIY_SUPPORTS_TRANSACTIONS = (short) 0x0001;
    /** Capability bit - supports reliable messages */
    public static final short CAPABILITIY_SUPPORTS_RELIABLE_MSGS = (short) 0x0002;
    /** Capability bit - supports assured messages */
    public static final short CAPABILITIY_SUPPORTS_ASSURED_MSG = (short) 0x0004;
    /**
     * Capability bit - requires non-Java bootstrap. When this capabilities bit is set, the
     * client requires that no Java specific functionality (for example Java serialisation)
     * is used during the bootstrap process.
     */
    public static final short CAPABILITIY_REQUIRES_NONJAVA_BOOTSTRAP = (short) 0x0008;
    /**
     * Capability bit - requires JMS formatted messages. When this is set we must assume
     * that our peer cannot process messages that are not JMS messages. Thus we may need
     * to take the appropriate action to convert / not deliver non-JMS messages
     */
    public static final short CAPABILITIY_REQUIRES_JMS_MESSAGES = (short) 0x0010;
    /**
     * Capability bit - requires JMF encoded messages. When this bit is set we must assume
     * that our peer cannot process messages that are not encoded using JMF encoding. Thus
     * we may need to re-encode any messages transfered.
     */
    public static final short CAPABILITIY_REQUIRES_JMF_ENCODING = (short) 0x0020;
    // end F247975

    /**
     * Capability bit - requires optimized transactions. When this bit is set we must
     * assume that out peer will encode the start of a transaction with the first unit
     * of work, and in the case of XA transactions not send start and end flows.
     * This changes the format of any transmission that carries transactional data.
     */
    public static final short CAPABILITIY_REQUIRES_OPTIMIZED_TX = (short) 0x0040;

    /**
     * The value that we would describe as 'default' capabilities for this client.
     */
    public static final short CAPABILITIES_DEFAULT =
                    CommsConstants.CAPABILITIY_SUPPORTS_TRANSACTIONS | // Supports transactions (always the case)
                                    CommsConstants.CAPABILITIY_SUPPORTS_RELIABLE_MSGS | // Supports reliable messages (always the case)
                                    CommsConstants.CAPABILITIY_SUPPORTS_ASSURED_MSG | // Supports assured messages (always the case)
                                    CommsConstants.CAPABILITIY_REQUIRES_OPTIMIZED_TX; // Requires optimized transactions (FAP 4 and up)

    /**
     * Not currently used - used on handshake to indicate the maximum message size that can be
     * accepted.
     */
    public static final long MAX_MESSAGE_SIZE = 10000;

    /**
     * Not currently used - used on handshake to indicate the maximum transmission size that can be
     * used.
     */
    public static final int MAX_TRANSMISSION_SIZE = 10000;

    // Product ID's flowed in the handshake
    public static final short PRODUCT_ID_JETSTREAM = 0x0001;
    public static final short PRODUCT_ID_XMS = 0x0002;
    public static final short PRODUCT_ID_DOTNET = 0x0003;

    // Event ID's that are asynchronously sent to us by the server code
    public static final short EVENTID_CONNECTION_QUIESCING = 0x0001;
    public static final short EVENTID_ME_QUIESCING = 0x0002;
    public static final short EVENTID_ME_TERMINATED = 0x0003;
    public static final short EVENTID_ASYNC_EXCEPTION = 0x0004;

    // Browser flags used when creating a browser                 SIB0113.comms.1
    // NOTE: When you add a flag - don't forget to update the BF_MAX_VALID
    // value to ensure we are always checking for legals values
    public static final short BF_ALLOW_GATHERING = 0x0001;

    public static final short BF_MAX_VALID = 0x0001;

    // Consumer flags used when creating a consumer
    // NOTE: When you add a flag - don't forget to update the CF_MAX_VALID
    // value to ensure we are always checking for legals values
    public static final short CF_READAHEAD = 0x0001;
    public static final short CF_NO_LOCAL = 0x0002;
    public static final short CF_MULTI_CONSUMER = 0x0004;
    public static final short CF_UNICAST = 0x0008;
    public static final short CF_MULTICAST = 0x0010;
    public static final short CF_RELIABLE_MULTICAST = 0x0020;
    public static final short CF_BIFURCATABLE = 0x0040; // F219476.2
    public static final short CF_IGNORE_INITIAL_INDOUBTS = 0x0080; // D351339.comms
    public static final short CF_ALLOW_GATHERING = 0x0100; //SIB0113.comms.1

    public static final short CF_MAX_VALID = 0x01FF; //D237047,SIB0113.comms.1

    // Producer flags used when creating a browser                SIB0113.comms.1
    // NOTE: When you add a flag - don't forget to update the PF_MAX_VALID
    // value to ensure we are always checking for legals values
    public static final short PF_BIND_TO_QUEUE_POINT = 0x0001;
    public static final short PF_PREFER_LOCAL_QUEUE_POINT = 0x0002;

    public static final short PF_MAX_VALID = 0x0003;

    public static final byte DESTADDR_ISFROMMEDIATION = 1; // D255694

    // Exception constants
    public static final short SI_NO_EXCEPTION = 0x0000;
    public static final short SI_EXCEPTION = 0x0001;
    public static final short SI_INCORRECT_CALL_EXCEPTION = 0x0002;
    public static final short SI_INVALID_DESTINATION_PREFIX_EXCEPTION = 0x0003;
    public static final short SI_DISCRIMINATOR_SYNTAX_EXCEPTION = 0x0004;
    public static final short SI_SELECTOR_SYNTAX_EXCEPTION = 0x0005;
    public static final short SI_INSUFFICIENT_DATA_FOR_FACT_EXCEPTION = 0x0006;
    public static final short SI_AUTHENTICATION_EXCEPTION = 0x0007;
    public static final short SI_NOT_POSSIBLE_IN_CUR_CONFIG_EXCEPTION = 0x0008;
    public static final short SI_NOT_AUTHORISED_EXCEPTION = 0x0009;
    public static final short SI_SESSION_UNAVAILABLE_EXCEPTION = 0x000A;
    public static final short SI_SESSION_DROPPED_EXCEPTION = 0x000B;
    public static final short SI_DURSUB_ALREADY_EXISTS_EXCEPTION = 0x000C;
    public static final short SI_DURSUB_MISMATCH_EXCEPTION = 0x000D;
    public static final short SI_DURSUB_NOT_FOUND_EXCEPTION = 0x000E;
    public static final short SI_CONNECTION_UNAVAILABLE_EXCEPTION = 0x000F;
    public static final short SI_CONNECTION_DROPPED_EXCEPTION = 0x0010;
    public static final short SI_DATAGRAPH_FORMAT_MISMATCH_EXCEPTION = 0x0011;
    public static final short SI_DATAGRAPH_SCHEMA_NOT_FOUND_EXCEPTION = 0x0012;
    public static final short SI_DESTINATION_LOCKED_EXCEPTION = 0x0013;
    public static final short SI_TEMPORARY_DEST_NOT_FOUND_EXCEPTION = 0x0014;
    public static final short SI_MESSAGE_EXCEPTION = 0x0015;
    public static final short SI_RESOURCE_EXCEPTION = 0x0016;
    public static final short SI_LIMIT_EXCEEDED_EXCEPTION = 0x0017;
    public static final short SI_CONNECTION_LOST_EXCEPTION = 0x0018;
    public static final short SI_ROLLBACK_EXCEPTION = 0x0019;
    public static final short SI_NOT_SUPPORTED_EXCEPTION = 0x001A;
    public static final short SI_MSG_DOMAIN_NOT_SUPPORTED_EXCEPTION = 0x001B;
    public static final short SI_DATAGRAPH_EXCEPTION = 0x001C;
    public static final short SI_ERROR_EXCEPTION = 0x001D;
    public static final short SI_COMMAND_INVOCATION_FAILED_EXCEPTION = 0x001E; // SIB0009.comms.1
    public static final short SI_MESSAGE_NOT_LOCKED_EXCEPTION = 0x001F;

    public static final short EXCEPTION_INTERNAL_ERROR = 0x0100;
    public static final short EXCEPTION_XAEXCEPTION = 0x0101;

    public static final short DATAID_EXCEPTION_PROBEID = 0x0001;
    public static final short DATAID_EXCEPTION_MESSAGE = 0x0002;
    public static final short DATAID_EXCEPTION_REASON = 0x0003;

    /** This is flown across the wire when there is no transaction present */
    public static final int NO_TRANSACTION = 0;

    /** This is flown across the wire when there is no order context present */
    public static final short NO_ORDER_CONTEXT = 0;

    /** This is flown across the wire when there is no destination type */
    public static final short NO_DEST_TYPE = -1;
    // End D217372

    public static final short NO_DEST_AVAIL = -1; //SIB0137.comms.2

    ////////////////////////////////////////////////////////////////////////
    //// Bits used in the flags field of an optimized transaction flow
    // Transacted bit - when this bit is set the operation is transacted.
    // If this bit is clear then the client specified a null transaction.
    public static final int OPTIMIZED_TX_FLAGS_TRANSACTED_BIT = 0x00000001;
    // Local transaction - the optimized transaction is a local (uncoordinated)
    // transaction when this bit it set.  When this bit is cleared the
    // transaction is a global (XA) transaction.
    public static final int OPTIMIZED_TX_FLAGS_LOCAL_BIT = 0x00000002;
    // Create transaction - when set a new transaction should be created
    // before performing the operation.  When cleared - the operation is
    // being performed in the scope of an existing transaction.
    public static final int OPTIMIZED_TX_FLAGS_CREATE_BIT = 0x00000004;
    // End previous transaction - when set the current XA transaction should
    // be ended before performing the operation.
    public static final int OPTIMIZED_TX_END_PREVIOUS_BIT = 0x00000008;
    // Local transaction allows subordinates flag - when set the creation
    // of a local transaction will supply the "allow subordinates" flag.
    public static final int OPTIMIZED_TX_FLAGS_SUBORDINATES_ALLOWED = 0x00000010;
    ////////////////////////////////////////////////////////////////////////

    /** This constant is used in ME-ME messages to denote a normal JsMessage */
    public static final byte MEME_JSMESSAGE = (byte) 0x00;

    /** This constant is used in ME-ME messages to denote a ControlMessage */
    public static final byte MEME_CONTROLMESSAGE = (byte) 0x01;

    // These constants are used by the async consumer
    public static final short ASYNC_START_OR_MID_BATCH = 0x0000;
    public static final short ASYNC_LAST_IN_BATCH = 0x0001;

    // These flags are used when sending messages in chunks
    public static final byte CHUNKED_MESSAGE_FIRST = (byte) 0x01;
    public static final byte CHUNKED_MESSAGE_MIDDLE = (byte) 0x02;
    public static final byte CHUNKED_MESSAGE_LAST = (byte) 0x04;

    /** The minimum size of a message that is sent in chunks (1Mb) */
    public static final int MINIMUM_MESSAGE_SIZE_FOR_CHUNKING = 1024000;

    // **** FFDC Probe ID Constants **** //

    // The following constants are to be used in the FFDC probe ID field.
    // Everytime a FFDC is thrown, it should use a constant in it's probe ID
    // field that is defined here. That way, even if code line number change
    // it can still be easily identified where the FFDC is being generated
    // from.
    // Probe Id's should follow the numbering convention that is:
    // <component>-<unique id for class>-<unique id for probe>
    // The components are:
    //  1 - Client
    //  2 - Client proxy queues
    //  3 - Server
    //  4 - ME->ME
    //  5 - Common (used across more than one above component)
    // Probe Id' should also remain unique across releases.

    // ***** Client side ***** //
//   public static final String CLIENTSIDECONNECTION_CONNECT_01              = "1-001-0001";
    public static final String CLIENTSIDECONNECTION_CONNECT_02 = "1-001-0002";
    public static final String CLIENTSIDECONNECTION_CONNECT_03 = "1-001-0003";
    public static final String CLIENTSIDECONNECTION_CONNECT_04 = "1-001-0004";
    public static final String CLIENTSIDECONNECTION_CONNECT_05 = "1-001-0005";
    public static final String CLIENTSIDECONNECTION_CONNECT_06 = "1-001-0006";
    public static final String CLIENTSIDECONNECTION_CLOSE_01 = "1-001-0007";
    public static final String CLIENTSIDECONNECTION_TRMHANDSHAKEEXCHANGE_01 = "1-001-0008";
    public static final String CLIENTSIDECONNECTION_SETSICONN_01 = "1-001-0009";
    public static final String CLIENTSIDECONNECTION_SENDMFPSCHEMA_01 = "1-001-0010";//D234369
    public static final String CLIENTSIDECONNECTION_REQUESTMFPSCHEMA_01 = "1-001-0011";//F247845

//   public static final String CONNECTIONPROXY_SEND_01                      = "1-002-0001";
//   public static final String CONNECTIONPROXY_SENDTOEXCEPTIONDEST_01       = "1-002-0002";
    public static final String CONNECTIONPROXY_STATICINIT_01 = "1-002-0003";
//   public static final String CONNECTIONPROXY_RECEIVEWITHWAIT_01           = "1-002-0004";
    public static final String CONNECTIONPROXY_GETDESTINATIONDEF_01 = "1-002-0005";
    public static final String CONNECTIONPROXY_INVOKECMD_01 = "1-002-0006";// SIB0009.comms.1
    public static final String CONNECTIONPROXY_INVOKECMD_02 = "1-002-0007";// SIB0009.comms.1
    public static final String CONNECTIONPROXY_SENDCHUNKED_01 = "1-002-0008";
    public static final String CONNECTIONPROXY_SENDCHUNKEDEXCEPTION_01 = "1-002-0009";
    public static final String CONNECTIONPROXY_STATICINIT_02 = "1-002-0010";

    public static final String CONSUMERSESSIONPROXY_RCVNOWAIT_01 = "1-003-0001";
    public static final String CONSUMERSESSIONPROXY_RCVWITHWAIT_01 = "1-003-0002";
    public static final String CONSUMERSESSIONPROXY_STATICINIT_01 = "1-003-0003";
    public static final String CONSUMERSESSIONPROXY_REGASYNC_01 = "1-003-0004";
//   public static final String CONSUMERSESSIONPROXY_PERFORMRCV_01           = "1-003-0005";
    public static final String CONSUMERSESSIONPROXY_PERFORMRCV_02 = "1-003-0006";
    public static final String CONSUMERSESSIONPROXY_LOCKMSG_01 = "1-003-0007";
//   public static final String CONSUMERSESSIONPROXY_UNLOCKMSG_01            = "1-003-0008";
    public static final String CONSUMERSESSIONPROXY_SESSION_STOPPED_01 = "1-003-0009";
    public static final String CONSUMERSESSIONPROXY_SESSION_STOPPED_02 = "1-003-0010";
    public static final String CONSUMERSESSIONPROXY_SESSION_STOPPED_03 = "1-003-0011";
    public static final String CONSUMERSESSIONPROXY_INCALLBACKACT_01 = "1-003-0012";

//   public static final String PRODUCERSESSIONPROXY_SEND_01                 = "1-004-0001";
    public static final String PRODUCERSESSIONPROXY_STATICINIT_01 = "1-004-0002";
    public static final String PRODUCERSESSIONPROXY_SENDCHUNKED_01 = "1-004-0003";

    public static final String BROWSERSESSION_NEXT_01 = "1-005-0001";

//   public static final String BIFCONSUMERSESSION_GETJMOFROMBUFFER_01       = "1-006-0001";

    public static final String SIXARESOURCEPROXY_COMMIT_01 = "1-007-0001";
    public static final String SIXARESOURCEPROXY_END_01 = "1-007-0002";
    public static final String SIXARESOURCEPROXY_FORGET_01 = "1-007-0003";
    public static final String SIXARESOURCEPROXY_GETTXTIMEOUT_01 = "1-007-0004";
    public static final String SIXARESOURCEPROXY_PREPARE_01 = "1-007-0005";
    public static final String SIXARESOURCEPROXY_RECOVER_01 = "1-007-0006";
    public static final String SIXARESOURCEPROXY_ROLLBACK_01 = "1-007-0007";
    public static final String SIXARESOURCEPROXY_SETTXTIMEOUT_01 = "1-007-0008";
    public static final String SIXARESOURCEPROXY_START_01 = "1-007-0009";

    public static final String PROXYRECEIVELISTENER_DATARCVD_01 = "1-008-0001";
    public static final String PROXYRECEIVELISTENER_DATARCVD_02 = "1-008-0002";
    public static final String PROXYRECEIVELISTENER_PROCESSMSG_01 = "1-008-0003";
    public static final String PROXYRECEIVELISTENER_PROCESSMSG_02 = "1-008-0004";
    public static final String PROXYRECEIVELISTENER_PROCESSEVENT_01 = "1-008-0005";
    public static final String PROXYRECEIVELISTENER_PROCESSEVENT_02 = "1-008-0006";
    public static final String PROXYRECEIVELISTENER_PROCESSEVENT_03 = "1-008-0007";
    public static final String PROXYRECEIVELISTENER_PROCESSSCHEMA_01 = "1-008-0008";
    public static final String PROXYRECEIVELISTENER_DESTLIST_CALLBACK_02 = "1-008-0010"; //SIB0137.comms.3
    public static final String PROXYRECEIVELISTENER_SESSION_STOPPED_01 = "1-008-0011"; //SIB0115d.comms
    public static final String PROXYRECEIVELISTENER_SESSION_STOPPED_02 = "1-008-0012"; //SIB0115d.comms

    public static final String PROXYRECEIVELISTENER_CONSUMERMON_CALLBACK_01 = "1-008-0013"; //F011127

    public static final String CONNECTIONPROPS_GETWLMEP_01 = "1-009-0001";
    public static final String CONNECTIONPROPS_GETEP_01 = "1-009-0002";
    public static final String CONNECTIONPROPS_GETCHAIN_01 = "1-009-0003";

    public static final String PROXY_GETPROXYID_01 = "1-010-0001";
    public static final String PROXY_GETCONNECTIONPROXY_01 = "1-010-0002";

    public static final String COMMS_ADMIN_START_01 = "1-011-0001";// D225856

    public static final String ORDERCONTEXTPROXY_CREATE_01 = "1-012-0001";// D241156
    public static final String ORDERCONTEXTPROXY_CLOSE_01 = "1-012-0002";// D241156

    public static final String OPTRESOURCEPROXY_COMMIT_01 = "1-013-0001";
    public static final String OPTRESOURCEPROXY_SETENDNOTREQUIRED_01 = "1-013-0002";
    public static final String OPTRESOURCEPROXY_GETENDFLAGS_01 = "1-013-0003";

    public static final String OPTUNCOORDPROXY_GETXID_01 = "1-014-00001";
    public static final String OPTUNCOORDPROXY_ISENDREQUIRED_01 = "1-014-00002";
    public static final String OPTUNCOORDPROXY_SETENDNOTREQUIRED_01 = "1-014-00003";
    public static final String OPTUNCOORDPROXY_GETENDFLAGS_01 = "1-014-00004";

    public static final String CLIENTASYNCHEVENTTHREADPOOL_INVOKE_01 = "1-015-00001";
    public static final String DESTINATIONLISTENERTHREAD_RUN_01 = "1-015-00002";

    public static final String SUSPENDABLEXARESOURCE_START_01 = "1-016-0001";

    public static final String ASYNC_CON_THREADPOOL_SCHQ_RUN_01 = "1-017-0001"; //PM12356
    public static final String ASYNC_CON_THREADPOOL_SCHQ_RUN_02 = "1-017-0002"; //PM12356
    public static final String ASYNC_CON_THREADPOOL_SCHQ_RUN_03 = "1-017-0003"; //PM12356

    public static final String CONSUMERSETCHANGECALLBACKTHREAD_RUN_01 = "1-018-00001"; //F011127

    // ***** Proxy-queue related ***** //
    public static final String ORDEREDPROXYQUEUEIMPL_CLOSE_01 = "2-001-0001";

    public static final String PROXYQUEUECONVGROUPIMPL_CLOSE_01 = "2-002-0001";
    public static final String PROXYQUEUECONVGROUPIMPL_NOTIFYCLOSE_01 = "2-002-0002";

    public static final String ASYNC_THREADPOOL_HASCAPACITY_01 = "2-003-0001";

    public static final String ASYNC_DISPATCHER_DELIVERMSGS_01 = "2-004-0001";
    public static final String ASYNC_DISPATCHER_DELIVERMSGS_02 = "2-004-0002";

    public static final String CONVERSATIONHELPERIMPL_01 = "2-005-0001";
    public static final String CONVERSATIONHELPERIMPL_02 = "2-005-0002";
    public static final String CONVERSATIONHELPERIMPL_03 = "2-005-0003";
    public static final String CONVERSATIONHELPERIMPL_04 = "2-005-0004";
    public static final String CONVERSATIONHELPERIMPL_05 = "2-005-0005";
    public static final String CONVERSATIONHELPERIMPL_06 = "2-005-0006";
    public static final String CONVERSATIONHELPERIMPL_07 = "2-005-0007";
    public static final String CONVERSATIONHELPERIMPL_08 = "2-005-0008";
    public static final String CONVERSATIONHELPERIMPL_09 = "2-005-0009";
    public static final String CONVERSATIONHELPERIMPL_10 = "2-005-0010";
    public static final String CONVERSATIONHELPERIMPL_11 = "2-005-0011";
    public static final String CONVERSATIONHELPERIMPL_12 = "2-005-0012";
    public static final String CONVERSATIONHELPERIMPL_13 = "2-005-0013";
    public static final String CONVERSATIONHELPERIMPL_14 = "2-005-0014";
    public static final String CONVERSATIONHELPERIMPL_15 = "2-005-0015";//D234369
    public static final String CONVERSATIONHELPERIMPL_16 = "2-005-0016";//D234369
    public static final String CONVERSATIONHELPERIMPL_17 = "2-005-0017";//D234369
    public static final String CONVERSATIONHELPERIMPL_18 = "2-005-0018";//D234369
    public static final String CONVERSATIONHELPERIMPL_19 = "2-005-0019";//D234369
    public static final String CONVERSATIONHELPERIMPL_20 = "2-005-0020";//F013661

    public static final String ASYNCHPQIMPL_SETCONSUMERSESS_01 = "2-006-0001";
    public static final String ASYNCHPQIMPL_SETCONSUMERSESS_02 = "2-006-0002";
    public static final String ASYNCHPQIMPL_PROCESSEXCEPTIONS_01 = "2-006-0003";// D225856
    public static final String ASYNCHPQIMPL_SETASYNCCALLBACK_01 = "2-006-0004";// D225856

    public static final String ASYNCHPQ_PUT_01 = "2-007-0001";
    public static final String ASYNCHPQ_GET_01 = "2-007-0002";
    public static final String ASYNCHPQ_GETBATCH_01 = "2-007-0003";
    public static final String ASYNCHPQ_GETBATCH_02 = "2-007-0004";
    public static final String ASYNCHPQ_DELIVER_01 = "2-007-0005";//D249096
    public static final String ASYNCHPQ_DELIVER_02 = "2-007-0006";//D249096
    public static final String ASYNCHPQ_DELIVER_03 = "2-007-0007";//D249096
    public static final String ASYNCHPQ_DELIVER_04 = "2-007-0008";//D249096
    public static final String ASYNCHPQ_DELIVER_05 = "2-007-0009";//D249096

    public static final String BROWSERPQ_SETBROWSERSESS_01 = "2-008-0001";
    public static final String BROWSERPQ_SETBROWSERSESS_02 = "2-008-0002";

    public static final String ORDEREDPQ_GET_01 = "2-009-0001";
    public static final String ORDEREDPQ_GETBATCH_01 = "2-009-0002";
    public static final String ORDEREDPQ_GETBATCH_02 = "2-009-0003";
    public static final String ORDEREDPQ_GETDESCRIPTOR_01 = "2-009-0004";

    public static final String PQCONVGRPFACTIMPL_CREATE_01 = "2-010-0001";

    public static final String RHPQIMPL_SETCONSUMERSESS_01 = "2-011-0001";
    public static final String RHPQIMPL_SETCONSUMERSESS_02 = "2-011-0002";
    public static final String RHPQIMPL_PROCESSEXCEPTIONS_01 = "2-011-0003";// D225856
    public static final String RHPQIMPL_DELIVERMESSAGES_01 = "2-011-0004";

    public static final String RHPQ_WAITNONEMPTY_01 = "2-012-0001";
    public static final String RHPQ_GET_01 = "2-012-0002";// D220088
    public static final String RHPQ_WAITNONEMPTY_02 = "2-012-0003";// D218324
    public static final String RHPQ_DELIVER_01 = "2-012-0004";//D249096
    public static final String RHPQ_DELIVER_02 = "2-012-0005";//D249096
    public static final String RHPQ_DELIVER_03 = "2-012-0006";//D249096
    public static final String RHPQ_DELIVER_04 = "2-012-0007";//D249096
    public static final String RHPQ_DELIVER_05 = "2-012-0008";//D249096
    public static final String RHPQ_DELIVER_06 = "2-012-0009";//PM12356
    public static final String RHPQ_DELIVER_07 = "2-012-0010";//PM12356

    public static final String SHORTIDALLOCATOR_INIT_01 = "2-013-0001";

//   public static final String BASEQ_CLINIT_01                              = "2-014-0001";// D225856

    public static final String MULTICASTRHPROXYQUEUE_CLOSED_01 = "2-015-0001";

    public static final String RHSESSPQIMPL_RECEIVENOWAIT_01 = "2-016-0001";
    public static final String RHSESSPQIMPL_RECEIVEWITHWAIT_01 = "2-016-0002";

    public static final String QUEUEDATA_GETMESSAGE_01 = "2-017-0001";

    // ***** Server side ***** //
    public static final String GENERICTRANSPORTRECEIVELISTENER_ERROR_01 = "3-001-0001";

    public static final String COMMONSERVERRECEIVELISTENER_RHSHAKE_01 = "3-031-0001";
    public static final String COMMONSERVERRECEIVELISTENER_HSREJCT_01 = "3-031-0002";
    public static final String COMMONSERVERRECEIVELISTENER_RHSHAKE_02 = "3-031-0003";

    public static final String SERVERSIDECONNECTION_CLOSE_01 = "3-002-0001";
    public static final String SERVERSIDECONNECTION_CONNECT_01 = "3-002-0002";
    public static final String SERVERSIDECONNECTION_SETSICORECONN_01 = "3-002-0003";
    public static final String SERVERSIDECONNECTION_TRMEXCHANGE_01 = "3-002-0004";
    public static final String SERVERSIDECONNECTION_MFPEXCHANGE_01 = "3-002-0005";
    public static final String SERVERSIDECONNECTION_SENDMFPSCHEMA_01 = "3-002-0006";// D234369
    public static final String SERVERSIDECONNECTION_REQUESTMFPSCHEMATA_01 = "3-002-0007";// F247845
    public static final String SERVERSIDECONNECTION_EMITNOTIFICATION_01 = "3-002-0008";// F206161.5

    public static final String SERVERTRANSPORTFACTORY_INIT_01 = "3-003-0001";
    public static final String SERVERTRANSPORTFACTORY_INIT_02 = "3-003-0002";

    public static final String SERVERTRANSPORTRECEIVELISTENER_INIT_01 = "3-004-0001";

    public static final String SERVERTRANSPORTRECEIVELISTENER_TRMEXCG_01 = "3-004-0003";
    public static final String SERVERTRANSPORTRECEIVELISTENER_TRMEXCG_02 = "3-004-0004";
    public static final String SERVERTRANSPORTRECEIVELISTENER_CONNGET_01 = "3-004-0005";
    public static final String SERVERTRANSPORTRECEIVELISTENER_CONNGET_02 = "3-004-0006";
    public static final String SERVERTRANSPORTRECEIVELISTENER_CONNGET_03 = "3-004-0007";
    public static final String SERVERTRANSPORTRECEIVELISTENER_CONNGET_04 = "3-004-0008";
    public static final String SERVERTRANSPORTRECEIVELISTENER_CONNCLS_01 = "3-004-0009";
    public static final String SERVERTRANSPORTRECEIVELISTENER_ERROR_01 = "3-004-0010";
    public static final String SERVERTRANSPORTRECEIVELISTENER_ERROR_02 = "3-004-0011";
    public static final String SERVERTRANSPORTRECEIVELISTENER_ERROR_03 = "3-004-0012";
    public static final String SERVERTRANSPORTRECEIVELISTENER_DATARCV_01 = "3-004-0013";
    public static final String SERVERTRANSPORTRECEIVELISTENER_CLOSECONN_01 = "3-004-0016";
    public static final String SERVERTRANSPORTRECEIVELISTENER_MFPEXCG_01 = "3-004-0017";
    public static final String SERVERTRANSPORTRECEIVELISTENER_MFPEXCG_02 = "3-004-0018";
    public static final String SERVERTRANSPORTRECEIVELISTENER_MFPSCHEMA_01 = "3-004-0019";
    public static final String SERVERTRANSPORTRECEIVELISTENER_DIRECTCN_01 = "3-004-0020";
    public static final String SERVERTRANSPORTRECEIVELISTENER_DIRECTCN_02 = "3-004-0021";
    public static final String SERVERTRANSPORTRECEIVELISTENER_DIRECTCN_03 = "3-004-0022";
    public static final String SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_01 = "3-004-0023";
    public static final String SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_02 = "3-004-0023";// D297060
    public static final String SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_03 = "3-004-0023";// D297060
    public static final String SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_04 = "3-004-0023";// D297060
    public static final String SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_05 = "3-004-0023";// D297060
    public static final String SERVERTRANSPORTRECEIVELISTENER_MFPREQSCH_01 = "3-004-0024";// F247845
    public static final String SERVERTRANSPORTRECEIVELISTENER_MFPREQSCH_02 = "3-004-0025";// F247845
    public static final String SERVERTRANSPORTRECEIVELISTENER_MFPREQSCH_03 = "3-004-0026";// F247845

    public static final String SERVERSICORECONNECTIONLISTENER_MEN_01 = "3-005-0001";
    public static final String SERVERSICORECONNECTIONLISTENER_MET_01 = "3-005-0002";
    public static final String SERVERSICORECONNECTIONLISTENER_ASYNC_01 = "3-005-0003";
    public static final String SERVERSICORECONNECTIONLISTENER_COMMS_01 = "3-005-0004";
    public static final String SERVERSICORECONNECTIONLISTENER_MEQ_01 = "3-005-0005";
    public static final String SERVERSICORECONNECTIONLISTENER_ASYNC_02 = "3-005-0006";

    public static final String STATICCATCONNECTION_CONNCLONE_01 = "3-006-0001";
    public static final String STATICCATCONNECTION_CONNCLONE_02 = "3-006-0002";
    public static final String STATICCATCONNECTION_CONNCLONE_03 = "3-006-0003";
    public static final String STATICCATCONNECTION_CONNCLOSE_01 = "3-006-0004";
    public static final String STATICCATCONNECTION_CONNCLOSE_02 = "3-006-0005";
    public static final String STATICCATCONNECTION_UNIQID_01 = "3-006-0006";
    public static final String STATICCATCONNECTION_UNIQID_02 = "3-006-0007";
    public static final String STATICCATCONNECTION_CONNCLS_02 = "3-006-0008";
    public static final String STATICCATCONNECTION_RCVCONNMSG_01 = "3-006-0009";
    public static final String STATICCATCONNECTION_CREATE_OC_01 = "3-006-0010";
    public static final String STATICCATCONNECTION_CREATE_OC_02 = "3-006-0011";
    public static final String STATICCATCONNECTION_CREATE_OC_03 = "3-006-0012";
    public static final String STATICCATCONNECTION_CLOSE_OC_01 = "3-006-0012";
    public static final String STATICCATCONNECTION_CHK_MESSAGING_REQ_01 = "3-006-0013"; // LIDB3684.11.1.3
    public static final String STATICCATCONNECTION_CHK_MESSAGING_REQ_02 = "3-006-0014"; // LIDB3684.11.1.3
    public static final String STATICCATCONNECTION_INVOKECMD_01 = "3-006-0015"; // SIB0009.comms.1
    public static final String STATICCATCONNECTION_INVOKECMD_02 = "3-006-0016"; // SIB0009.comms.1
    public static final String STATICCATCONNECTION_ADD_DEST_LISTENER_01 = "3-006-0017"; // SIB0137.comms.2
    public static final String STATICCATCONNECTION_ADD_DEST_LISTENER_02 = "3-006-0018"; // SIB0137.comms.3
    public static final String STATICCATCONNECTION_CONNCLOSE_03 = "3-006-0019";
    public static final String STATICCATCONNECTION_REG_SET_CONSUMER_MON_01 = "3-006-0020"; //F011127
    public static final String STATICCATCONNECTION_REG_SET_CONSUMER_MON_02 = "3-006-0021"; //F011127
    public static final String STATICCATDESTINATION_DESTCREATE_01 = "3-007-0001";
    public static final String STATICCATDESTINATION_DESTCREATE_02 = "3-007-0002";
    public static final String STATICCATDESTINATION_DESTDELETE_01 = "3-007-0003";
    public static final String STATICCATDESTINATION_DESTDELETE_02 = "3-007-0004";
    public static final String STATICCATDESTINATION_DESTGET_01 = "3-007-0005";
    public static final String STATICCATDESTINATION_DESTGET_02 = "3-007-0006";
    public static final String STATICCATDESTINATION_DESTGET_03 = "3-007-0007";
    public static final String STATICCATDESTINATION_DESTGET_04 = "3-007-0008";
    public static final String STATICCATDESTINATION_DESTGET_05 = "3-007-0009";
    public static final String STATICCATDESTINATION_DESTGET_06 = "3-007-0010";

    public static final String STATICCATPRODUCER_CREATE_01 = "3-008-0001";
    public static final String STATICCATPRODUCER_CREATE_02 = "3-008-0002";
    public static final String STATICCATPRODUCER_CREATE_03 = "3-008-0003";
    public static final String STATICCATPRODUCER_CLOSE_01 = "3-008-0003";
    public static final String STATICCATPRODUCER_CLOSE_02 = "3-008-0004";
    public static final String STATICCATPRODUCER_SEND_01 = "3-008-0005";
    public static final String STATICCATPRODUCER_SEND_02 = "3-008-0006";
    public static final String STATICCATPRODUCER_SEND_03 = "3-008-0007";
    public static final String STATICCATPRODUCER_SEND_04 = "3-008-0008";
    public static final String STATICCATPRODUCER_CONNSEND_01 = "3-008-0009";
    public static final String STATICCATPRODUCER_CONNSEND_02 = "3-008-0010";
    public static final String STATICCATPRODUCER_CONNSEND_03 = "3-008-0011";
    public static final String STATICCATPRODUCER_CONNSEND_04 = "3-008-0012";
    public static final String STATICCATPRODUCER_SENDCHUNKED_01 = "3-008-0013";
    public static final String STATICCATPRODUCER_SENDCHUNKED_02 = "3-008-0014";
    public static final String STATICCATPRODUCER_SENDCHUNKED_03 = "3-008-0015";
    public static final String STATICCATPRODUCER_SENDCHUNKED_04 = "3-008-0016";
    public static final String STATICCATPRODUCER_SENDCHUNKED_05 = "3-008-0017";
    public static final String STATICCATPRODUCER_SENDCHUNKEDCONN_01 = "3-008-0018";
    public static final String STATICCATPRODUCER_SENDCHUNKEDCONN_02 = "3-008-0019";
    public static final String STATICCATPRODUCER_SENDCHUNKEDCONN_03 = "3-008-0020";
    public static final String STATICCATPRODUCER_SENDCHUNKEDCONN_04 = "3-008-0021";
    public static final String STATICCATPRODUCER_SENDCHUNKEDCONN_05 = "3-008-0022";

    public static final String STATICCATTRANSACTION_CREATE_01 = "3-009-0001";
    public static final String STATICCATTRANSACTION_BEGIN_01 = "3-009-0002";
    public static final String STATICCATTRANSACTION_BEGIN_02 = "3-009-0003";
    public static final String STATICCATTRANSACTION_PREPARE_01 = "3-009-0004";
    public static final String STATICCATTRANSACTION_PREPARE_02 = "3-009-0005";
    public static final String STATICCATTRANSACTION_COMMIT_01 = "3-009-0006";
    public static final String STATICCATTRANSACTION_COMMIT_02 = "3-009-0007";
    public static final String STATICCATTRANSACTION_ROLLBACK_01 = "3-009-0008";
    public static final String STATICCATTRANSACTION_ROLLBACK_02 = "3-009-0009";

    public static final String STATICCATCONSUMER_CREATE_01 = "3-010-0001";
    public static final String STATICCATCONSUMER_CREATE_02 = "3-010-0002";
    public static final String STATICCATCONSUMER_CREATE_03 = "3-010-0003";
    public static final String STATICCATCONSUMER_CREATE_04 = "3-010-0004";
    public static final String STATICCATCONSUMER_CREATEBIF_01 = "3-010-0005";
    public static final String STATICCATCONSUMER_CREATEBIF_02 = "3-010-0006";

    public static final String STATICCATSUBSCRIPTION_CREATE_01 = "3-011-0001";
    public static final String STATICCATSUBSCRIPTION_CREATE_02 = "3-011-0002";
    public static final String STATICCATSUBSCRIPTION_CREATE_03 = "3-011-0003";
    public static final String STATICCATSUBSCRIPTION_DELETE_01 = "3-011-0004";
    public static final String STATICCATSUBSCRIPTION_DELETE_02 = "3-011-0005";
    public static final String STATICCATSUBSCRIPTION_CREATECONS_01 = "3-011-0006";
    public static final String STATICCATSUBSCRIPTION_CREATECONS_02 = "3-011-0007";
    public static final String STATICCATSUBSCRIPTION_CREATECONS_03 = "3-011-0008";
    public static final String STATICCATSUBSCRIPTION_CREATECONS_04 = "3-011-0009";
    public static final String STATICCATSUBSCRIPTION_CREATECONS_05 = "3-011-0010";

    public static final String STATICCATHELPER_SEND_EXCEP_01 = "3-012-0001";
    public static final String STATICCATHELPER_SEND_ASEXCEP_01 = "3-012-0002";
    public static final String STATICCATHELPER_SENDSESSRESPONSE_01 = "3-012-0003";
    public static final String STATICCATHELPER_SENDSESSRESPONSE_02 = "3-012-0004";
    public static final String STATICCATHELPER_SENDMCSESSRESPONSE_01 = "3-012-0005";

    public static final String STATICCATXATRANSACTION_XAOPEN_01 = "3-013-0001";
    public static final String STATICCATXATRANSACTION_XAOPEN_02 = "3-013-0002";
    public static final String STATICCATXATRANSACTION_XASTART_01 = "3-013-0003";
    public static final String STATICCATXATRANSACTION_XASTART_02 = "3-013-0004";
    public static final String STATICCATXATRANSACTION_XASTART_03 = "3-013-0005";
    public static final String STATICCATXATRANSACTION_XAEND_01 = "3-013-0006";
    public static final String STATICCATXATRANSACTION_XAEND_02 = "3-013-0007";
//   public static final String STATICCATXATRANSACTION_XAEND_03              = "3-013-0008";
    public static final String STATICCATXATRANSACTION_XAPREPARE_01 = "3-013-0009";
    public static final String STATICCATXATRANSACTION_XAPREPARE_02 = "3-013-0010";
//   public static final String STATICCATXATRANSACTION_XAPREPARE_03          = "3-013-0011";
    public static final String STATICCATXATRANSACTION_XACOMMIT_01 = "3-013-0012";
    public static final String STATICCATXATRANSACTION_XACOMMIT_02 = "3-013-0013";
//   public static final String STATICCATXATRANSACTION_XACOMMIT_03           = "3-013-0014";
    public static final String STATICCATXATRANSACTION_XAROLLBACK_01 = "3-013-0015";
    public static final String STATICCATXATRANSACTION_XAROLLBACK_02 = "3-013-0016";
//   public static final String STATICCATXATRANSACTION_XAROLLBACK_03         = "3-013-0017";
    public static final String STATICCATXATRANSACTION_XARECOVER_01 = "3-013-0018";
    public static final String STATICCATXATRANSACTION_XARECOVER_02 = "3-013-0019";
//   public static final String STATICCATXATRANSACTION_XARECOVER_03          = "3-013-0020";
    public static final String STATICCATXATRANSACTION_XAFORGET_01 = "3-013-0021";
    public static final String STATICCATXATRANSACTION_XAFORGET_02 = "3-013-0022";
//   public static final String STATICCATXATRANSACTION_XAFORGET_03           = "3-013-0023";
    public static final String STATICCATXATRANSACTION_GETTXTIMEOUT_01 = "3-013-0024";
    public static final String STATICCATXATRANSACTION_GETTXTIMEOUT_02 = "3-013-0025";
//   public static final String STATICCATXATRANSACTION_GETTXTIMEOUT_03       = "3-013-0026";
    public static final String STATICCATXATRANSACTION_SETTXTIMEOUT_01 = "3-013-0027";
    public static final String STATICCATXATRANSACTION_SETTXTIMEOUT_02 = "3-013-0028";
//   public static final String STATICCATXATRANSACTION_SETTXTIMEOUT_03       = "3-013-0029";
    public static final String STATICCATXATRANSACTION_XASTART_04 = "3-013-0030";// D297060
    public static final String STATICCATXATRANSACTION_XASTART_05 = "3-013-0031";// D297060
    public static final String STATICCATXATRANSACTION_GETRESFROMTX_01 = "3-013-0032";// D297060
    public static final String STATICCATXATRANSACTION_GETRESFROMTX_02 = "3-013-0033";// D297060
    public static final String STATICCATXATRANSACTION_GETRESFROMTX_03 = "3-013-0034";

    public static final String STATICCATDESTINATION_GETDESTCONFIG_01 = "3-014-0001";
    public static final String STATICCATDESTINATION_GETDESTCONFIG_02 = "3-014-0002";
    public static final String STATICCATDESTINATION_GETDESTCONFIG_03 = "3-014-0003";
    public static final String STATICCATDESTINATION_SENDTOEXCEP_01 = "3-014-0004";
    public static final String STATICCATDESTINATION_SENDTOEXCEP_02 = "3-014-0005";
    public static final String STATICCATDESTINATION_SENDTOEXCEP_03 = "3-014-0006";
    public static final String STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_01 = "3-014-0007";
    public static final String STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_02 = "3-014-0008";
    public static final String STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_03 = "3-014-0009";
    public static final String STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_04 = "3-014-0010";

    public static final String STATICCATBROWSER_RCVCREATEBROWSERSESS_01 = "3-015-0001";
    public static final String STATICCATBROWSER_RCVCREATEBROWSERSESS_02 = "3-015-0002";
    public static final String STATICCATBROWSER_RCVCREATEBROWSERSESS_03 = "3-015-0003";
    public static final String STATICCATBROWSER_RCVRESETBROWSERSESS_01 = "3-015-0004";
    public static final String STATICCATBROWSER_RCVRESETBROWSERSESS_02 = "3-015-0005";
    public static final String STATICCATBROWSER_RCVRESETBROWSERSESS_03 = "3-015-0006";
    public static final String STATICCATBROWSER_RCVRESETBROWSERSESS_04 = "3-015-0007";
    public static final String STATICCATBROWSER_RCVCREATEBROWSERSESS_04 = "3-015-0008"; //SIB0113.comms.1

    public static final String CATSYNCASYNCHREADER_CONSUME_MSGS_01 = "3-016-0001";
    public static final String CATSYNCASYNCHREADER_CONSUME_MSGS_03 = "3-016-0002";
    public static final String CATSYNCASYNCHREADER_CONSUME_MSGS_04 = "3-016-0003";
    public static final String CATSYNCASYNCHREADER_SEND_MSG_01 = "3-016-0004";
    public static final String CATSYNCASYNCHREADER_SEND_MSG_02 = "3-016-0005";
    public static final String CATSYNCASYNCHREADER_SEND_EXCEP_01 = "3-016-0006";
    public static final String CATSYNCASYNCHREADER_SEND_NO_MSG_01 = "3-016-0007";
    public static final String CATSYNCASYNCHREADER_CLOSE_SESS_01 = "3-016-0008";
    public static final String CATSYNCASYNCHREADER_ASYNCHEXCEPTION_01 = "3-016-0009";
    public static final String CATSYNCASYNCHREADER_COMMSFAILURE_01 = "3-016-0010";
    public static final String CATSYNCASYNCHREADER_METERMINATED_01 = "3-016-0011";
    public static final String CATSYNCASYNCHREADER_SEND_MSG_03 = "3-016-0012";
    public static final String CATSYNCASYNCHREADER_CONSUME_MSGS_05 = "3-016-0013";

    public static final String CATTIMER_ALARM_01 = "3-017-0001";

    public static final String CATASYNCHCONSUMER_SETCALLBACK_01 = "3-018-0001";
    public static final String CATASYNCHCONSUMER_SETCALLBACK_02 = "3-018-0002";
    public static final String CATASYNCHCONSUMER_UNLOCKSET_01 = "3-018-0003";
    public static final String CATASYNCHCONSUMER_UNLOCKSET_02 = "3-018-0004";
    public static final String CATASYNCHCONSUMER_DELETESET_01 = "3-018-0005";
    public static final String CATASYNCHCONSUMER_UNLOCKALL_01 = "3-018-0006";
    public static final String CATASYNCHCONSUMER_UNLOCKALL_02 = "3-018-0007";
//   public static final String CATASYNCHCONSUMER_SENDMESS_01                = "3-018-0008";
    public static final String CATASYNCHCONSUMER_CONSUME_MSGS_01 = "3-018-0009";
    public static final String CATASYNCHCONSUMER_CONSUME_MSGS_02 = "3-018-0010";
    public static final String CATASYNCHCONSUMER_SENDCHUNKEDMESS_01 = "3-018-0011";
    public static final String CATASYNCHCONSUMER_SENDENTIREMESS_01 = "3-018-0012";
    public static final String CATASYNCHCONSUMER_SENSSION_STOPPED_01 = "3-018-0013"; // SIB0115d.comms
    public static final String CATASYNCHCONSUMER_UNLOCKSET_03 = "3-018-0014";
    public static final String CATASYNCHCONSUMER_UNLOCKSET_04 = "3-018-0015";
    public static final String CATASYNCHCONSUMER_UNLOCKALL_03 = "3-018-0016";//F013661
    public static final String CATASYNCHCONSUMER_UNLOCKALL_04 = "3-018-0017";//F013661

    public static final String CATCONSUMER_RECEIVE_01 = "3-019-0001";
    public static final String CATCONSUMER_CLOSE_01 = "3-019-0002";
    public static final String CATCONSUMER_CLOSE_02 = "3-019-0003";
    public static final String CATCONSUMER_SETASYNCHCALLBACK_01 = "3-019-0004";
    public static final String CATCONSUMER_START_01 = "3-019-0005";
    public static final String CATCONSUMER_STOP_01 = "3-019-0006";
    public static final String CATCONSUMER_STOP_02 = "3-019-0007";
    public static final String CATCONSUMER_UNLOCKSET_01 = "3-019-0008";
    public static final String CATCONSUMER_DELETESET_01 = "3-019-0009";
    public static final String CATCONSUMER_UNLOCKALL_01 = "3-019-0010";
    public static final String CATCONSUMER_REQUESTMSGS_01 = "3-019-0011";
    public static final String CATCONSUMER_UNSETASYNCHCALLBACK_01 = "3-019-0012";
    public static final String CATCONSUMER_UNSETASYNCHCALLBACK_02 = "3-019-0013";
    public static final String CATCONSUMER_FLUSH_01 = "3-019-0014";
    public static final String CATCONSUMER_RESET_01 = "3-019-0015";
    public static final String CATCONSUMER_READSET_01 = "3-019-0016";
    public static final String CATCONSUMER_READANDDELTESET_01 = "3-019-0017";
    public static final String CATCONSUMER_START_02 = "3-019-0018";
    public static final String CATCONSUMER_UNLOCKSET_02 = "3-019-0019";
    public static final String CATCONSUMER_UNLOCKALL_02 = "3-019-0020";

    public static final String CATSESSSYNCHCONSUMER_RECEIVE_01 = "3-020-0001";
    public static final String CATSESSSYNCHCONSUMER_RECEIVE_02 = "3-020-0002";
    public static final String CATSESSSYNCHCONSUMER_RECEIVE_03 = "3-020-0003";
    public static final String CATSESSSYNCHCONSUMER_CLOSE_01 = "3-020-0004";
    public static final String CATSESSSYNCHCONSUMER_START_01 = "3-020-0005";
    public static final String CATSESSSYNCHCONSUMER_RECEIVE_04 = "3-020-0006";

    public static final String CATPROXYCONSUMER_DELETESET_01 = "3-021-0001";
    public static final String CATPROXYCONSUMER_DELETESET_02 = "3-021-0002";
    public static final String CATPROXYCONSUMER_UNLOCKSET_01 = "3-021-0003";
    public static final String CATPROXYCONSUMER_UNLOCKSET_02 = "3-021-0004";
    public static final String CATPROXYCONSUMER_UNLOCKALL_01 = "3-021-0005";
    public static final String CATPROXYCONSUMER_UNLOCKALL_02 = "3-021-0006";
    public static final String CATPROXYCONSUMER_FLUSH_01 = "3-021-0007";
    public static final String CATPROXYCONSUMER_FLUSH_02 = "3-021-0008";
    public static final String CATPROXYCONSUMER_SEND_MSG_01 = "3-021-0009";
    public static final String CATPROXYCONSUMER_REQUEST_MSGS_01 = "3-021-0010";
    public static final String CATPROXYCONSUMER_SEND_CHUNKED_MSG_01 = "3-021-0011";
    public static final String CATPROXYCONSUMER_UNLOCKSET_04 = "3-021-0012"; //This way matches up with 6.1.
    public static final String CATPROXYCONSUMER_UNLOCKSET_03 = "3-021-0013";
    public static final String CATPROXYCONSUMER_UNLOCKALL_03 = "3-021-0014";
    public static final String CATPROXYCONSUMER_UNLOCKALL_04 = "3-021-0015";

    public static final String CATASYNCHRHREADER_CONSUME_MSGS_01 = "3-022-0001";
    public static final String CATASYNCHRHREADER_CONSUME_MSGS_02 = "3-022-0002";

    public static final String CATBROWSECONSUMER_SENDMESSAGE_01 = "3-023-0001";
    public static final String CATBROWSECONSUMER_SENDMESSAGE_02 = "3-023-0002";
    public static final String CATBROWSECONSUMER_GETNEXTMESSAGE_01 = "3-023-0003";
    public static final String CATBROWSECONSUMER_FLUSH_01 = "3-023-0004";
    public static final String CATBROWSECONSUMER_CLOSE_01 = "3-023-0005";
    public static final String CATBROWSECONSUMER_CLOSE_02 = "3-023-0006";
    public static final String CATBROWSECONSUMER_SEND_CHUNKED_MSG_01 = "3-023-0007";
    public static final String CATBROWSECONSUMER_SEND_CHUNKED_MSG_02 = "3-023-0008";

    public static final String CATASYNCHCONSUMER_FLUSH_01 = "3-024-0001";
    public static final String CATASYNCHCONSUMER_FLUSH_02 = "3-024-0002";
    public static final String CATASYNCHCONSUMER_DELETESET_02 = "3-024-0003";

    public static final String CATBIFCONSUMER_DELETESET_01 = "3-025-0001";
    public static final String CATBIFCONSUMER_DELETESET_02 = "3-025-0002";
    public static final String CATBIFCONSUMER_DELETESET_03 = "3-025-0003";
    public static final String CATBIFCONSUMER_DELETESET_04 = "3-025-0004";
    public static final String CATBIFCONSUMER_UNLOCKSET_01 = "3-025-0005";
    public static final String CATBIFCONSUMER_UNLOCKSET_02 = "3-025-0006";
    public static final String CATBIFCONSUMER_READSET_01 = "3-025-0007";
    public static final String CATBIFCONSUMER_READSET_02 = "3-025-0008";
    public static final String CATBIFCONSUMER_READANDDELTESET_01 = "3-025-0009";
    public static final String CATBIFCONSUMER_READANDDELTESET_02 = "3-025-0010";
    public static final String CATBIFCONSUMER_ADDSIBMESS_01 = "3-025-0011";
    public static final String CATBIFCONSUMER_CLOSE_01 = "3-025-0012";
    public static final String CATBIFCONSUMER_CLOSE_02 = "3-025-0013";
    public static final String CATBIFCONSUMER_UNLOCKSET_03 = "3-025-0014";
    public static final String CATBIFCONSUMER_UNLOCKSET_04 = "3-025-0015";

    public static final String MULTICASTCONSUMERSESSION_CONSUMEMSGS_01 = "3-026-0001";
    public static final String MULTICASTCONSUMERSESSION_CONSUMEMSGS_02 = "3-026-0002";

    public static final String CATMAINCONSUMER_CHECKNOTBROWSER_01 = "3-027-0001";
    public static final String CATMAINCONSUMER_CHECKNOTBROWSER_02 = "3-027-0002";
    public static final String CATMAINCONSUMER_START_01 = "3-027-0004";
    public static final String CATMAINCONSUMER_STOP_01 = "3-027-0005";

    public static final String CONVERSATIONSTATE_INIT_01 = "3-028-0001";
    public static final String CONVERSATIONSTATE_INIT_02 = "3-028-0002";
    public static final String CONVERSATIONSTATE_INIT_03 = "3-028-0003";

    public static final String CATTRANSACTION_ROLLBACKINDOUBTTXS_01 = "3-029-0001";// D257768

    public static final String IDTOTXTABLE_SELFDESTRUCT_01 = "3-030-0001";
    public static final String IDTOTXTABLE_GETTXGLOBALTXBRANCH_01 = "3-030-0002";
    public static final String IDTOTXTABLE_GETEXCEPFORROLLBACKLOCALTX_01 = "3-030-0003";
    public static final String IDTOTXTABLE_ENDOPTGLOBALTXBRANCH_01 = "3-030-0004";
    public static final String IDTOTXTABLE_GETEXPFORROLLBACKONLYGLOBALTX_02 = "3-030-0005";
    public static final String IDTOTXTABLE_GETEXPFORROLLBACKONLYGLOBALTX_01 = "3-030-0006";
    public static final String IDTOTXTABLE_REMOVELOCALTX_01 = "3-030-0007";
    public static final String IDTOTXTABLE_ENDGLOBALTXBRANCH_01 = "3-030-0008";
    public static final String IDTOTXTABLE_ISLOCALTXROLLBACKONLY_01 = "3-030-0009";
    public static final String IDTOTXTABLE_REMOVEGLOBALTXBRANCH_01 = "3-030-0010";
    public static final String IDTOTXTABLE_LOCALTXENTRY_GETEXCEPTION_01 = "3-030-0011";
    public static final String IDTOTXTABLE_RMENTRY_ENDCURRENT_01 = "3-030-0012";
    public static final String IDTOTXTABLE_RMENTRY_ENDCURRENT_02 = "3-030-0013";
    public static final String IDTOTXTABLE_RMENTRY_GETEXCEPTION_01 = "3-030-0014";
    public static final String IDTOTXTABLE_RMENTRY_GETEXCEPTION_02 = "3-030-0015";
    public static final String IDTOTXTABLE_RMENTRY_GETEXCEPTION_03 = "3-030-0016";
    public static final String IDTOTXTABLE_RMENTRY_ISROLLBACKONLY_01 = "3-030-0017";
    public static final String IDTOTXTABLE_RMENTRY_COMPLETEXID_01 = "3-030-0018";
    public static final String IDTOTXTABLE_RMENTRY_COMPLETEXID_02 = "3-030-0019";
    public static final String IDTOTXTABLE_RMENTRY_ENDTXBRANCH_01 = "3-030-0020";
    public static final String IDTOTXTABLE_RMENTRY_SETINDOUBT_01 = "3-030-0021";
    public static final String IDTOTXTABLE_RMENTRY_SETINDOUBT_02 = "3-030-0022";
    public static final String IDTOTXTABLE_RMENTRY_MARKROLLBACKONLY_01 = "3-030-0023";
    public static final String IDTOTXTABLE_RMENTRY_ENDCURRENT_03 = "3-030-0024";

    public static final String SERVERTRANSPORTACCEPTLISTENER_REMOVEALL_01 = "3-031-0001";

    // ***** ME->ME related ***** //
    public static final String MECONNECTION_CONNECT_01 = "4-001-0001";
    public static final String MECONNECTION_CONNECT_02 = "4-001-0002";
    public static final String MECONNECTION_CONNECT_03 = "4-001-0003";
    public static final String MECONNECTION_SEND_01 = "4-001-0004";
    public static final String MECONNECTION_TRMHANDSHAKEEXCHANGE_01 = "4-001-0005";
    public static final String MECONNECTION_TRMHANDSHAKEEXCHANGE_02 = "4-001-0006";
    public static final String MECONNECTION_MFPHANDSHAKEEXCHANGE_01 = "4-001-0007";
    public static final String MECONNECTION_MFPHANDSHAKEEXCHANGE_02 = "4-001-0008";
    public static final String MECONNECTION_SEND_02 = "4-001-0009"; // D215177.2
    public static final String MECONNECTION_REQUESTMFPSCHEMATA_01 = "4-001-0010";// F247845
    public static final String MECONNECTION_CLOSEREQCOMPLETE_01 = "4-001-0011";
    public static final String MECONNECTION_RCVDCLOSEREQ_01 = "4-001-0012";
    public static final String MECONNECTION_RCVDCLOSERESP_01 = "4-001-0013";
    public static final String MECONNECTION_SENDCLOSERESPONSE_01 = "4-001-0014";

    public static final String METRANSPORTRECEIVELISTENER_INIT_01 = "4-002-0001";
    public static final String METRANSPORTRECEIVELISTENER_TRMEXCG_01 = "4-002-0002";
    public static final String METRANSPORTRECEIVELISTENER_TRMEXCG_02 = "4-002-0003";
    public static final String METRANSPORTRECEIVELISTENER_DATARCV_01 = "4-002-0004";
    public static final String METRANSPORTRECEIVELISTENER_DATARCV_02 = "4-002-0005";
    public static final String METRANSPORTRECEIVELISTENER_ERROR_01 = "4-002-0006";
//   public static final String METRANSPORTRECEIVELISTENER_RHSHAKE_01        = "4-002-0007";
    public static final String METRANSPORTRECEIVELISTENER_RMESSAGE_01 = "4-002-0008";
    public static final String METRANSPORTRECEIVELISTENER_RMESSAGE_02 = "4-002-0009";
//   public static final String METRANSPORTRECEIVELISTENER_HSREJCT_01        = "4-002-0010";
//   public static final String METRANSPORTRECEIVELISTENER_HSREJCT_02        = "4-002-0011";
//   public static final String METRANSPORTRECEIVELISTENER_RCLOSE_01         = "4-002-0012";
//   public static final String METRANSPORTRECEIVELISTENER_RCLOSE_02         = "4-002-0013";
    public static final String METRANSPORTRECEIVELISTENER_RCVTRMEXCHANGE_01 = "4-002-0014"; // D280276
    public static final String METRANSPORTRECEIVELISTENER_OUTBOUNDSETUP_01 = "4-002-0015"; // D280276
    public static final String METRANSPORTRECEIVELISTENER_OUTBOUNDSETUP_02 = "4-002-0016"; // D280276
    public static final String METRANSPORTRECEIVELISTENER_DATARCV_03 = "4-002-0017"; // D295765
    public static final String METRANSPORTRECEIVELISTENER_RCHMESSAGE_01 = "4-002-0018";
    public static final String METRANSPORTRECEIVELISTENER_RCHMESSAGE_02 = "4-002-0019";

    public static final String MECONNECTIONFACT_CLINIT_01 = "4-003-0001";

    // ***** Common ***** //
    public static final String JFAPCOMMUNICATOR_EXCHANGE_01 = "5-001-0001";
    public static final String JFAPCOMMUNICATOR_SEND_01 = "5-001-0002";
    public static final String JFAPCOMMUNICATOR_INITIATEHANDSHAKING_01 = "5-001-0003";
    public static final String JFAPCOMMUNICATOR_INITIATEHANDSHAKING_02 = "5-001-0004";
    public static final String JFAPCOMMUNICATOR_INITIATEHANDSHAKING_03 = "5-001-0005";
//   public static final String JFAPCOMMUNICATOR_GETCMDCOMPLETIONCODE_01     = "5-001-0006";
    public static final String JFAPCOMMUNICATOR_VALIDATECSTATE_01 = "5-001-0007";
    public static final String JFAPCOMMUNICATOR_VALIDATECSTATE_02 = "5-001-0008";
    public static final String JFAPCOMMUNICATOR_VALIDATECSTATE_03 = "5-001-0009";

//   public static final String COMMSSTRING_SETSTRING_01                     = "5-002-0001";
//   public static final String COMMSSTRING_SETBYTES_01                      = "5-002-0002";
//   public static final String COMMSSTRING_VALIDATE_01                      = "5-002-0003";

    public static final String COMMSUTILS_GETRUNTIMEINT_01 = "5-003-0001";// D235891
    public static final String COMMSUTILS_GETRUNTIMEDOUBLE_01 = "5-003-0002";// D235891

    public static final String CONVERSATIONSTATE_EXTENDOBJECTTABLE_01 = "5-004-0001";// D256703

    public static final String TRANTODISPATCHMAP_ADDDISPATCHLOCALTX_01 = "5-005-0001";// D297060
    public static final String TRANTODISPATCHMAP_ADDENLISTEDGLOBALTX_01 = "5-005-0002";// D297060
    public static final String TRANTODISPATCHMAP_MARKNOTENLISTED_01 = "5-005-0003";// D297060
    public static final String TRANTODISPATCHMAP_REMOVEFORLOCALTX_01 = "5-005-0004";// D297060
    public static final String TRANTODISPATCHMAP_REMOVEFORGLOBALTX_01 = "5-005-0005";// D297060
    public static final String TRANTODISPATCHMAP_GETENLISTED_01 = "5-005-0006";// D297060
    public static final String TRANTODISPATCHMAP_CREATENEWENLISTED_01 = "5-005-0007";// D297060
    public static final String TRANTODISPATCHMAP_GMARKNOTENLISTED_01 = "5-005-0008";// D297060
    public static final String TRANTODISPATCHMAP_GMARKNOTENLISTED_02 = "5-005-0009";// D297060
    public static final String TRANTODISPATCHMAP_REMOVEDISPATCHABLE_01 = "5-005-0010";// D297060

    public static final String COMMSBYTEBUFFER_PUTMESSAGE_01 = "5-006-0001";
    public static final String COMMSBYTEBUFFER_GETMESSAGE_01 = "5-006-0002";
    public static final String COMMSBYTEBUFFER_PUTSTRING_01 = "5-006-0003";
    public static final String COMMSBYTEBUFFER_GETSTRING_01 = "5-006-0004";
    public static final String COMMSBYTEBUFFER_PUTCLIENTMSG_01 = "5-006-0005";
    public static final String COMMSBYTEBUFFER_PUTCLIENTMSG_02 = "5-006-0006";
    public static final String COMMSBYTEBUFFER_PUTCLIENTMSG_03 = "5-006-0007";
    public static final String COMMSBYTEBUFFER_PUTCLIENTMSG_04 = "5-006-0008";
    public static final String COMMSBYTEBUFFER_SETREASONABLE_01 = "5-006-0009";
    public static final String COMMSBYTEBUFFER_SETREASONABLE_02 = "5-006-0010";
    public static final String COMMSBYTEBUFFER_CALC_ENC_STRLEN_01 = "5-006-0011";

    public static final String COMMSDIAGMODULE_INITIALIZE_01 = "5-005-0001";

    public static final String TRANSACTION_INFORMCONSUMERSOFROLLBACK_01 = "5-007-0001";// PK86574

    public static final String SERVERDESTINATIONLISTENER_DESTAVAILABLE_01 = "3-032-0001"; //SIB0137.comms.3
    public static final String SERVERDESTINATIONLISTENER_DESTAVAILABLE_02 = "3-032-0002"; //SIB0137.comms.3
    public static final String SERVERDESTINATIONLISTENER_DESTAVAILABLE_03 = "3-032-0003"; //SIB0137.comms.3

    public static final String SERVERCONSUMERMONITORLISTENER_CONSUMERSETCHANGE_01 = "3-033-0001"; //F011127
    public static final String SERVERCONSUMERMONITORLISTENER_CONSUMERSETCHANGE_02 = "3-033-0002"; //F011127
    //Add byte values for boolean true and false.
    public static final byte TRUE_BYTE = 0x00;
    public static final byte FALSE_BYTE = 0x01;
}
