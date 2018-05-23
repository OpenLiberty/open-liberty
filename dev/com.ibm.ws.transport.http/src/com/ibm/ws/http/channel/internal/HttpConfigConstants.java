/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

/**
 * Container for all of the various constants used in the HTTP Channel's
 * configuration code.
 */
public class HttpConfigConstants {

    /**
     * Private constructor... this class just provides some constants
     *
     */
    private HttpConfigConstants() {
        // nothing to do
    }

    /**
     * Maximum requests allowed on a single HTTP connection.
     */
    public static final String PROPNAME_MAX_PERSIST = "MaxKeepAliveRequests";

    /**
     * When creating an outbound HTTP request, this parameter controls
     * the default HTTP version in the message. Expected values are
     * 1.0 or 1.1
     */
    public static final String PROPNAME_OUTGOING_VERSION = "outgoingVersion";

    /**
     * When allocating new WsByteBuffers in the channel, this parameter
     * controls whether to use direct or indirect byte buffers. This
     * is a boolean value.
     */
    public static final String PROPNAME_DIRECT_BUFF = "useDirectBuffers";

    /**
     * When sending an outgoing HTTP message, this controls whether or
     * not to default to a persistent connection (Keep-Alive) as opposed
     * to a connection that will close after one request/response exchange.
     * This is a boolean value.
     * <p>
     * If this is set to false, then it will override any other persistence setting such as MaxKeepAliveRequests.
     */
    public static final String PROPNAME_KEEPALIVE_ENABLED = "KeepAliveEnabled";

    /**
     * While parsing incoming HTTP headers, this parameter controls the
     * internal cache size.
     */
    public static final String PROPNAME_BYTE_CACHE_SIZE = "byteCacheSize";

    /**
     * When allocating buffers for sending outgoing HTTP headers, this is
     * the size of each individual buffers.
     */
    public static final String PROPNAME_OUTGOING_HDR_BUFFSIZE = "outgoingHdrBufferSize";

    /**
     * While parsing the headers of an incoming HTTP message, this
     * controls the size of each buffer that receives the data.
     */
    public static final String PROPNAME_INCOMING_HDR_BUFFSIZE = "incomingHdrBufferSize";

    /**
     * While reading the body of an incoming HTTP message, this
     * controls the size of each buffer being used.
     */
    public static final String PROPNAME_INCOMING_BODY_BUFFSIZE = "incomingBodyBufferSize";

    /**
     * This timeout value controls the allowed idle time on
     * a socket between client requests. Once this timeout is passed,
     * the connection will be closed.
     * <p>
     * This integer value represents time in milliseconds.
     */
    public static final String PROPNAME_PERSIST_TIMEOUT = "persistTimeout";

    /**
     * This timeout value controls the allowed time to wait while
     * performing a read of data on a socket.
     * <p>
     * This integer value represents time in milliseconds.
     */
    public static final String PROPNAME_READ_TIMEOUT = "readTimeout";

    /**
     * This timeout value controls the allowed time to wait while
     * performing a write of data on a socket.
     * <p
     * . This integer value represents time in milliseconds.
     */
    public static final String PROPNAME_WRITE_TIMEOUT = "writeTimeout";

    /**
     * Property name for the flag on whether to extract the header
     * values out of the incoming buffers immediately or to delay
     * that extraction until a channel requests the header value.
     * True means to extract immediately and is intended for situations
     * where the channel is always going to be requesting the
     * majority of the headers. False would be when only a few
     * header values are ever queried.
     */
    public static final String PROPNAME_EXTRACT_VALUE = "extractValue";

    /**
     * Outgoing and incoming data for this channel can be sent in the
     * regular String encoding, or with the optional binary transport
     * mode. The channel at the other end of the connection must use
     * the same encoding method for the connection to work correctly.
     * This is a boolean value.
     */
    public static final String PROPNAME_BINARY_TRANSPORT = "enableBinaryTransport";

    /**
     * Property name for the client access log id.
     */
    public static final String PROPNAME_ACCESSLOG_ID = "accessLogID";
    /**
     * Property name for the client error log path plus filename. This logging
     * handles all of the important information related to connections not
     * covered by the access log.
     */
    public static final String PROPNAME_ERRORLOG_FILENAME = "errorLogFileName";

    /**
     * Property name for the level of logging to enable on this channel. Only
     * used if the internal channel logging is enabled (not the global service)
     */
    public static final String PROPNAME_LOGGING_LEVEL = "loggingLevel";

    /**
     * Property name for the maximum number of backup files to keep of the
     * error logs.
     */
    public static final String PROPNAME_ERRORLOG_MAXFILES = "MaximumErrorBackupFiles";

    /**
     * The local HTTP channel will adjust the filenames of the access and error
     * logs by prepending this prefix onto the name, thus logs/httpaccess.log
     * would become logs/localhttpaccess.log if the prefix was set to "local".
     */
    public static final String PROPNAME_LOCALLOG_PREFIX = "localLogFilenamePrefix";

