/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.h2internal.Constants;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.internal.HttpEndpointImpl;
import com.ibm.ws.http.logging.internal.DisabledLogger;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.http.logging.AccessLog;
import com.ibm.wsspi.http.logging.DebugLog;

/**
 * Class to handle parsing the configuration data and storing/supplying the
 * various configuration parameters to the rest of the code.
 *
 */
public class HttpChannelConfig {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpChannelConfig.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Multiplier for converting from seconds to milliseconds */
    private static final int TIMEOUT_MODIFIER = 1000;

    /** Maximum persistent requests to allow on a single socket */
    private int maxPersistRequest = 100;
    /** Default HTTP version to put into an outgoing HTTP message */
    private VersionValues outgoingHttpVersion = VersionValues.V11;
    /** Flag on whether to allocate direct or indirect byte buffers. */
    private boolean bDirectBuffers = true;
    /** Flag on whether to default outgoing messages to Keep-Alive or not. */
    private boolean bKeepAliveEnabled = true;
    /** Size of buffers to use while marshalling outgoing headers. */
    private int outgoingHdrBuffSize = 1024;
    /** Size of buffers to use while parsing incoming headers. */
    private int incomingHdrBuffSize = 8192;
    /** Size of buffers to use while reading incoming bodies. */
    private int incomingBodyBuffSize = 32768;
    /** Time to wait for additional requests on a socket (milliseconds). */
    private int persistTimeout = 30000;
    /** Time to wait for a read to complete (milliseconds). */
    private int readTimeout = 60000;
    /** Time to wait for a write to complete (milliseconds). */
    private int writeTimeout = 60000;
    /** Size of the byte cache to use while parsing data. */
    private int byteCacheSize = 512;
    /** Flag on whether to extract the header values or not. */
    private boolean bExtractValue = true;
    /** Flag on whether to use binary HTTP transport encoding/decoding */
    private boolean bBinaryTransport = false;
    /** NCSA access logger reference */
    private AtomicReference<AccessLog> accessLogger = new AtomicReference<AccessLog>(DisabledLogger.getRef());
    /** Debug/error logger reference */
    private final DebugLog debugLogger = DisabledLogger.getRef();
    /** Setting for the maximum field size of a message */
    private int limitFieldSize = HttpConfigConstants.MAX_LIMIT_FIELDSIZE;
    /** Setting for the maximum number of headers per message */
    private int limitNumHeaders = HttpConfigConstants.MAX_LIMIT_NUMHEADERS;
    /** Setting limiting the number of temporary responses we will skip */
    private int limitNumResponses = 10;
    /** Setting limiting the allowed incoming body size */
    private long limitMessageSize = HttpConfigConstants.UNLIMITED;
    /** Setting for whether retries are allowed on outbound connections */
    private boolean bAllowRetries = true;
    /** Flag on whether we're running on the Z/OS servant region or not */
    private final boolean bServantRegion = false;
    /** Flag on whether or not we are running on a z/OS machine */
    private final boolean bRunningOnZOS = false;
    /** Flag on whether or not we are running in a z/OS Control region */
    private final boolean bControlRegion = false;
    /** Flag on whether to do verification against outgoing headers */
    private boolean bHeaderValidation = true;
    /** Flag on whether to force JIT allocate only reads */
    private boolean bJITOnlyReads = false;
    /** Flag on whether to enforce strict RFC compliance with parsing URLs */
    private boolean bStrictURLFormat = false;
    /** PK15848 - Flag on whether we should remove any Server header */
    private boolean bRemoveServerHeader = false;
    /** PK15848 - Server header value to give to response messages */
    private byte[] baServerHeaderValue = null;
    /** Range in milliseconds to allow cached Date header values to be used */
    private long lDateHeaderRange = 1000L;
    /** PK20531 - Whether Set-Cookie should update Cache-Control header */
    private boolean bCookiesConfigureNoCache = true;
    /** LI4530 - Change limit before all headers are always remarshalled */
    private int headerChangeLimit = -1;
    /** PK41619 - whether body autodecompression is on or off */
    private boolean bAutoDecompression = true;
    /** PK53193 - whether req smuggling protection is on or off */
    private boolean bEnableSmugglingProtection = true;
    /** Whether 4 digit year format is output on v0 cookies or not */
    private boolean v0CookieDateRFC1123compat = true;
    /** PI31734 - Prevent multiple Set-Cookies with the same name */
    private boolean doNotAllowDuplicateSetCookies = false;
    /**
     * PI33453 - Wait for end of message data, if not immediately available, after the first CRLF
     * sequence following the 0 byte chunk.
     */
    private boolean waitForEndOfMessage = false;
    /** PI35277 - Property to not send content-length header on 1xx and 204 status codes */
    private boolean removeCLHeaderInTempStatusRespRFC7230compat = false;
    /** PI45266 SECINT must be true by default */
    private boolean preventResponseSplit = true;
    /** PI11176 - Attempt to purge the data at the close of the connection */
    private boolean attemptPurgeData = false;

    /** PI57542 - Throw IOE for inbound connections */
    private Boolean throwIOEForInboundConnections = null;

    /** 738893 - Should the HTTP Channel skip adding the quotes to the cookie's path attribute */
    private boolean skipCookiePathQuotes = false;
    /** The amount of time the connection will be left open when HTTP/2 goes into an idle state */
    private long h2ConnectionCloseTimeout = 30;
    private int h2ConnectionReadWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE; // init the connection read window to the spec max
    /** PI81572 Purge the remaining response body off the wire when clear is called */
    private boolean purgeRemainingResponseBody = true;

    /** Set as an attribute to the HttpEndpoint **/
    private Boolean useH2ProtocolAttribute = null;

    private int http2ConnectionIdleTimeout = 0;
    private int http2MaxConcurrentStreams = 200;
    private int http2MaxFrameSize = 57344; //Default to 56kb
    /** Identifies if the channel has been configured to use X-Forwarded-* and Forwarded headers */
    private boolean useForwardingHeaders = false;
    /** Regex to be used to verify that proxies in forwarded headers are known to user */
    private String proxiesRegex = HttpConfigConstants.DEFAULT_PROXIES_REGEX;
    private Pattern proxiesPattern = null;
    /**
     * Set as an attribute to the remoteIP element to specify if X-Forwarded-* and Forwarded header
     * values affect the NCSA Access Log remote directives
     */
    private boolean useForwardingHeadersInAccessLog = false;

    /**
     * Constructor for an HTTP channel config object.
     *
     * @param cc
     */
    public HttpChannelConfig(ChannelData cc) {
        parseConfig(cc);
    }

    /**
     * Update the existing configuration with the input channel config object.
     *
     * @param cc
     */
    public void updateConfig(ChannelData cc) {
        parseConfig(cc);
    }