    /**
     * Property name for the maximum size of any parsed field in the HTTP
     * messages (header value, URI, etc)
     */
    public static final String PROPNAME_LIMIT_FIELDSIZE = "limitFieldSize";

    /**
     * Property name for the maximum number of headers allowed in each HTTP
     * message.
     */
    public static final String PROPNAME_LIMIT_NUMHEADERS = "limitNumHeaders";

    /**
     * Property name for whether any socket retries are allowed with outbound
     * connections (does not apply to inbound connections). This covers any
     * attempt to reconnec to a target server and/or rewrite or reread data.
     */
    public static final String PROPNAME_ALLOW_RETRIES = "allowRetries";

    /**
     * Property name used to store the value on whether we are running this
     * channel instance inside the Z/OS servant region.
     */
    public static final String PROPNAME_SERVANT_REGION = "ServantRegion";

    /**
     * Property name used to store the value on whether we are running this
     * channel instance inside the Z/OS control region.
     */
    public static final String PROPNAME_CONTROL_REGION = "ControlRegion";

    /**
     * Property used to stored whether the channel is running on z/OS or not,
     * any region.
     */
    public static final String PROPNAME_RUNNING_ON_ZOS = "RunningOnZOS";

    /**
     * Property name used to indicate that this channel should only use JIT
     * allocate reads (if set to true). If false, then the default behavior
     * of picking JIT allocate reads or providing a buffer is used as normal.
     */
    public static final String PROPNAME_JIT_ONLY_READS = "JITOnlyReads";

    /**
     * Property that indicates whether the outgoing header values should have
     * verification checks performed to ensure no CRLF characters are contained
     * inside them.
     */
    public static final String PROPNAME_HEADER_VALIDATION = "splitResponseProtection";

    /**
     * Property that is used to limit the acceptable size of an incoming
     * message. If one arrives larger than this, then an error scenario is
     * triggered.
     */
    public static final String PROPNAME_MSG_SIZE_LIMIT = "MessageSizeLimit";

    /**
     * Property that extends the message size limit by allowing a single message
     * over the standard limit but under this higher size. If one message is
     * active in the system already in this range, then no others are allowed
     * in over the standard size limit, until that active message finishs.
     */
    public static final String PROPNAME_MSG_SIZE_LARGEBUFFER = "LargeMessageSize";

    /**
     * Property that allows the admin to limit the number of temporary response
     * messages the HTTP channel will parse before closing down the connection.
     */
    public static final String PROPNAME_LIMIT_NUMBER_RESPONSES = "LimitNumberResponses";

    /**
     * Property controlling whether or not URL parsing obeys strict RFC
     * compliance.
     */
    public static final String PROPNAME_STRICT_URL_FORMAT = "StrictURLFormat";

    /**
     * Property that allows the user to configure the Server header that will
     * be sent out with response messages (inbound chains only).
     */
    // @295174 - control Server header contents
    public static final String PROPNAME_SERVER_HEADER_VALUE = "ServerHeaderValue";

    /**
     * Property that allows the user to force the removal of any existing Server
     * header in the outgoing response messages (inbound chains only).
     */
    // @295174 - control Server header contents
    public static final String PROPNAME_REMOVE_SERVER_HEADER = "RemoveServerHeader";

    /**
     * Property that allows the user to control the range of the cached Date
     * header to be used (i.e. if the cache value falls within the allowed range
     * then it will be used and not reformatted).
     */
    // @313641 - control Date header formation
    public static final String PROPNAME_DATE_HEADER_RANGE = "DateHeaderRange";

    /**
     * Property that allows the user to control whether or not the presence of
     * a Set-Cookie(2) header should update the Cache-Control header with a
     * matching no-cache value, as well as adding the Expires header.
     */
    // PK20531 - add Cache-Control header
    public static final String PROPNAME_COOKIES_CONFIGURE_NOCACHE = "CookiesConfigureNoCache";

    /**
     * Property that limits the number of allowed header changes before the
     * headers will be remarshalled. This is used for a proxy env where the
     * message was originally parsed on one side and will be sent out the other
     * side, and if the changes are under the limit then the original parse
     * buffers will be used.
     */
    // @LIDB4530
    public static final String PROPNAME_HEADER_CHANGE_LIMIT = "HeaderChangeLimit";

    /**
     * Property controlling whether or not autodecompression of HTTP bodies is
     * on or off.
     */
    // PK41619 - allow autodecompression to be disabled
    public static final String PROPNAME_AUTODECOMPRESSION = "AutoDecompression";

    /**
     * Property to decide whether or not the request smuggling protection code
     * is on or off. If it's on, then malformed messages with both Content-Length
     * and Transfer-Encoding: chunked headers will disable HTTP persistence.
     */
    // PK53193
    public static final String PROPNAME_ENABLE_SMUGGLING_PROTECTION = "EnableSmugglingProtection";

    /**
     * Property name for the FRCA specific access log file.
     */
    public static final String PROPNAME_FRCALOG_NAME = "FRCALogName";

    /**
     * Property name for the FRCA log's maximum allowed file size before rollover.
     */
    public static final String PROPNAME_FRCALOG_MAXSIZE = "FRCALogMaximumSize";

    /**
     * Property name for the FRCA log's maximum number of backup files to keep.
     */
    public static final String PROPNAME_FRCALOG_MAXFILES = "FRCALogMaximumBackupFiles";

    /**
     * Property name for the FRCA log's access log format to use.
     */
    public static final String PROPNAME_FRCALOG_FORMAT = "FRCALogAccessFormat";

    /**
     * Property controlling whether we should prevent multiple Set-Cookies with the same name.
     */
    public static final String PROPNAME_DO_NOT_ALLOW_DUPLICATE_SET_COOKIES = "DoNotAllowDuplicateSetCookies";

    /**
     * PI33453 - Property controlling whether to wait for data, if not immediately available,
     * to determine if end of message has been parsed.
     */
    public static final String PROPNAME_WAIT_FOR_END_OF_MESSAGE = "WaitForEndOfMessage";

    /**
     * Property controlling whether we send the content length on response messages
     * with 1xx or 204 status codes
     */
    // PI35277 - Property to not send content-length header on 1xx and 204 status codes
    public static final String REMOVE_CLHEADER_IN_TEMP_STATUS_RFC7230_COMPAT = "RemoveCLHeaderInTempStatusRespRFC7230compat";

    /**
     * Property controlling whether we should attempt to purge any left over data
     * if we are going to close the connection instead of persist it
     */
    // PI11176 - Attempt to Flush the data at the end of the request
    public static final String PROPNAME_PURGE_DATA_DURING_CLOSE = "PurgeDataDuringClose";

    public static final String PROPNAME_H2_CONN_CLOSE_TIMEOUT = "H2ConnCloseTimeout";

    public static final String PROPNAME_H2_CONN_READ_WINDOW_SIZE = "H2ConnReadWindowSize";

    //PI81572 - Purge the remaining response body off the wire
    public static final String PROPNAME_PURGE_REMAINING_RESPONSE = "PurgeRemainingResponseBody";

    /** Minimum setting on the persistent requests (-1 is unlimited) */
    public static final int MIN_PERSIST_REQ = -1;
    /** Minimum allowed size for the byte cache */
    public static final int MIN_BYTE_CACHE_SIZE = 256;
    /** Minimum buffer size allowed to be allocated */
    public static final int MIN_BUFFER_SIZE = 1024;
    /** Minimum allowed setting for the maximum number of headers allowed */
    public static final int MIN_LIMIT_NUMHEADERS = 50;
    /** Minimum timeout value allowed */
    public static final int MIN_TIMEOUT = 0;
    /** Minimum allowed setting for the limit on the field size */
    public static final int MIN_LIMIT_FIELDSIZE = 50;

    /** Maximum buffer size allowed to be allocated */
    public static final int MAX_BUFFER_SIZE = 1048576;
    /** Maximum size to allow the byte cache to be set to */
    public static final int MAX_BYTE_CACHE_SIZE = 2048;
    /** Maximum allowed setting for the limit on the field size */
    public static final int MAX_LIMIT_FIELDSIZE = 32768;
    /** Maximum allowed setting for the limit on the number of headers */
    public static final int MAX_LIMIT_NUMHEADERS = 500;
    /** Maximum number of responses to skip past */
    public static final int MAX_LIMIT_NUMRESPONSES = 50;

    /** Several configuration constants allow unlimited values */
    public static final int UNLIMITED = -1;

    /** Will allow Cookie to be created with RFC1123 format i.e. 4 digit year */
    public static final String PROPNAME_V0_COOKIE_RFC1123_COMPAT = "v0CookieDateRFC1123compat";

    /**
     * Property controlling whether we should automatically add the quote to the cookie
     * path attribute
     */
    public static final String PROPNAME_SKIP_PATH_QUOTE = "SkipCookiePathQuotes";

    /** Will prevent response splitting */
    public static final String PROPNAME_PREVENT_RESPONSE_SPLIT = "PreventReponseSplit"; //PI45266

    /** Will swallow inbound connections IOE */
    public static final String PROPNAME_THROW_IOE_FOR_INBOUND_CONNECTIONS = "ThrowIOEForInboundConnections"; //PI57542

    /** Equivalent to CookiesConfigureNoCache - required due to 'configure' not being an allowed word for metatype **/
    public static final String PROPNAME_NO_CACHE_COOKIES_CONTROL = "NoCacheCookiesControl"; //PI75280

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

    public static final int MIN_LIMIT_FRAME_SIZE = 16384;
    public static final int MAX_LIMIT_FRAME_SIZE = 16777215;

    public static final String PROPNAME_H2_CONNECTION_IDLE_TIMEOUT = "http2ConnectionIdleTimeout";
    public static final String PROPNAME_H2_MAX_CONCURRENT_STREAMS = "maxConcurrentStreams";
    public static final String PROPNAME_H2_MAX_FRAME_SIZE = "maxFrameSize";

}