    /**
     * Parse the configuration data into the separate values.
     *
     * @param cc
     */
    private void parseConfig(ChannelData cc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "parseConfig: " + cc.getName());
        }

        Map<Object, Object> propsIn = cc.getPropertyBag();

        Map<Object, Object> props = new HashMap<Object, Object>();
        // convert all keys to valid case independent of case
        String key;
        Object value;

        // match keys independent of case.
        // So this is a bit ugly, but the parsing code is not state independent, meaning if it parses A then and
        // only then it will parse B, and we can't be certain that only properties that we know about from the HTTP Config
        // are in this Map (so we can't just lower case everything).  So, to be case independent we need to convert
        // the entries to their known internal string constants.  We shouldn't need to configure the channel often, and there
        // should not be many custom properties, so performance should not be an issue.
        for (Entry<Object, Object> entry : propsIn.entrySet()) {
            key = (String) entry.getKey();
            value = entry.getValue();

            // First comparisons are for ones exposed in metatype.xml
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_KEEPALIVE_ENABLED)) {
                props.put(HttpConfigConstants.PROPNAME_KEEPALIVE_ENABLED, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_MAX_PERSIST)) {
                props.put(HttpConfigConstants.PROPNAME_MAX_PERSIST, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PERSIST_TIMEOUT)) {
                props.put(HttpConfigConstants.PROPNAME_PERSIST_TIMEOUT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_READ_TIMEOUT)) {
                props.put(HttpConfigConstants.PROPNAME_READ_TIMEOUT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_WRITE_TIMEOUT)) {
                props.put(HttpConfigConstants.PROPNAME_WRITE_TIMEOUT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_JIT_ONLY_READS)) {
                props.put(HttpConfigConstants.PROPNAME_JIT_ONLY_READS, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_OUTGOING_VERSION)) {
                props.put(HttpConfigConstants.PROPNAME_OUTGOING_VERSION, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DIRECT_BUFF)) {
                props.put(HttpConfigConstants.PROPNAME_DIRECT_BUFF, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_OUTGOING_HDR_BUFFSIZE)) {
                props.put(HttpConfigConstants.PROPNAME_OUTGOING_HDR_BUFFSIZE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_INCOMING_HDR_BUFFSIZE)) {
                props.put(HttpConfigConstants.PROPNAME_INCOMING_HDR_BUFFSIZE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_INCOMING_BODY_BUFFSIZE)) {
                props.put(HttpConfigConstants.PROPNAME_INCOMING_BODY_BUFFSIZE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_BYTE_CACHE_SIZE)) {
                props.put(HttpConfigConstants.PROPNAME_BYTE_CACHE_SIZE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_EXTRACT_VALUE)) {
                props.put(HttpConfigConstants.PROPNAME_EXTRACT_VALUE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_BINARY_TRANSPORT)) {
                props.put(HttpConfigConstants.PROPNAME_BINARY_TRANSPORT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_LIMIT_FIELDSIZE)) {
                props.put(HttpConfigConstants.PROPNAME_LIMIT_FIELDSIZE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_LIMIT_NUMHEADERS)) {
                props.put(HttpConfigConstants.PROPNAME_LIMIT_NUMHEADERS, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_LIMIT_NUMBER_RESPONSES)) {
                props.put(HttpConfigConstants.PROPNAME_LIMIT_NUMBER_RESPONSES, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_MSG_SIZE_LIMIT)) {
                props.put(HttpConfigConstants.PROPNAME_MSG_SIZE_LIMIT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_ALLOW_RETRIES)) {
                props.put(HttpConfigConstants.PROPNAME_ALLOW_RETRIES, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_HEADER_VALIDATION)) {
                props.put(HttpConfigConstants.PROPNAME_HEADER_VALIDATION, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_STRICT_URL_FORMAT)) {
                props.put(HttpConfigConstants.PROPNAME_STRICT_URL_FORMAT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SERVER_HEADER_VALUE)) {
                props.put(HttpConfigConstants.PROPNAME_SERVER_HEADER_VALUE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER)) {
                props.put(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DATE_HEADER_RANGE)) {
                props.put(HttpConfigConstants.PROPNAME_DATE_HEADER_RANGE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE)) {
                props.put(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_HEADER_CHANGE_LIMIT)) {
                props.put(HttpConfigConstants.PROPNAME_HEADER_CHANGE_LIMIT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_AUTODECOMPRESSION)) {
                props.put(HttpConfigConstants.PROPNAME_AUTODECOMPRESSION, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_ENABLE_SMUGGLING_PROTECTION)) {
                props.put(HttpConfigConstants.PROPNAME_ENABLE_SMUGGLING_PROTECTION, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RUNNING_ON_ZOS)) {
                props.put(HttpConfigConstants.PROPNAME_RUNNING_ON_ZOS, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SERVANT_REGION)) {
                props.put(HttpConfigConstants.PROPNAME_SERVANT_REGION, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_CONTROL_REGION)) {
                props.put(HttpConfigConstants.PROPNAME_CONTROL_REGION, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_V0_COOKIE_RFC1123_COMPAT)) {
                props.put(HttpConfigConstants.PROPNAME_V0_COOKIE_RFC1123_COMPAT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DO_NOT_ALLOW_DUPLICATE_SET_COOKIES)) {
                props.put(HttpConfigConstants.PROPNAME_DO_NOT_ALLOW_DUPLICATE_SET_COOKIES, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_WAIT_FOR_END_OF_MESSAGE)) {
                props.put(HttpConfigConstants.PROPNAME_WAIT_FOR_END_OF_MESSAGE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.REMOVE_CLHEADER_IN_TEMP_STATUS_RFC7230_COMPAT)) {
                props.put(HttpConfigConstants.REMOVE_CLHEADER_IN_TEMP_STATUS_RFC7230_COMPAT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PREVENT_RESPONSE_SPLIT)) {
                props.put(HttpConfigConstants.PROPNAME_PREVENT_RESPONSE_SPLIT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PURGE_DATA_DURING_CLOSE)) {
                props.put(HttpConfigConstants.PROPNAME_PURGE_DATA_DURING_CLOSE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_THROW_IOE_FOR_INBOUND_CONNECTIONS)) {
                props.put(HttpConfigConstants.PROPNAME_THROW_IOE_FOR_INBOUND_CONNECTIONS, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SKIP_PATH_QUOTE)) {
                props.put(HttpConfigConstants.PROPNAME_SKIP_PATH_QUOTE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_CONN_CLOSE_TIMEOUT)) {
                props.put(HttpConfigConstants.PROPNAME_H2_CONN_CLOSE_TIMEOUT, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_CONN_READ_WINDOW_SIZE)) {
                props.put(HttpConfigConstants.PROPNAME_H2_CONN_READ_WINDOW_SIZE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PURGE_REMAINING_RESPONSE)) {
                props.put(HttpConfigConstants.PROPNAME_PURGE_REMAINING_RESPONSE, value);
                continue;
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION)) {
                props.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, value);
                continue;
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_CONNECTION_IDLE_TIMEOUT)) {
                props.put(HttpConfigConstants.PROPNAME_H2_CONNECTION_IDLE_TIMEOUT, value);
                continue;
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_MAX_CONCURRENT_STREAMS)) {
                props.put(HttpConfigConstants.PROPNAME_H2_MAX_CONCURRENT_STREAMS, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_MAX_FRAME_SIZE)) {
                props.put(HttpConfigConstants.PROPNAME_H2_MAX_FRAME_SIZE, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_REMOTE_PROXIES)) {
                props.put(HttpConfigConstants.PROPNAME_REMOTE_PROXIES, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_REMOTE_IP)) {
                props.put(HttpConfigConstants.PROPNAME_REMOTE_IP, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG)) {
                props.put(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG, value);
            }

            props.put(key, value);
        }

        parseProtocolVersion(props);
        parsePersistence(props);
        parseOutgoingVersion(props);
        parseBufferType(props);
        parseOutgoingBufferSize(props);
        parseIncomingHdrBufferSize(props);
        parseIncomingBodyBufferSize(props);
        parsePersistTimeout(props);
        parseReadTimeout(props);
        parseWriteTimeout(props);
        parseByteCacheSize(props);
        parseDelayedExtract(props);
        parseBinaryTransport(props);
        parseLimitFieldSize(props);
        parseLimitNumberHeaders(props);
        parseLimitNumberResponses(props);
        parseLimitMessageSize(props);
        parseAllowRetries(props);
        parseLoggingInfo(props);
        parseHeaderValidation(props);
        parseStrictURLFormat(props);
        parseServerHeader(props);
        parseDateHeaderRange(props);
        parseCookieUpdate(props);//PI75280
        parseHeaderChangeLimit(props);
        parseAutoDecompression(props);
        parseRequestSmugglingProtection(props);
        parsev0CookieDateRFC1123compat(props);
        parseDoNotAllowDuplicateSetCookies(props); //PI31734
        parseWaitForEndOfMessage(props); //PI33453
        parseRemoveCLHeaderInTempStatusRespRFC7230compat(props);//PI35277
        parsePreventResponseSplit(props); //PI45266
        parseAttemptPurgeData(props); //PI11176
        parseThrowIOEForInboundConnections(props); //PI57542
        parseSkipCookiePathQuotes(props); //738893
        parseH2ConnCloseTimeout(props);
        parseH2ConnReadWindowSize(props);
        parsePurgeRemainingResponseBody(props); //PI81572
        parseH2ConnectionIdleTimeout(props);
        parseH2MaxConcurrentStreams(props);
        parseH2MaxFrameSize(props);
        parseRemoteIp(props);
        parseRemoteIpProxies(props);
        parseRemoteIpAccessLog(props);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "parseConfig");
        }
    }

    /**
     * Access the property that may or may not exist for the given key.
     *
     * @param props
     * @param key
     * @return String
     */
    private String getProp(Map<Object, Object> props, String key) {
        String value = (String) props.get(key);
        if (null == value) {
            value = (String) props.get(key.toLowerCase());
        }
        return (null != value) ? value.trim() : null;
    }

    /**
     * Method to handle parsing all of the persistence related configuration
     * values.
     *
     * @param props
     */
    private void parsePersistence(Map<Object, Object> props) {
        parseKeepAliveEnabled(props);
        if (isKeepAliveEnabled()) {
            parseMaxPersist(props);
        }
    }

    /**
     * Check the input configuration for the default flag on whether to use
     * persistent connections or not. If this is false, then the other related
     * configuration values will be ignored (such as MaxKeepAliveRequests).
     *
     * @param props
     */
    private void parseKeepAliveEnabled(Map<Object, Object> props) {
        boolean flag = this.bKeepAliveEnabled;
        Object value = props.get(HttpConfigConstants.PROPNAME_KEEPALIVE_ENABLED);
        if (null != value) {
            flag = convertBoolean(value);
        }
        this.bKeepAliveEnabled = flag;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Config: KeepAliveEnabled is " + isKeepAliveEnabled());
        }
    }

    /**
     * Check the input configuration for the maximum allowed requests per socket
     * setting.
     *
     * @param props
     */
    private void parseMaxPersist(Map<Object, Object> props) {
        // -1 means unlimited
        // 0..1 means 1
        // X means X
        Object value = props.get(HttpConfigConstants.PROPNAME_MAX_PERSIST);
        if (null != value) {
            try {
                this.maxPersistRequest = minLimit(convertInteger(value), HttpConfigConstants.MIN_PERSIST_REQ);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Max persistent requests is " + getMaximumPersistentRequests());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseMaxPersist", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid max persistent requests; " + value);
                }
            }
        }
    }

    /**
     * Check the input configuration for the default outgoing HTTP version
     * setting.
     *
     * @param props
     */
    private void parseOutgoingVersion(Map<Object, Object> props) {
        String value = getProp(props, HttpConfigConstants.PROPNAME_OUTGOING_VERSION);
        if ("1.0".equals(value)) {
            this.outgoingHttpVersion = VersionValues.V10;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: Outgoing version is " + getOutgoingVersion().getName());
            }
        }
    }

    /**
     * Check the input configuration for the type of ByteBuffer to use, direct
     * or indirect.
     *
     * @param props
     */
    private void parseBufferType(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_DIRECT_BUFF);
        if (null != value) {
            this.bDirectBuffers = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: use direct buffers is " + isDirectBufferType());
            }
        }
    }

    /**
     * Check the input configuration for the maximum buffer size allowed for
     * marshalling headers outbound.
     *
     * @param props
     */
    private void parseOutgoingBufferSize(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_OUTGOING_HDR_BUFFSIZE);
        if (null != value) {
            try {
                this.outgoingHdrBuffSize = rangeLimit(convertInteger(value), HttpConfigConstants.MIN_BUFFER_SIZE, HttpConfigConstants.MAX_BUFFER_SIZE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Outgoing hdr buffer size is " + getOutgoingHdrBufferSize());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseOutgoingBufferSize", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid outgoing header buffer size; " + value);
                }
            }
        }
    }

    /**
     * Check the input configuration for the buffer size to use when parsing
     * the incoming headers.
     *
     * @param props
     */
    private void parseIncomingHdrBufferSize(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_INCOMING_HDR_BUFFSIZE);
        if (null != value) {
            try {
                this.incomingHdrBuffSize = rangeLimit(convertInteger(value), HttpConfigConstants.MIN_BUFFER_SIZE, HttpConfigConstants.MAX_BUFFER_SIZE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Incoming hdr buffer size is " + getIncomingHdrBufferSize());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseIncomingHdrBufferSize", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid incoming hdr buffer size of " + value);
                }
            }
        }
    }

    /**
     * Check the input configuration for the buffer size to use when reading
     * the incoming body.
     *
     * @param props
     */
    private void parseIncomingBodyBufferSize(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_INCOMING_BODY_BUFFSIZE);
        if (null != value) {
            try {
                this.incomingBodyBuffSize = rangeLimit(convertInteger(value), HttpConfigConstants.MIN_BUFFER_SIZE, HttpConfigConstants.MAX_BUFFER_SIZE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Incoming body buffer size is " + getIncomingBodyBufferSize());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseIncomingBodyBufferSize", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid incoming body buffer size; " + value);
                }
            }
        }
    }

    /**
     * Check the input configuration for the timeout to use in between
     * persistent requests.
     *
     * @param props
     */
    private void parsePersistTimeout(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_PERSIST_TIMEOUT);
        if (null != value) {
            try {
                this.persistTimeout = TIMEOUT_MODIFIER * minLimit(convertInteger(value), HttpConfigConstants.MIN_TIMEOUT);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Persist timeout is " + getPersistTimeout());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parsePersistTimeout", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid persist timeout; " + value);
                }
            }
        }
    }

    /**
     * Check the input configuration for the timeout to use when doing any read
     * during a connection.
     *
     * @param props
     */
    private void parseReadTimeout(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_READ_TIMEOUT);
        if (null != value) {
            try {
                this.readTimeout = TIMEOUT_MODIFIER * minLimit(convertInteger(value), HttpConfigConstants.MIN_TIMEOUT);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Read timeout is " + getReadTimeout());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseReadTimeout", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid read timeout; " + value);
                }
            }
        }
    }

    /**
     * Check the input configuration for the timeout to use when writing data
     * during the connection.
     *
     * @param props
     */
    private void parseWriteTimeout(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_WRITE_TIMEOUT);
        if (null != value) {
            try {
                this.writeTimeout = TIMEOUT_MODIFIER * minLimit(convertInteger(value), HttpConfigConstants.MIN_TIMEOUT);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Write timeout is " + getWriteTimeout());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseWriteTimeout", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid write timeout; " + value);
                }
            }
        }
    }

    private void parseH2ConnectionIdleTimeout(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_H2_CONNECTION_IDLE_TIMEOUT);
        if (null != value) {
            try {
                this.http2ConnectionIdleTimeout = TIMEOUT_MODIFIER * minLimit(convertInteger(value), HttpConfigConstants.MIN_TIMEOUT);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: HTTP/2 Connection idle timeout is " + getH2ConnectionIdleTimeout());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2ConnectionIdleTimeout", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid HTTP/2 connection idle timeout; " + value);
                }

            }
        }

    }

    private void parseH2MaxConcurrentStreams(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_H2_MAX_CONCURRENT_STREAMS);
        if (null != value) {
            try {
                this.http2MaxConcurrentStreams = convertInteger(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: HTTP/2 Max Concurrent Streams is " + getH2MaxConcurrentStreams());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2MaxConcurrentStreams", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid HTTP/2 Max Concurrent Streams; " + value);

                }
            }
        }
    }

    private void parseH2MaxFrameSize(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_H2_MAX_FRAME_SIZE);
        if (null != value) {
            try {
                this.http2MaxFrameSize = rangeLimit(convertInteger(value), HttpConfigConstants.MIN_LIMIT_FRAME_SIZE, HttpConfigConstants.MAX_LIMIT_FRAME_SIZE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: HTTP/2 Max Frame Size is " + getH2MaxFrameSize());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2MaxFrameSize", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid HTTP/2 Frame Size; " + value);

                }
            }
        }
    }

    /**
     * Check the input configuration for the size of the parse byte cache to use.
     *
     * @param props
     */
    private void parseByteCacheSize(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_BYTE_CACHE_SIZE);
        if (null != value) {
            try {
                this.byteCacheSize = rangeLimit(convertInteger(value), HttpConfigConstants.MIN_BYTE_CACHE_SIZE, HttpConfigConstants.MAX_BYTE_CACHE_SIZE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: byte cache size is " + getByteCacheSize());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseByteCacheSize", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid bytecache setting of " + value);
                }
            }
        }
    }

    /**
     * Check the input configuration for the flag on whether to immediately
     * extract header values during the parsing stage or not.
     *
     * @param props
     */
    private void parseDelayedExtract(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_EXTRACT_VALUE);
        if (null != value) {
            this.bExtractValue = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: header value extraction is " + shouldExtractValue());
            }
        }
    }

    /**
     * Check the input configuration for whether the parsing and marshalling
     * should use the binary transport mode or not.
     *
     * @param props
     */
    private void parseBinaryTransport(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_BINARY_TRANSPORT);
        if (null != value) {
            this.bBinaryTransport = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: binary transport is " + isBinaryTransportEnabled());
            }
        }
    }

    /**
     * Check the input configuration for a maximum size allowed for HTTP fields.
     *
     * @param props
     */
    private void parseLimitFieldSize(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_LIMIT_FIELDSIZE);
        if (null != value) {
            try {
                this.limitFieldSize = rangeLimit(convertInteger(value), HttpConfigConstants.MIN_LIMIT_FIELDSIZE, HttpConfigConstants.MAX_LIMIT_FIELDSIZE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: field size limit is " + getLimitOfFieldSize());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseLimitFieldSize", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invaild max field size setting of " + value);
                }
            }
        }
    }

    /**
     * Check the input configuration for a maximum limit on the number of
     * headers allowed per message.
     *
     * @param props
     */
    private void parseLimitNumberHeaders(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_LIMIT_NUMHEADERS);
        if (null != value) {
            try {
                this.limitNumHeaders = rangeLimit(convertInteger(value), HttpConfigConstants.MIN_LIMIT_NUMHEADERS, HttpConfigConstants.MAX_LIMIT_NUMHEADERS);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Num hdrs limit is " + getLimitOnNumberOfHeaders());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseLimitNumberHeaders", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid number of headers limit; " + value);
                }
            }
        }
    }

    /**
     * Check the input configuration for a maximum limit on the number of
     * temporary responses that will be read and skipped past.
     *
     * @param props
     */
    private void parseLimitNumberResponses(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_LIMIT_NUMBER_RESPONSES);
        if (null != value) {
            try {
                int size = convertInteger(value);
                if (HttpConfigConstants.UNLIMITED == size) {
                    this.limitNumResponses = HttpConfigConstants.MAX_LIMIT_NUMRESPONSES;
                } else {
                    this.limitNumResponses = rangeLimit(size, 1, HttpConfigConstants.MAX_LIMIT_NUMRESPONSES);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Num responses limit is " + getLimitOnNumberOfResponses());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseLimitNumberResponses", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid max number of responses; " + value);
                }
            }
        }
    }

    /**
     * Parse the possible configuration limit on the incoming message body.
     * size.
     *
     * @param props
     */
    private void parseLimitMessageSize(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_MSG_SIZE_LIMIT);
        if (null != value) {
            try {
                this.limitMessageSize = convertLong(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Message size limit is " + getMessageSizeLimit());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseLimitMessageSize", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid message size limit; " + value);
                }
            }
        }
    }

    /**
     * Query the limit on the incoming message body, this will return UNLIMITED
     * if not set.
     *
     * @return long
     */
    public long getMessageSizeLimit() {
        return this.limitMessageSize;
    }

    /**
     * Parse the NCSA access log information from the property map.
     *
     * @param props
     */
    private void parseAccessLog(Map<Object, Object> props) {

        String id = (String) props.get(HttpConfigConstants.PROPNAME_ACCESSLOG_ID);
        if (id != null) {
            AtomicReference<AccessLog> aLog = HttpEndpointImpl.getAccessLogger(id);
            if (aLog != null) {
                this.accessLogger = aLog;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config: using logging service", accessLogger);
            }
        }
    }

    /**
     * Check the configuration to see if the remoteIp element has been configured
     * to consider forwarding header values in the NCSA Access Log
     *
     * @param props
     */
    private void parseRemoteIp(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_REMOTE_IP);
        if (null != value) {

            this.useForwardingHeaders = convertBoolean(value);

            if (this.useForwardingHeaders && (TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
                Tr.event(tc, "HTTP Channel Config: remoteIp has been enabled");
            }
        }

    }

    /**
     * @param props
     */
    private void parseRemoteIpProxies(Map<Object, Object> props) {
        String value = (String) props.get(HttpConfigConstants.PROPNAME_REMOTE_PROXIES);
        if (null != value) {
            this.proxiesRegex = value;

            if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
                Tr.event(tc, "RemoteIp Config: proxies regex set to: " + value);
            }
        }

        if (this.useForwardingHeaders) {
            this.proxiesPattern = Pattern.compile(this.proxiesRegex);
        }

    }

    private void parseRemoteIpAccessLog(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG);
        if (null != value) {
            this.useForwardingHeadersInAccessLog = convertBoolean(value);

            if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
                Tr.event(tc, "RemoteIp Config: useRemoteIpInAccessLog set to: " + useForwardingHeadersInAccessLog);
            }

        }
    }

    /**
     * Check the input configuration for the access/error logging configuration.
     *
     * @param props
     */
    private void parseLoggingInfo(Map<Object, Object> props) {
        parseAccessLog(props);
    }

    /**
     * Parse the input configuration for the flag on whether to allow retries
     * or not.
     *
     * @param props
     */
    private void parseAllowRetries(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_ALLOW_RETRIES);
        if (null != value) {
            this.bAllowRetries = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: allow retries is " + allowsRetries());
            }
        }
    }

    /**
     * Parse the configuration on whether to perform header validation or not.
     *
     * @param props
     */
    private void parseHeaderValidation(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_HEADER_VALIDATION);
        if (null != value) {
            this.bHeaderValidation = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: header validation is " + isHeaderValidationEnabled());
            }
        }
    }

    /**
     * Parse the configuration on whether to perform JIT allocate only reads
     * or leave it to the default behavior.
     *
     * @param props
     */
    private void parseJITOnlyReads(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_JIT_ONLY_READS);
        if (null != value) {
            this.bJITOnlyReads = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: JIT only reads is " + isJITOnlyReads());
            }
        }
    }

    /**
     * Check the input configuration to decide whether to enforce a strict RFC
     * compliance while parsing URLs.
     *
     * @param props
     */
    private void parseStrictURLFormat(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_STRICT_URL_FORMAT);
        if (null != value) {
            this.bStrictURLFormat = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: Strict URL formatting is " + isStrictURLFormat());
            }
        }
    }

    /**
     * Check the input configuration map for the parameters that control the
     * Server header value.
     *
     * @param props
     */
    private void parseServerHeader(Map<Object, Object> props) {
        // @PK15848
        String value = getProp(props, HttpConfigConstants.PROPNAME_SERVER_HEADER_VALUE);
        if (null == value || "".equals(value)) {
            // due to security change, do not default value in Server header. // PM87013 Start
        } else {
            if ("DefaultServerVersion".equalsIgnoreCase(value)) {
                value = "WebSphere Application Server";
            }
            this.baServerHeaderValue = GenericUtils.getEnglishBytes(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: server header value [" + value + "]");
            }
        }
        // PM87013 (PM75371) End

        Object ov = props.get(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER);
        if (null != ov) {
            this.bRemoveServerHeader = convertBoolean(ov);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: remove server header is " + removeServerHeader());
            }
        }
    }

    /**
     * Parse the date header range value from the input properties.
     *
     * @param props
     */
    private void parseDateHeaderRange(Map<Object, Object> props) {
        // @313642
        Object value = props.get(HttpConfigConstants.PROPNAME_DATE_HEADER_RANGE);
        if (null != value) {
            try {
                this.lDateHeaderRange = minLimit(convertLong(value), 0L);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: date header range is " + value);
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseDateHeaderRange", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid date header range; " + value);
                }
            }
        }
    }

    /**
     * Check the configuration map for the Set-Cookie updating no-cache value.
     *
     * @param props
     */
    private void parseCookieUpdate(Map<Object, Object> props) {
        //This property needed to be documented using a new name because
        //the original property contains a banned word for metatype: 'config'
        //This change will verify if either (or both) original/documented properties
        //are set. The instance variable they reference will be set to false if
        //either property is set to false.

        Object value = props.get(HttpConfigConstants.PROPNAME_NO_CACHE_COOKIES_CONTROL);
        Object value2 = props.get(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE);
        boolean documentedProperty = true;
        boolean originalProperty = true;

        if (null != value) {
            documentedProperty = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: set no-cache cookie control is " + documentedProperty);
            }
        }

        if (null != value2) {
            originalProperty = convertBoolean(value2);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: set-cookie configures no-cache is " + originalProperty);
            }
        }
        this.bCookiesConfigureNoCache = originalProperty && documentedProperty;
    }

    /**
     * Parse the header change limit property.
     *
     * @param props
     */
    private void parseHeaderChangeLimit(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_HEADER_CHANGE_LIMIT);
        if (null != value) {
            try {
                this.headerChangeLimit = convertInteger(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: header change limit is " + getHeaderChangeLimit());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseHeaderChangeLimit", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid header change count of " + value);
                }
            }
        }
    }

    /**
     * Check whether or not the request smuggling protection has been changed.
     *
     * @param props
     */
    private void parseRequestSmugglingProtection(Map<Object, Object> props) {
        // PK53193 - allow this to be disabled
        Object value = props.get(HttpConfigConstants.PROPNAME_ENABLE_SMUGGLING_PROTECTION);
        if (null != value) {
            this.bEnableSmugglingProtection = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config: request smuggling protection is " + this.bEnableSmugglingProtection);
            }
        }
    }

    /**
     * Query if this channel has the request smuggling protection on or off.
     *
     * @return boolean
     */
    public boolean isRequestSmugglingProtectionEnabled() {
        return this.bEnableSmugglingProtection;
    }

    /**
     * Check the configuration map for the property turning on or off the body
     * autodecompression code.
     *
     * @param props
     */
    private void parseAutoDecompression(Map<Object, Object> props) {
        // PK41619 - allow this to be turned off
        Object value = props.get(HttpConfigConstants.PROPNAME_AUTODECOMPRESSION);
        if (null != value) {
            this.bAutoDecompression = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: autodecompression is " + isAutoDecompressionEnabled());
            }
        }
    }

    /**
     * Check the for the property v0CookieDateRFC1123compat
     *
     * @param props
     */
    private void parsev0CookieDateRFC1123compat(Map<?, ?> props) {

        Object value = props.get(HttpConfigConstants.PROPNAME_V0_COOKIE_RFC1123_COMPAT);
        if (null != value) {
            this.v0CookieDateRFC1123compat = convertBoolean(value);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Config: v0CookieDateRFC1123compat is " + isv0CookieDateRFC1123compat() + " this = " + this);
        }
    }

    /**
     * Check the configuration map for if we should skip adding the quote
     * to the cookie path attribute
     *
     * @ param props
     */
    private void parseSkipCookiePathQuotes(Map<?, ?> props) {
        //738893 - Skip adding the quotes to the cookie path attribute
        String value = (String) props.get(HttpConfigConstants.PROPNAME_SKIP_PATH_QUOTE);
        if (null != value) {
            this.skipCookiePathQuotes = convertBoolean(value);
            if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
                Tr.event(tc, "Config: SkipCookiePathQuotes is " + shouldSkipCookiePathQuotes());
            }
        }
    }

    /**
     * Check the configuration map for the property to tell us if we should prevent multiple set-cookies with the same name
     *
     * @ param props
     */
    private void parseDoNotAllowDuplicateSetCookies(Map<?, ?> props) {
        //PI31734 - prevent multiple Set-Cookies with the same name
        String value = (String) props.get(HttpConfigConstants.PROPNAME_DO_NOT_ALLOW_DUPLICATE_SET_COOKIES);
        if (null != value) {
            this.doNotAllowDuplicateSetCookies = convertBoolean(value);
            if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
                Tr.event(tc, "Config: DoNotAllowDuplicateSetCookies is " + doNotAllowDuplicateSetCookies());
            }
        }
    }

    /**
     * Check the configuration map for using WaitForEndOfMessage
     *
     * @param props
     */
    private void parseWaitForEndOfMessage(Map props) {
        //PI11176
        String value = (String) props.get(HttpConfigConstants.PROPNAME_WAIT_FOR_END_OF_MESSAGE);
        if (null != value) {
            this.waitForEndOfMessage = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: PI33453:WaitForEndOfMessage is " + shouldWaitForEndOfMessage());
            }
        }
    }

    /**
     * Check the configuration map for to see if we should send or not content-length on 1xx and 204 responses
     *
     * @ param props
     */
    private void parseRemoveCLHeaderInTempStatusRespRFC7230compat(Map props) {
        //PI35277
        String value = (String) props.get(HttpConfigConstants.REMOVE_CLHEADER_IN_TEMP_STATUS_RFC7230_COMPAT);
        if (null != value) {
            this.removeCLHeaderInTempStatusRespRFC7230compat = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config: RemoveCLHeaderInTempStatusRespRFC7230compat "
                             + shouldRemoveCLHeaderInTempStatusRespRFC7230compat());
            }
        }
    }

    /**
     * Check the configuration map for if we should prevent response splitting
     *
     * @ param props
     */
    private void parsePreventResponseSplit(Map<?, ?> props) {
        //PI45266
        String value = (String) props.get(HttpConfigConstants.PROPNAME_PREVENT_RESPONSE_SPLIT);
        if (null != value) {
            this.preventResponseSplit = convertBoolean(value);
            if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
                Tr.event(tc, "Config: PreventResponseSplit is " + shouldPreventResponseSplit());
            }
        }
    }

    /**
     * Check the configuration map for using purge behavior at the close of the connection
     *
     * @ param props
     */
    private void parseAttemptPurgeData(Map props) {
        //PI11176
        String value = (String) props.get(HttpConfigConstants.PROPNAME_PURGE_DATA_DURING_CLOSE);
        if (null != value) {
            this.attemptPurgeData = convertBoolean(value);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: PI11176:PurgeDataDuringClose is " + shouldAttemptPurgeData());
            }
        }
    }

    /**
     * Check the configuration map for if we should swallow inbound connections IOEs
     *
     * @ param props
     */
    private void parseThrowIOEForInboundConnections(Map<?, ?> props) {
        //PI57542
        String value = (String) props.get(HttpConfigConstants.PROPNAME_THROW_IOE_FOR_INBOUND_CONNECTIONS);
        if (null != value) {
            this.throwIOEForInboundConnections = convertBoolean(value);
            if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
                Tr.event(tc, "Config: ThrowIOEForInboundConnections is " + throwIOEForInboundConnections());
            }
        }
    }

    private void parseH2ConnCloseTimeout(Map<?, ?> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_H2_CONN_CLOSE_TIMEOUT);
        if (null != value) {
            try {
                this.h2ConnectionCloseTimeout = convertLong(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: H2 Connection Close timeout is " + getH2ConnCloseTimeout());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2ConnCloseTimeout", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid H2 Connection Close Timeout of " + value);
                }
            }
        }
    }

    private void parseH2ConnReadWindowSize(Map<?, ?> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_H2_CONN_READ_WINDOW_SIZE);
        if (null != value) {
            try {
                if ((Long) value > Integer.MAX_VALUE) {
                    throw new ArithmeticException();
                }
                this.h2ConnectionReadWindowSize = (Integer) value;
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: H2 Connection Read Window Size is " + getH2ConnReadWindowSize());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2ConnReadWindowSize", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid H2 Connection Read Window Size of " + value);
                }
            } catch (ArithmeticException ae) {
                FFDCFilter.processException(ae, getClass().getName() + ".parseH2ConnReadWindowSize", "2");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid H2 Connection Read Window Size: cannot exceed 2^31 - 1.  Value was: " + value);
                }
            }
        }

    }

    /**
     * Check the configuration if we should purge the remaining response data
     * This is a JVM custom property as it's intended for outbound scenarios
     *
     * PI81572
     *
     * @ param props
     */
    private void parsePurgeRemainingResponseBody(Map<?, ?> props) {

        String purgeRemainingResponseProperty = AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
            @Override
            public String run() {
                return (System.getProperty(HttpConfigConstants.PROPNAME_PURGE_REMAINING_RESPONSE));
            }
        });

        if (purgeRemainingResponseProperty != null) {
            this.purgeRemainingResponseBody = convertBoolean(purgeRemainingResponseProperty);
            if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
                Tr.event(tc, "Config: PurgeRemainingResponseBody is " + shouldPurgeRemainingResponseBody());
            }
        }
    }

    /**
     * Check the configuration to see if there is a desired http protocol version
     * that has been provided for this HTTP Channel
     *
     * @param props
     */
    private void parseProtocolVersion(Map<?, ?> props) {
        Object protocolVersionProperty = props.get(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION);
        if (null != protocolVersionProperty) {

            String protocolVersion = ((String) protocolVersionProperty).toLowerCase();
            if (HttpConfigConstants.PROTOCOL_VERSION_11.equals(protocolVersion)) {
                this.useH2ProtocolAttribute = Boolean.FALSE;
            } else if (HttpConfigConstants.PROTOCOL_VERSION_2.equals(protocolVersion)) {
                this.useH2ProtocolAttribute = Boolean.TRUE;

            }

            if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled()) && this.useH2ProtocolAttribute != null) {
                Tr.event(tc, "HTTP Channel Config: versionProtocol has been set to " + protocolVersion);
            }

        }

    }

    /**
     * Configured http protocol version used by this HttpChannel
     *
     * @return
     */
    public Boolean getUseH2ProtocolAttribute() {
        return this.useH2ProtocolAttribute;
    }

    public int getH2ConnectionIdleTimeout() {
        return this.http2ConnectionIdleTimeout;
    }

    public int getH2MaxFrameSize() {
        return this.http2MaxFrameSize;
    }

    public int getH2MaxConcurrentStreams() {
        return this.http2MaxConcurrentStreams;
    }

    /**
     * Convert a String to a boolean value. If the string does not
     * match "true", then it defaults to false.
     *
     * @param s
     * @return boolean
     */
    private boolean convertBoolean(Object o) {
        if (o instanceof Boolean)
            return (Boolean) o;
        return "true".equalsIgnoreCase(o.toString().trim());
    }

    private int convertInteger(Object o) {
        if (o instanceof Integer)
            return (Integer) o;
        return Integer.parseInt(o.toString().trim());
    }

    private long convertLong(Object o) {
        if (o instanceof Long)
            return (Long) o;
        return Long.parseLong(o.toString().trim());
    }

    /**
     * Take the user input size variable and make sure it is inside
     * the given range of min and max. If it is outside the range,
     * then set the value to the closest limit.
     *
     * @param size
     * @param min
     * @param max
     * @return int
     */
    private int rangeLimit(int size, int min, int max) {
        if (size < min) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config: " + size + " too small");
            }
            return min;
        } else if (size > max) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config: " + size + " too large");
            }
            return max;
        }
        return size;
    }

    /**
     * Return the larger value of either the input int or the minimum
     * limit.
     *
     * @param input
     * @param min
     * @return int
     */
    private int minLimit(int input, int min) {
        if (input < min) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config: " + input + " too small.");
            }
            return min;
        }
        return input;
    }

    /**
     * Return the larger value of either the input long or the minimum
     * limit.
     *
     * @param input
     * @param min
     * @return int
     */
    private long minLimit(long input, long min) {
        if (input < min) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config: " + input + " too small.");
            }
            return min;
        }
        return input;
    }

    /**
     * Query the value of the "maximum requests" on a persistent connection.
     *
     * @return int
     */
    public int getMaximumPersistentRequests() {
        return this.maxPersistRequest;
    }

    /**
     * Query the value of the default outgoing HTTP version.
     *
     * @return VersionValues
     */
    public VersionValues getOutgoingVersion() {
        return this.outgoingHttpVersion;
    }

    /**
     * Query whether the configuration is for direct or indirect byte buffers.
     *
     * @return boolean (true means direct)
     */
    public boolean isDirectBufferType() {
        return this.bDirectBuffers;
    }

    /**
     * Query whether outgoing requests are "Keep-Alive" by default.
     *
     * @return boolean
     */
    public boolean isKeepAliveEnabled() {
        return this.bKeepAliveEnabled;
    }

    /**
     * Query the size of the buffers to use while marshalling headers.
     *
     * @return int
     */
    public int getOutgoingHdrBufferSize() {
        return this.outgoingHdrBuffSize;
    }

    /**
     * Query the size of the buffers to use while parsing incoming headers.
     *
     * @return int
     */
    public int getIncomingHdrBufferSize() {
        return this.incomingHdrBuffSize;
    }

    /**
     * Query the size of the buffers to use while reading message bodies.
     *
     * @return int
     */
    public int getIncomingBodyBufferSize() {
        return this.incomingBodyBuffSize;
    }

    /**
     * Query the timeout value to use while waiting for secondary requests
     * on a persistent connection (time idle between requests).
     *
     * @return int
     */
    public int getPersistTimeout() {
        return this.persistTimeout;
    }

    /**
     * Query the timeout value used while waiting on a read to finish.
     *
     * @return int
     */
    public int getReadTimeout() {
        return this.readTimeout;
    }

    /**
     * Query the timeout value used while waiting on a write to finish.
     *
     * @return int
     */
    public int getWriteTimeout() {
        return this.writeTimeout;
    }

    /**
     * Size of the buffer to cache bytes for parsing tokens.
     *
     * @return int
     */
    public int getByteCacheSize() {
        return this.byteCacheSize;
    }

    /**
     * Query whether the configuration says to extract the header
     * value immediately (true) or wait until it is requested by a
     * channel (false).
     *
     * @return boolean
     */
    public boolean shouldExtractValue() {
        return this.bExtractValue;
    }

    /**
     * Query whether the configuration says to use the binary transport
     * encoding/decoding methods or not.
     *
     * @return boolean
     */
    public boolean isBinaryTransportEnabled() {
        return this.bBinaryTransport;
    }

    /**
     * Access the log for the NCSA access logging information.
     *
     * @return AccessLog
     */
    public AccessLog getAccessLog() {
        return this.accessLogger.get();
    }

    /**
     * Access the log file for generic HTTP debug or error messages.
     *
     * @return DebugLog
     */
    public DebugLog getDebugLog() {
        return this.debugLogger;
    }

    /**
     * Query the current size setting on the maximum limit for a field size.
     *
     * @return int
     */
    public int getLimitOfFieldSize() {
        return this.limitFieldSize;
    }

    /**
     * Query the current limit on number of headers allowed per message.
     *
     * @return int
     */
    public int getLimitOnNumberOfHeaders() {
        return this.limitNumHeaders;
    }

    /**
     * Query the current limit on number of temporary responses to allow.
     *
     * @return int
     */
    public int getLimitOnNumberOfResponses() {
        return this.limitNumResponses;
    }

    /**
     * Query whether this configuration allows retries or not.
     *
     * @return boolean
     */
    public boolean allowsRetries() {
        return this.bAllowRetries;
    }

    /**
     * Query whether we are running on the Z/OS servant region or not.
     *
     * @return boolean
     */
    public boolean isServantRegion() {
        return this.bServantRegion;
    }

    /**
     * Query whether we are running on the Z/OS control region or not.
     *
     * @return boolean
     */
    public boolean isControlRegion() {
        return this.bControlRegion;
    }

    /**
     * Query whether or not we are running on a z/OS machine.
     *
     * @return boolean
     */
    public boolean runningOnZOS() {
        return this.bRunningOnZOS;
    }

    /**
     * Query whether the header validation is enabled or not.
     *
     * @return boolean
     */
    public boolean isHeaderValidationEnabled() {
        return this.bHeaderValidation;
    }

    /**
     * Query whether this channel instance should perform JIT allocate only
     * reads or not.
     *
     * @return boolean
     */
    public boolean isJITOnlyReads() {
        return this.bJITOnlyReads;
    }

    /**
     * Query whether or not this config is enforcing strict URL parsing based
     * on RFCs.
     *
     * @return boolean
     */
    public boolean isStrictURLFormat() {
        return this.bStrictURLFormat;
    }

    /**
     * Query what the default server header value is.
     *
     * @return byte[]
     */
    public byte[] getServerHeaderValue() {
        // @PK15848
        return this.baServerHeaderValue;
    }

    /**
     * Query whether this channel should remove the Server header from outgoing
     * response message.
     *
     * @return boolean
     */
    public boolean removeServerHeader() {
        // @PK15848
        return this.bRemoveServerHeader;
    }

    /**
     * Query what the Date header range is configured to allow. <br>
     *
     * @return long
     */
    public long getDateHeaderRange() {
        return this.lDateHeaderRange;
    }

    /**
     * Query whether or not the presence of Set-Cookie(2) headers should auto-
     * matically add the Cache-Control no-cache values.
     *
     * @return boolean
     */
    public boolean shouldCookiesConfigureNoCache() {
        // @PK20531 - add Cache-Control header
        return this.bCookiesConfigureNoCache;
    }

    /**
     * Query the configured limit on the number of header changes to allow
     * before completely remarshalling the headers.
     *
     * @return int
     */
    public int getHeaderChangeLimit() {
        return this.headerChangeLimit;
    }

    /**
     * Query whether or not autodecompression of bodies is enabled or not.
     *
     * @return boolean
     */
    public boolean isAutoDecompressionEnabled() {
        // PK41619
        return this.bAutoDecompression;
    }

    /**
     * Query whether or not the HTTP access logging is enabled
     *
     * @return boolean
     */
    public boolean isAccessLoggingEnabled() {
        return this.accessLogger.get().isStarted();
    }

    /**
     * Query whether or not v0CookieDateRFC1123compat is set.
     *
     * @return boolean
     */
    public boolean isv0CookieDateRFC1123compat() {
        return this.v0CookieDateRFC1123compat;
    }

    /**
     * Query whether or not the HTTP Channel should skip adding the quotes
     * to the cookie attribute
     *
     * @return boolean
     */
    public boolean shouldSkipCookiePathQuotes() {
        //738893 - Skip adding the quotes to the cookie path attribute
        return this.skipCookiePathQuotes;
    }

    /**
     * Query whether or not the HTTP Channel should allow duplicate set-cookies
     *
     *
     * @return boolean
     */
    public boolean doNotAllowDuplicateSetCookies() {
        //PI31734 - Prevent multiple Set-Cookies with the same name
        return this.doNotAllowDuplicateSetCookies;
    }

    /**
     * Query whether or not the HTTP Channel should wait for
     * data to arrive to the TCP layer to determine if the
     * end of message has been parsed.
     *
     * @return boolean
     */
    public boolean shouldWaitForEndOfMessage() {
        return this.waitForEndOfMessage;
    }

    /**
     * Query whether or not the HTTP Channel should send content-length on
     * response messages with status code of 1xx or 204 (RFC7230)
     *
     * @return boolean
     */
    public boolean shouldRemoveCLHeaderInTempStatusRespRFC7230compat() {
        //PI35277
        return this.removeCLHeaderInTempStatusRespRFC7230compat;
    }

    /**
     * Query whether or not the HTTP Channel should prevent response splitting
     *
     * @return boolean
     */
    public boolean shouldPreventResponseSplit() {
        // PI45266
        return this.preventResponseSplit;
    }

    /**
     * Query whether or not the HTTP Channel should attempt to
     * purge the data at the close of the connection
     *
     * @return boolean
     */
    public boolean shouldAttemptPurgeData() {
        return this.attemptPurgeData;
    }

    /**
     * Query whether or not the HTTP Channel should swallow
     * inbound connections IOE
     *
     * @return boolean
     */
    public boolean throwIOEForInboundConnections() {
        //If the httpOption throwIOEForInboundConnections is defined, return that value
        if (this.throwIOEForInboundConnections != null)
            return this.throwIOEForInboundConnections; //PI57542

        //Otherwise, verify if a declarative service has been set to dictate the behavior
        //for this property. If not, return false.
        Boolean IOEForInboundConnectionsBehavior = HttpDispatcher.useIOEForInboundConnectionsBehavior();

        return ((IOEForInboundConnectionsBehavior != null) ? IOEForInboundConnectionsBehavior : Boolean.FALSE);

    }

    public long getH2ConnCloseTimeout() {
        return h2ConnectionCloseTimeout;
    }

    public int getH2ConnReadWindowSize() {
        return h2ConnectionReadWindowSize;
    }

    /**
     * Query whether or not the HTTP Channel should purge remaining response data
     *
     * @return boolean
     */
    public boolean shouldPurgeRemainingResponseBody() {
        // PI81572
        return this.purgeRemainingResponseBody;
    }

    public boolean useForwardingHeaders() {
        return this.useForwardingHeaders;
    }

    public Pattern getForwardedProxiesRegex() {
        if (this.proxiesPattern == null) {
            this.proxiesPattern = Pattern.compile(this.proxiesRegex);
        }

        return this.proxiesPattern;
    }

    /**
     * @return
     */
    public boolean useForwardingHeadersInAccessLog() {
        return (this.useForwardingHeadersInAccessLog && this.useForwardingHeaders);
    }
}
