/*******************************************************************************
 * Copyright (c) 2004, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

import java.security.AccessController;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
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
import com.ibm.ws.http.netty.MSP;
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
    private int maxPersistRequest = -1;
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
    private int limitFieldSize = 32768;
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
    /** PI81572 Purge the remaining response body off the wire when clear is called */
    private boolean purgeRemainingResponseBody = true;

    /** Set as an attribute to the HttpEndpoint **/
    private Boolean useH2ProtocolAttribute = null;
    /** The amount of time the connection will be left open when HTTP/2 goes into an idle state */
    private long http2ConnectionCloseTimeout = 30;
    /** Stream default initial window to the spec max **/
    private int http2SettingsInitialWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE;
    /** Connection default initial window size to the spec max **/
    private int http2ConnectionWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE;
    private int http2ConnectionIdleTimeout = 0;
    private int http2MaxConcurrentStreams = 100;
    private int http2MaxFrameSize = 57344; //Default to 56kb
    /** Don't start sending window update frames until 1/2 the window is used **/
    private boolean http2LimitWindowUpdateFrames = false;
    private int http2MaxResetFrames = 100;
    // reset frames window size in milliseconds
    private int http2ResetFramesWindow = 30000;
    private int http2MaxStreamsRefused = 100;
    private long http2MaxHeaderBlockSize = 512000;
    /** Identifies if the channel has been configured to use X-Forwarded-* and Forwarded headers */
    protected boolean useRemoteIpOptions = false;
    /** Regex to be used to verify that proxies in forwarded headers are known to user */
    private final String proxiesRegex = HttpConfigConstants.DEFAULT_PROXIES_REGEX;
    private Pattern proxiesPattern = null;
    /** Describes the maximum inflation ratio allowed for a request's body when decompressing */
    private int decompressionRatioLimit = 200;
    /** Describes the amount of times a request's body can be decompressed with a ratio above the decompressionRatioLimit before an exception in thrown */
    private int decompressionTolerance = 3;
    /**
     * Set as an attribute to the remoteIP element to specify if X-Forwarded-* and Forwarded header
     * values affect the NCSA Access Log remote directives
     */
    private boolean useForwardingHeadersInAccessLog = false;
    /** Identifies if the channel has been configured to use Auto Compression */
    protected boolean useCompressionOptions = false;
    /** Identifies the preferred compression algorithm */
    private String preferredCompressionAlgorithm = "none";

    protected Set<String> includedCompressionContentTypes = null;
    protected Set<String> excludedCompressionContentTypes = null;
    private final String compressionQValueRegex = HttpConfigConstants.DEFAULT_QVALUE_REGEX;
    private Pattern compressionQValuePattern = null;

    /** Identifies if the channel has been configured to use cookie configuration */
    protected boolean useSameSiteOptions = false;
    /**
     * Sets of cookies configured to be defaulted to have SameSite attribute set to lax, none, or strict. This attribute is added when the cookie has no SameSite attribute defined
     */
    protected Map<String, String> sameSiteCookies = null;
    protected Set<String> sameSiteErrorCookies = null;
    protected Map<String, String> sameSiteStringPatterns = null;
    private Map<Pattern, String> sameSitePatterns = null;
    private boolean onlySameSiteStar = false;
    
    /* Identifies if the partitioned cookie attribute should be set */
    private boolean isPartitioned = false;

    /** Identifies if the channel has been configured to use <headers> configuration */
    protected boolean useHeadersOptions = false;

    /** Maps containing all configured header values to be added in each response */
    protected Map<Integer, List<Map.Entry<String, String>>> configuredHeadersToAdd = null;
    protected Map<Integer, Map.Entry<String, String>> configuredHeadersToSet = null;
    protected Map<Integer, Map.Entry<String, String>> configuredHeadersToSetIfMissing = null;
    /** Tracks header names that will be removed from each response if present */
    protected Map<Integer, String> configuredHeadersToRemove = null;

    /** Tracks headers that have been configured erroneously **/
    protected HashSet<String> configuredHeadersErrorSet = null;

    protected boolean useNetty = Boolean.TRUE;

    /**
     * Constructor for an HTTP channel config object.
     *
     * @param cc
     */
    public HttpChannelConfig(ChannelData cc) {
        parseConfig(cc);
    }

    /**
     * Constructor for an HTTP channel config object using only property bag.
     *
     * @param config
     */

    public HttpChannelConfig(Map<String, Object> config) {
        System.out.println("MSP: properties set");
        parseConfig("default", config);
    }

    /**
     *
     */
    public HttpChannelConfig() {
        // TODO Auto-generated constructor stub
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
     * Update the existing configuration with the input channel property bag
     *
     * @param config
     */
    public void updateConfig(Map<String, Object> config) {
        parseConfig("default", config);
    }

    protected void parseConfig(ChannelData cc) {

        Map<String, Object> propertyBag = new HashMap<>();
        for (Object key : cc.getPropertyBag().keySet()) {
            propertyBag.putIfAbsent(String.valueOf(key), cc.getPropertyBag().get(key));
        }

        parseConfig(cc.getName(), propertyBag);
    }

    /**
     * Parse the configuration data into the separate values.
     *
     * @param cc
     */
    protected void parseConfig(String name, Map<String, Object> config) {
        Tr.entry(tc, "parseConfig: " + name);

        Map<String, Object> propsIn = config;

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
        for (Entry<String, Object> entry : propsIn.entrySet()) {
            key = entry.getKey();
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
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_SETTINGS_INITIAL_WINDOW_SIZE)) {
                props.put(HttpConfigConstants.PROPNAME_H2_SETTINGS_INITIAL_WINDOW_SIZE, value);
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
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_LIMIT_WINDOW_UPDATE_FRAMES)) {
                props.put(HttpConfigConstants.PROPNAME_H2_LIMIT_WINDOW_UPDATE_FRAMES, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_MAX_RESET_FRAMES)) {
                props.put(HttpConfigConstants.PROPNAME_H2_MAX_RESET_FRAMES, value);
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_RESET_FRAMES_WINDOW)) {
                props.put(HttpConfigConstants.PROPNAME_H2_RESET_FRAMES_WINDOW, value);
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_MAX_STREAMS_REFUSED)) {
                props.put(HttpConfigConstants.PROPNAME_H2_MAX_STREAMS_REFUSED, value);
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_MAX_HEADER_BLOCK_SIZE)) {
                props.put(HttpConfigConstants.PROPNAME_H2_MAX_HEADER_BLOCK_SIZE, value);
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PURGE_REMAINING_RESPONSE)) {
                props.put(HttpConfigConstants.PROPNAME_PURGE_REMAINING_RESPONSE, value);
                continue;
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION)) {
                props.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, value);
                continue;
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

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_COMPRESSION)) {
                props.put(HttpConfigConstants.PROPNAME_COMPRESSION, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES)) {
                props.put(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM)) {
                props.put(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM, value);
            }
            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DECOMPRESSION_RATIO_LIMIT)) {
                props.put(HttpConfigConstants.PROPNAME_DECOMPRESSION_RATIO_LIMIT, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DECOMPRESSION_TOLERANCE)) {
                props.put(HttpConfigConstants.PROPNAME_DECOMPRESSION_TOLERANCE, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SAMESITE)) {
                props.put(HttpConfigConstants.PROPNAME_SAMESITE, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SAMESITE_LAX_INTERNAL)) {
                props.put(HttpConfigConstants.PROPNAME_SAMESITE_LAX_INTERNAL, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SAMESITE_NONE_INTERNAL)) {
                props.put(HttpConfigConstants.PROPNAME_SAMESITE_NONE_INTERNAL, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SAMESITE_STRICT_INTERNAL)) {
                props.put(HttpConfigConstants.PROPNAME_SAMESITE_STRICT_INTERNAL, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SAMESITE_PARTITIONED)) {
                props.put(HttpConfigConstants.PROPNAME_SAMESITE_PARTITIONED, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS)) {
                props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD)) {
                props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET)) {
                props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING)) {
                props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING, value);
            }

            if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE)) {
                props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE, value);
            }

            props.put(key, value);
        }

        parseProtocolVersion(props.get(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION));
        parsePersistence(props.get(HttpConfigConstants.PROPNAME_KEEPALIVE_ENABLED), props.get(HttpConfigConstants.PROPNAME_MAX_PERSIST));
        parseOutgoingVersion(props.get(HttpConfigConstants.PROPNAME_OUTGOING_VERSION));
        parseBufferType(props.get(HttpConfigConstants.PROPNAME_DIRECT_BUFF));
        parseOutgoingBufferSize(props.get(HttpConfigConstants.PROPNAME_OUTGOING_HDR_BUFFSIZE));
        parseIncomingHdrBufferSize(props.get(HttpConfigConstants.PROPNAME_INCOMING_HDR_BUFFSIZE));
        parseIncomingBodyBufferSize(props.get(HttpConfigConstants.PROPNAME_INCOMING_BODY_BUFFSIZE));
        parsePersistTimeout(props.get(HttpConfigConstants.PROPNAME_PERSIST_TIMEOUT));
        parseReadTimeout(props.get(HttpConfigConstants.PROPNAME_READ_TIMEOUT));
        parseWriteTimeout(props.get(HttpConfigConstants.PROPNAME_WRITE_TIMEOUT));
        parseByteCacheSize(props.get(HttpConfigConstants.PROPNAME_BYTE_CACHE_SIZE));
        parseDelayedExtract(props.get(HttpConfigConstants.PROPNAME_EXTRACT_VALUE));
        parseBinaryTransport(props.get(HttpConfigConstants.PROPNAME_BINARY_TRANSPORT));
        parseLimitFieldSize(props.get(HttpConfigConstants.PROPNAME_LIMIT_FIELDSIZE));
        parseLimitNumberHeaders(props.get(HttpConfigConstants.PROPNAME_LIMIT_NUMHEADERS));
        parseLimitNumberResponses(props.get(HttpConfigConstants.PROPNAME_LIMIT_NUMBER_RESPONSES));
        parseLimitMessageSize(props.get(HttpConfigConstants.PROPNAME_MSG_SIZE_LIMIT));
        parseAllowRetries(props.get(HttpConfigConstants.PROPNAME_ALLOW_RETRIES));
        parseAccessLog(props.get(HttpConfigConstants.PROPNAME_ACCESSLOG_ID));
        parseHeaderValidation(props.get(HttpConfigConstants.PROPNAME_HEADER_VALIDATION));
        parseStrictURLFormat(props.get(HttpConfigConstants.PROPNAME_STRICT_URL_FORMAT));
        parseServerHeader(props.get(HttpConfigConstants.PROPNAME_SERVER_HEADER_VALUE), props.get(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER));
        parseDateHeaderRange(props.get(HttpConfigConstants.PROPNAME_DATE_HEADER_RANGE));
        parseCookieUpdate(props.get(HttpConfigConstants.PROPNAME_NO_CACHE_COOKIES_CONTROL), props.get(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE));//PI75280
        parseHeaderChangeLimit(props.get(HttpConfigConstants.PROPNAME_HEADER_CHANGE_LIMIT));
        parseAutoDecompression(props.get(HttpConfigConstants.PROPNAME_AUTODECOMPRESSION));
        parseRequestSmugglingProtection(props.get(HttpConfigConstants.PROPNAME_ENABLE_SMUGGLING_PROTECTION));
        parsev0CookieDateRFC1123compat(props.get(HttpConfigConstants.PROPNAME_V0_COOKIE_RFC1123_COMPAT));
        parseDoNotAllowDuplicateSetCookies(props.get(HttpConfigConstants.PROPNAME_DO_NOT_ALLOW_DUPLICATE_SET_COOKIES)); //PI31734
        parseWaitForEndOfMessage(props.get(HttpConfigConstants.PROPNAME_WAIT_FOR_END_OF_MESSAGE)); //PI33453
        parseRemoveCLHeaderInTempStatusRespRFC7230compat(props.get(HttpConfigConstants.REMOVE_CLHEADER_IN_TEMP_STATUS_RFC7230_COMPAT));//PI35277
        parsePreventResponseSplit(props.get(HttpConfigConstants.PROPNAME_PREVENT_RESPONSE_SPLIT)); //PI45266
        parseAttemptPurgeData(props.get(HttpConfigConstants.PROPNAME_PURGE_DATA_DURING_CLOSE)); //PI11176
        parseThrowIOEForInboundConnections(props.get(HttpConfigConstants.PROPNAME_THROW_IOE_FOR_INBOUND_CONNECTIONS)); //PI57542
        parseSkipCookiePathQuotes(props.get(HttpConfigConstants.PROPNAME_SKIP_PATH_QUOTE)); //738893
        parseH2ConnCloseTimeout(props.get(HttpConfigConstants.PROPNAME_H2_CONN_CLOSE_TIMEOUT));
        parseH2ConnectionIdleTimeout(props.get(HttpConfigConstants.PROPNAME_H2_CONNECTION_IDLE_TIMEOUT));
        parseH2MaxConcurrentStreams(props.get(HttpConfigConstants.PROPNAME_H2_MAX_CONCURRENT_STREAMS));
        parseH2MaxFrameSize(props.get(HttpConfigConstants.PROPNAME_H2_MAX_FRAME_SIZE));
        parseH2SettingsInitialWindowSize(props.get(HttpConfigConstants.PROPNAME_H2_SETTINGS_INITIAL_WINDOW_SIZE));
        parseH2ConnectionWindowSize(props.get(HttpConfigConstants.PROPNAME_H2_CONN_WINDOW_SIZE));
        parseH2LimitWindowUpdateFrames(props.get(HttpConfigConstants.PROPNAME_H2_LIMIT_WINDOW_UPDATE_FRAMES));
        parsePurgeRemainingResponseBody(); //PI81572
        parseRemoteIp(props.get(HttpConfigConstants.PROPNAME_REMOTE_IP));
        parseRemoteIpProxies(props.get(HttpConfigConstants.PROPNAME_REMOTE_PROXIES));
        parseRemoteIpAccessLog(props.get(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG));
        parseCompression(props.get(HttpConfigConstants.PROPNAME_COMPRESSION));
        parseCompressionTypes(props.get(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES_INTERNAL));
        parseCompressionPreferredAlgorithm(props.get(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM_INTERNAL));
        parseDecompressionRatioLimit(props.get(HttpConfigConstants.PROPNAME_DECOMPRESSION_RATIO_LIMIT));
        parseDecompressionTolerance(props.get(HttpConfigConstants.PROPNAME_DECOMPRESSION_TOLERANCE));
        parseSameSiteConfig(props.get(HttpConfigConstants.PROPNAME_SAMESITE));
        parseCookiesSameSiteLax(props.get(HttpConfigConstants.PROPNAME_SAMESITE_LAX_INTERNAL));
        parseCookiesSameSiteNone(props.get(HttpConfigConstants.PROPNAME_SAMESITE_NONE_INTERNAL));
        parseCookiesSameSiteStrict(props.get(HttpConfigConstants.PROPNAME_SAMESITE_STRICT_INTERNAL));
        parseH2MaxResetFrames(props);
        parseH2ResetFramesWindow(props);
        parseH2MaxStreamsRefused(props);
        parseH2MaxHeaderBlockSize(props);
        parseCookiesSameSitePartitioned(props);
        initSameSiteCookiesPatterns();
        parseHeaders(props);

        Tr.exit(tc, "parseConfig");

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
    protected void parsePersistence(Object keepAlive, Object maxPersist) {
        parseKeepAliveEnabled(keepAlive);
        if (isKeepAliveEnabled()) {
            parseMaxPersist(maxPersist);
        }
    }

    /**
     * Check the input configuration for the default flag on whether to use
     * persistent connections or not. If this is false, then the other related
     * configuration values will be ignored (such as MaxKeepAliveRequests).
     *
     * @param props
     */
    protected void parseKeepAliveEnabled(Object option) {

        if (Objects.nonNull(option)) {
            this.bKeepAliveEnabled = convertBoolean(option);
            Tr.event(tc, "Config: KeepAliveEnabled is " + isKeepAliveEnabled());
        }
    }

    /**
     * Check the input configuration for the maximum allowed requests per socket
     * setting.
     *
     * @param props
     */
    protected void parseMaxPersist(Object option) {
        // -1 means unlimited
        // 0..1 means 1
        // X means X
        if (Objects.nonNull(option)) {
            try {
                this.maxPersistRequest = minLimit(convertInteger(option), HttpConfigConstants.MIN_PERSIST_REQ);
                Tr.event(tc, "Config: Max persistent requests is " + getMaximumPersistentRequests());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseMaxPersist", "1");
                Tr.event(tc, "Config: Invalid max persistent requests; " + option);

            }
        }
    }

    /**
     * Check the input configuration for the default outgoing HTTP version
     * setting.
     *
     * @param props
     */
    protected void parseOutgoingVersion(Object option) {
        if ("1.0".equals(option)) {
            this.outgoingHttpVersion = VersionValues.V10;
            Tr.event(tc, "Config: Outgoing version is " + getOutgoingVersion().getName());

        }
    }

    /**
     * Check the input configuration for the type of ByteBuffer to use, direct
     * or indirect.
     *
     * @param props
     */
    protected void parseBufferType(Object option) {
        if (Objects.nonNull(option)) {
            this.bDirectBuffers = convertBoolean(option);
            Tr.event(tc, "Config: use direct buffers is " + isDirectBufferType());

        }
    }

    /**
     * Check the input configuration for the maximum buffer size allowed for
     * marshalling headers outbound.
     *
     * @param props
     */
    protected void parseOutgoingBufferSize(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.outgoingHdrBuffSize = rangeLimit(convertInteger(option), HttpConfigConstants.MIN_BUFFER_SIZE, HttpConfigConstants.MAX_BUFFER_SIZE);

                Tr.event(tc, "Config: Outgoing hdr buffer size is " + getOutgoingHdrBufferSize());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseOutgoingBufferSize", "1");

                Tr.event(tc, "Config: Invalid outgoing header buffer size; " + option);

            }
        }
    }

    /**
     * Check the input configuration for the buffer size to use when parsing
     * the incoming headers.
     *
     * @param props
     */
    protected void parseIncomingHdrBufferSize(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.incomingHdrBuffSize = rangeLimit(convertInteger(option), HttpConfigConstants.MIN_BUFFER_SIZE, HttpConfigConstants.MAX_BUFFER_SIZE);

                Tr.event(tc, "Config: Incoming hdr buffer size is " + getIncomingHdrBufferSize());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseIncomingHdrBufferSize", "1");

                Tr.event(tc, "Config: Invalid incoming hdr buffer size of " + option);

            }
        }
    }

    /**
     * Check the input configuration for the buffer size to use when reading
     * the incoming body.
     *
     * @param props
     */
    protected void parseIncomingBodyBufferSize(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.incomingBodyBuffSize = rangeLimit(convertInteger(option), HttpConfigConstants.MIN_BUFFER_SIZE, HttpConfigConstants.MAX_BUFFER_SIZE);

                Tr.event(tc, "Config: Incoming body buffer size is " + getIncomingBodyBufferSize());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseIncomingBodyBufferSize", "1");

                Tr.event(tc, "Config: Invalid incoming body buffer size; " + option);
            }
        }

    }

    /**
     * Check the input configuration for the timeout to use in between
     * persistent requests.
     *
     * @param props
     */
    protected void parsePersistTimeout(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.persistTimeout = TIMEOUT_MODIFIER * minLimit(convertInteger(option), HttpConfigConstants.MIN_TIMEOUT);
                Tr.event(tc, "Config: Persist timeout is " + getPersistTimeout());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parsePersistTimeout", "1");
                Tr.event(tc, "Config: Invalid persist timeout; " + option);

            }
        }
    }

    /**
     * Check the input configuration for the timeout to use when doing any read
     * during a connection.
     *
     * @param props
     */
    protected void parseReadTimeout(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.readTimeout = TIMEOUT_MODIFIER * minLimit(convertInteger(option), HttpConfigConstants.MIN_TIMEOUT);
                Tr.event(tc, "Config: Read timeout is " + getReadTimeout());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseReadTimeout", "1");
                Tr.event(tc, "Config: Invalid read timeout; " + option);

            }
        }
    }

    /**
     * Check the input configuration for the timeout to use when writing data
     * during the connection.
     *
     * @param props
     */
    protected void parseWriteTimeout(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.writeTimeout = TIMEOUT_MODIFIER * minLimit(convertInteger(option), HttpConfigConstants.MIN_TIMEOUT);
                Tr.event(tc, "Config: Write timeout is " + getWriteTimeout());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseWriteTimeout", "1");
                Tr.event(tc, "Config: Invalid write timeout; " + option);

            }
        }
    }

    protected void parseH2ConnectionIdleTimeout(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.http2ConnectionIdleTimeout = TIMEOUT_MODIFIER * minLimit(convertInteger(option), HttpConfigConstants.MIN_TIMEOUT);
                Tr.event(tc, "Config: HTTP/2 Connection idle timeout is " + getH2ConnectionIdleTimeout());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2ConnectionIdleTimeout", "1");
                Tr.event(tc, "Config: Invalid HTTP/2 connection idle timeout; " + option);
            }

        }
    }

    protected void parseH2MaxConcurrentStreams(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.http2MaxConcurrentStreams = convertInteger(option);
                Tr.event(tc, "Config: HTTP/2 Max Concurrent Streams is " + getH2MaxConcurrentStreams());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2MaxConcurrentStreams", "1");
                Tr.event(tc, "Config: Invalid HTTP/2 Max Concurrent Streams; " + option);

            }
        }
    }

    private void parseH2MaxResetFrames(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_H2_MAX_RESET_FRAMES);
        if (null != value) {
            try {
                this.http2MaxResetFrames = convertInteger(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: HTTP/2 Max Reset Frames " + getH2MaxResetFrames());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2MaxResetFrames", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid HTTP/2 Max Reset Frames; " + value);

                }
            }
        }
    }

    private void parseH2ResetFramesWindow(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_H2_RESET_FRAMES_WINDOW);
        if (null != value) {
            try {
                this.http2ResetFramesWindow = convertInteger(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: HTTP/2 Reset Frames Window " + getH2ResetFramesWindow());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2ResetFramesWindow", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid HTTP/2 Reset Frames Window; " + value);

                }
            }
        }
    }

    private void parseH2MaxStreamsRefused(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_H2_MAX_STREAMS_REFUSED);
        if (null != value) {
            try {
                this.http2MaxStreamsRefused = convertInteger(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: HTTP/2 Max Streams Refused " + getH2MaxStreamsRefused());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2MaxStreamsRefused", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid HTTP/2 Max Streams Refused; " + value);

                }
            }
        }
    }

    protected void parseH2MaxHeaderBlockSize(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_H2_MAX_HEADER_BLOCK_SIZE);
        if (null != value) {
            try {
                this.http2MaxHeaderBlockSize = convertLong(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: HTTP/2 Max Header Block Size is " + getH2MaxHeaderBlockSize());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2MaxHeaderBlockSize", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: Invalid HTTP/2 Header Block Size; " + value);

                }
            }
        }
    }

    protected void parseH2MaxFrameSize(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.http2MaxFrameSize = rangeLimit(convertInteger(option), HttpConfigConstants.MIN_LIMIT_FRAME_SIZE, HttpConfigConstants.MAX_LIMIT_FRAME_SIZE);

                Tr.event(tc, "Config: HTTP/2 Max Frame Size is " + getH2MaxFrameSize());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2MaxFrameSize", "1");
                Tr.event(tc, "Config: Invalid HTTP/2 Frame Size; " + option);

            }
        }
    }

    protected void parseH2SettingsInitialWindowSize(Object option) {
        if (Objects.nonNull(option)) {
            this.http2SettingsInitialWindowSize = convertInteger(option);
            Tr.event(tc, "Config: HTTP/2 Settings Initial Window Size is " + getH2SettingsInitialWindowSize());

        }
    }

    protected void parseH2ConnectionWindowSize(Object option) {
        if (Objects.nonNull(option)) {
            this.http2ConnectionWindowSize = convertInteger(option);
            Tr.event(tc, "Config: HTTP/2 Connection Window Size is " + getH2ConnectionWindowSize());

        }
    }

    protected void parseH2LimitWindowUpdateFrames(Object option) {
        if (Objects.nonNull(option)) {
            this.http2LimitWindowUpdateFrames = convertBoolean(option);
            Tr.event(tc, "Config: HTTP/2 Limit Window Update Frames is " + getH2LimitWindowUpdateFrames());

        }

    }

    protected void parseH2ConnCloseTimeout(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.http2ConnectionCloseTimeout = convertLong(option);
                Tr.event(tc, "Config: H2 Connection Close timeout is " + getH2ConnCloseTimeout());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseH2ConnCloseTimeout", "1");
                Tr.event(tc, "Config: Invalid H2 Connection Close Timeout of " + option);

            }
        }
    }

    /**
     * Check the input configuration for the size of the parse byte cache to use.
     *
     * @param props
     */
    protected void parseByteCacheSize(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.byteCacheSize = rangeLimit(convertInteger(option), HttpConfigConstants.MIN_BYTE_CACHE_SIZE, HttpConfigConstants.MAX_BYTE_CACHE_SIZE);

                Tr.event(tc, "Config: byte cache size is " + getByteCacheSize());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseByteCacheSize", "1");

                Tr.event(tc, "Config: Invalid bytecache setting of " + option);

            }
        }
    }

    /**
     * Check the input configuration for the flag on whether to immediately
     * extract header values during the parsing stage or not.
     *
     * @param props
     */
    protected void parseDelayedExtract(Object option) {
        if (Objects.nonNull(option)) {
            this.bExtractValue = convertBoolean(option);
            Tr.event(tc, "Config: header value extraction is " + shouldExtractValue());

        }
    }

    /**
     * Check the input configuration for whether the parsing and marshalling
     * should use the binary transport mode or not.
     *
     * @param props
     */
    protected void parseBinaryTransport(Object option) {
        if (Objects.nonNull(option)) {
            this.bBinaryTransport = convertBoolean(option);
            Tr.event(tc, "Config: binary transport is " + isBinaryTransportEnabled());
        }
    }

    /**
     * Check the input configuration for a maximum size allowed for HTTP fields.
     *
     * @param props
     */
    protected void parseLimitFieldSize(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.limitFieldSize = minLimit(convertInteger(value), HttpConfigConstants.MIN_LIMIT_FIELDSIZE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: field size limit is " + getLimitOfFieldSize());
                }
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseLimitFieldSize", "1");
                Tr.event(tc, "Config: Invaild max field size setting of " + option);

            }
        }
    }

    /**
     * Check the input configuration for a maximum limit on the number of
     * headers allowed per message.
     *
     * @param props
     */
    protected void parseLimitNumberHeaders(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.limitNumHeaders = rangeLimit(convertInteger(option), HttpConfigConstants.MIN_LIMIT_NUMHEADERS, HttpConfigConstants.MAX_LIMIT_NUMHEADERS);
                Tr.event(tc, "Config: Num hdrs limit is " + getLimitOnNumberOfHeaders());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseLimitNumberHeaders", "1");
                Tr.event(tc, "Config: Invalid number of headers limit; " + option);

            }
        }
    }

    /**
     * Check the input configuration for a maximum limit on the number of
     * temporary responses that will be read and skipped past.
     *
     * @param props
     */
    protected void parseLimitNumberResponses(Object option) {
        if (Objects.nonNull(option)) {
            try {
                int size = convertInteger(option);
                if (HttpConfigConstants.UNLIMITED == size) {
                    this.limitNumResponses = HttpConfigConstants.MAX_LIMIT_NUMRESPONSES;
                } else {
                    this.limitNumResponses = rangeLimit(size, 1, HttpConfigConstants.MAX_LIMIT_NUMRESPONSES);
                }
                Tr.event(tc, "Config: Num responses limit is " + getLimitOnNumberOfResponses());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseLimitNumberResponses", "1");
                Tr.event(tc, "Config: Invalid max number of responses; " + option);

            }
        }
    }

    /**
     * Parse the possible configuration limit on the incoming message body.
     * size.
     *
     * @param props
     */
    protected void parseLimitMessageSize(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.limitMessageSize = convertLong(option);
                Tr.event(tc, "Config: Message size limit is " + getMessageSizeLimit());
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseLimitMessageSize", "1");
                Tr.event(tc, "Config: Invalid message size limit; " + option);
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
    protected void parseAccessLog(Object option) {

        if (Objects.nonNull(option)) {
            String id = String.valueOf(option);
            AtomicReference<AccessLog> aLog = HttpEndpointImpl.getAccessLogger(id);
            if (aLog != null) {
                this.accessLogger = aLog;
            }
            MSP.log("access log format is set to: " + accessLogger.get().getFormat());
            Tr.debug(tc, "Config: using logging service", accessLogger);
        }
    }

    /**
     * Check the configuration to see if the samesite config element has been configured
     * to consider cookie specific attributes.
     *
     * @param props
     */
    protected void parseSameSiteConfig(Object option) {
        if (Objects.nonNull(option)) {
            this.useSameSiteOptions = convertBoolean(option);

            if (this.useSameSiteOptions) {
                this.sameSiteCookies = new HashMap<String, String>();
                this.sameSiteErrorCookies = new HashSet<String>();
                this.sameSiteStringPatterns = new HashMap<String, String>();

                Tr.event(tc, "Http Channel Config: SameSite configuration has been enabled");

            }
        }

    }

    /**
     * Parse the configuration to map all cookies configured to have the SameSite=Lax attribute
     * added to them.
     *
     * @param option
     */
    protected void parseCookiesSameSiteLax(Object option) {
        if (Objects.nonNull(option) && this.useSameSiteOptions) {

            if (option instanceof String[]) {
                String[] cookies = (String[]) option;
                for (String s : cookies) {

                    addSameSiteAttribute(s, HttpConfigConstants.SameSite.LAX);

                }
            }
            Tr.event(tc, "Http Channel Config: SameSite Lax configuration parsed.");

        }

    }

    protected void parseCookiesSameSiteNone(Object option) {
        if (Objects.nonNull(option) && this.useSameSiteOptions) {

            if (option instanceof String[]) {
                String[] cookies = (String[]) option;
                for (String s : cookies) {

                    addSameSiteAttribute(s, HttpConfigConstants.SameSite.NONE);

                }
                Tr.event(tc, "Http Channel Config: SameSite None configuration parsed.");
            }

        }
    }

    protected void parseCookiesSameSiteStrict(Object option) {
        if (Objects.nonNull(option) && this.useSameSiteOptions) {

            if (option instanceof String[]) {
                String[] cookies = (String[]) option;
                for (String s : cookies) {

                    addSameSiteAttribute(s, HttpConfigConstants.SameSite.STRICT);

                }
                Tr.event(tc, "Http Channel Config: SameSite Strict configuration parsed.");
            }
        }
    }

    private void parseCookiesSameSitePartitioned(Map<Object, Object> props) {
        Object value = props.get(HttpConfigConstants.PROPNAME_SAMESITE_PARTITIONED);
        if (null != value && this.useSameSiteConfig) {

            if (value instanceof Boolean) {
                Boolean partitionedValue= (Boolean) value;
                if(partitionedValue){
                    this.isPartitioned = true;
                }
            }
            if (this.useSameSiteConfig && (TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
                Tr.event(tc, "Http Channel Config: SameSite Partitioned configuration parsed.");
            }
        }
    }


    private void addSameSiteAttribute(String name, HttpConfigConstants.SameSite sameSiteAttribute) {
        if (this.sameSiteErrorCookies.contains(name)) {
            Tr.warning(tc, "cookies.samesite.knownDuplicateName", name, sameSiteAttribute.getName().toLowerCase());
        }

        //If this cookie name has already been added to the error list, do not attempt to
        //add it. Otherwise, check each set to confirm its uniqueness. If not unique,
        //remove it from the list, warn the user, and set the cookie as erroneous. Otherwise,
        //store the cookie under the respective list.
        if (!sameSiteErrorCookies.contains(name)) {

            //Wildcard support is only supported for patterns ending on the * character. There cannot
            //be more than one * character in the string.
            if (name.endsWith(HttpConfigConstants.WILDCARD_CHAR) && name.indexOf(HttpConfigConstants.WILDCARD_CHAR) == name.lastIndexOf(HttpConfigConstants.WILDCARD_CHAR)) {
                //Check that it isn't already defined with a different SameSite value
                if (this.sameSiteStringPatterns.containsKey(name) && !this.sameSiteStringPatterns.get(name).equals(sameSiteAttribute.getName())) {
                    this.sameSiteStringPatterns.remove(name);
                    Tr.warning(tc, "cookies.samesite.duplicateName", name, sameSiteAttribute.getName().toLowerCase());
                    this.sameSiteErrorCookies.add(name);
                } else {
                    // If this is not a duplicate with the same value then add it, otherwise ignore the duplicate.
                    if (!this.sameSiteStringPatterns.containsKey(name)) {
                        this.sameSiteStringPatterns.put(name, sameSiteAttribute.getName());
                    } else {
                        Tr.event(tc, "The duplicate pattern: " + name + " was not added again to the: " + sameSiteAttribute.getName() + " list.");
                    }
                }
            }
            //If the wildcard character is used in any other context, report the error
            else if (name.contains(HttpConfigConstants.WILDCARD_CHAR)) {
                Tr.warning(tc, "cookies.samesite.unsupportedWildcard", name);
                this.sameSiteErrorCookies.add(name);
            }
            //This is an explicitly named cookie
            else {
                /*
                 * Check that the cookie isn't already defined with a different value. If the cookieName exists
                 * in the sameSiteCookies map but has the same sameSiteAttribute value specified here then just
                 * ignore since this is just a duplicate in the configuration. There is no ambiguity here since the values are the same.
                 */
                if (this.sameSiteCookies.containsKey(name) && !this.sameSiteCookies.get(name).equals(sameSiteAttribute.getName())) {
                    this.sameSiteCookies.remove(name);
                    Tr.warning(tc, "cookies.samesite.duplicateName", name, sameSiteAttribute.getName().toLowerCase());
                    this.sameSiteErrorCookies.add(name);
                } else {
                    // If this is not a duplicate with the same value then add it, otherwise ignore the duplicate.
                    if (!this.sameSiteCookies.containsKey(name)) {
                        this.sameSiteCookies.put(name, sameSiteAttribute.getName());
                    } else {
                        Tr.event(tc, "The duplicate cookieName: " + name + " was not added again to the: " + sameSiteAttribute.getName() + " list.");

                    }
                }
            }
        }
    }

    /**
     * If the Http Endpoint is configured to use the <samesite> child element, this method will compile all the
     * wildcard regex patterns that were provided thru the samesite lax, none, and strict attributes. The patterns
     * are loaded into a sorted linked hash map that will contain range from most specific to most broad. Both the
     * explicit name's Map and the pattern's Map are iterated to print out a visual representation of all names
     * registered as 'lax', 'none', and 'strict'; as well as a representation of all values that were considered
     * erroneous.
     */
    private void initSameSiteCookiesPatterns() {
        if (this.useSameSiteConfig()) {
            Map<Pattern, String> patterns = new HashMap<Pattern, String>();
            Pattern p = null;
            String sameSiteValue = null;

            if (this.sameSiteStringPatterns.size() == 1 && this.sameSiteStringPatterns.containsKey(HttpConfigConstants.WILDCARD_CHAR)) {
                this.onlySameSiteStar = true;
                this.sameSiteCookies.put(HttpConfigConstants.WILDCARD_CHAR, this.sameSiteStringPatterns.get(HttpConfigConstants.WILDCARD_CHAR));
            }
            if (!this.onlySameSiteStar) {

                for (String s : this.sameSiteStringPatterns.keySet()) {
                    sameSiteValue = this.sameSiteStringPatterns.get(s);
                    s = s.replace(HttpConfigConstants.WILDCARD_CHAR, ".*");
                    p = Pattern.compile(s);
                    patterns.put(p, sameSiteValue);
                }

                List<Map.Entry<Pattern, String>> list = new LinkedList<Map.Entry<Pattern, String>>(patterns.entrySet());
                //Sort by alphabetical ordering
                Collections.sort(list, new Comparator<Map.Entry<Pattern, String>>() {
                    @Override
                    public int compare(Map.Entry<Pattern, String> pattern1, Map.Entry<Pattern, String> pattern2) {
                        return pattern1.getKey().toString().compareTo(pattern2.toString());
                    }
                });
                //Now order from most specific pattern to most general. If a pattern's string representation matches a second pattern,
                //the former is considered to be more specific (a subset of the latter).
                Collections.sort(list, new Comparator<Map.Entry<Pattern, String>>() {
                    Pattern pat = null;
                    Matcher mat = null;

                    @Override
                    public int compare(Map.Entry<Pattern, String> pattern1, Map.Entry<Pattern, String> pattern2) {
                        pat = pattern1.getKey();
                        mat = pat.matcher(pattern2.getKey().toString());
                        return mat.matches() ? 1 : -1;
                    }
                });
                //Take the sorted list and create a linked hash map to preserve ordering
                this.sameSitePatterns = new LinkedHashMap<Pattern, String>();
                for (Map.Entry<Pattern, String> entry : list) {
                    this.sameSitePatterns.put(entry.getKey(), entry.getValue());
                }
            }

            //If tracing is enabled, print out the state of these maps.
            Set<String> laxCookies = new HashSet<String>();
            Set<String> noneCookies = new HashSet<String>();
            Set<String> strictCookies = new HashSet<String>();

            StringBuilder sb = new StringBuilder();
            sb.append("Http Channel Config: SameSite configuration complete. The following values are set:\n");
            for (String key : this.sameSiteCookies.keySet()) {
                if (HttpConfigConstants.SameSite.LAX.getName().equalsIgnoreCase(this.sameSiteCookies.get(key))) {
                    laxCookies.add(key);
                }
                if (HttpConfigConstants.SameSite.NONE.getName().equalsIgnoreCase(this.sameSiteCookies.get(key))) {
                    noneCookies.add(key);
                }
                if (HttpConfigConstants.SameSite.STRICT.getName().equalsIgnoreCase(this.sameSiteCookies.get(key))) {
                    strictCookies.add(key);
                }
            }
            //This is only defined when there are patterns, ensure the list has been initialized
            if (this.sameSitePatterns != null) {
                for (Pattern key : this.sameSitePatterns.keySet()) {

                    if (HttpConfigConstants.SameSite.LAX.getName().equalsIgnoreCase(this.sameSitePatterns.get(key))) {
                        laxCookies.add(key.toString());
                    }
                    if (HttpConfigConstants.SameSite.NONE.getName().equalsIgnoreCase(this.sameSitePatterns.get(key))) {
                        noneCookies.add(key.toString());
                    }
                    if (HttpConfigConstants.SameSite.STRICT.getName().equalsIgnoreCase(this.sameSitePatterns.get(key))) {
                        strictCookies.add(key.toString());
                    }
                }
            }

            //Construct the lax names
            sb.append("SameSite Lax Cookies ").append(laxCookies).append("\n");
            sb.append("SameSite None Cookies ").append(noneCookies).append("\n");
            sb.append("SameSite Strict Cookies ").append(strictCookies);
            if (!this.sameSiteErrorCookies.isEmpty()) {
                sb.append("\n").append("Misconfigured SameSite cookies ").append(this.sameSiteErrorCookies);
            }
            Tr.event(tc, sb.toString());

        }
    }

    /**
     * Check the configuration to see if the autoCompression element has been configured
     * to consider Accept-Encoding header values to determine whether to compress the
     * response body.
     *
     * @param props
     */
    protected void parseCompression(Object option) {

        if (Objects.nonNull(option)) {
            this.useCompressionOptions = convertBoolean(option);

            if (this.useCompressionOptions) {
                this.includedCompressionContentTypes = new HashSet<String>();
                this.includedCompressionContentTypes.add("text/*");
                this.includedCompressionContentTypes.add("application/javascript");
                this.excludedCompressionContentTypes = new HashSet<String>();

                Tr.event(tc, "Http Channel Config: compression has been enabled");
            }
        }
    }

    /**
     * Check the configuration to see if the compression element has been configured
     * to modify the list of content types to be considered for compression.
     *
     * @param props
     */
    protected void parseCompressionTypes(Object option) {
        if (Objects.nonNull(option) && this.useCompressionOptions) {

            HashSet<String> configuredCompressionTypes = new HashSet<String>();
            HashSet<String> addCompressionConfig = new HashSet<String>();
            HashSet<String> removeCompressionConfig = new HashSet<String>();
            StringBuilder sb = new StringBuilder();
            boolean hasConfigError = Boolean.FALSE;

            //Build the string representation of the default configuration values for autocompression filter types
            for (String s : this.includedCompressionContentTypes) {
                if (sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append(s);
            }
            String defaultConfiguration = sb.toString();

            if (option instanceof String[]) {
                String[] filterTypes = (String[]) option;
                for (String s : filterTypes) {
                    s = s.trim().toLowerCase(Locale.ENGLISH);

                    //All filters added through the add option (+) will start with the add (+) character.
                    //Put all of these into the addCompressionConfig set.
                    if (s.indexOf("+") == 0) {
                        s = s.replaceFirst("\\+", "");
                        //If this filter already exists in the addCompressionConfig set, mark it as a
                        //duplicate and enable the error flag.
                        if (!addCompressionConfig.add(s)) {

                            Tr.warning(tc, "compression.duplicateType", s, defaultConfiguration);

                            hasConfigError = true;
                            break;
                        }

                    }
                    //All filters added through the remove option (-) will start with the remove (-) character.
                    //Put all of these into the removeCompressionConfig set.
                    else if (s.indexOf("-") == 0) {
                        s = s.replaceFirst("-", "");
                        //If this filter already exists in the removeCompressionConfig set, mark it as a
                        //duplicate and enable the error flag.
                        if (!removeCompressionConfig.add(s)) {

                            Tr.warning(tc, "compression.duplicateTypeRemoval", s, defaultConfiguration);

                            hasConfigError = true;
                            break;
                        }
                    } else {
                        //All filters added without the add or remove characters (+/-) will end up overwriting the
                        //default values. Ensure no duplicates are added to this list.
                        if (!configuredCompressionTypes.add(s)) {
                            Tr.warning(tc, "compression.duplicateType", s, defaultConfiguration);
                            hasConfigError = true;
                            break;
                        }
                    }
                }
            }

            if (!addCompressionConfig.isEmpty() && !hasConfigError) {

                //If the add function is being used, the default configuration should not be configured to be
                //overwritten. If configuredCompressionTypes is not empty, it is a bad configuration
                if (!configuredCompressionTypes.isEmpty()) {
                    Tr.warning(tc, "compression.duplicateOverwriteAndAdd", defaultConfiguration);
                    hasConfigError = true;
                }

                //If no error up to this point, ensure that the values being added are not part of
                //the default includeCompressionFilterTypes set or not being excluded by the
                //removeCompressionConfig set
                if (!hasConfigError) {

                    for (String s : addCompressionConfig) {
                        //there isn't a configured compression types set at this point, check that the
                        //user doesn't try to add a value already in the default filter
                        if (this.includedCompressionContentTypes.contains(s)) {
                            Tr.warning(tc, "compression.duplicateType", s, defaultConfiguration);
                            hasConfigError = true;
                            break;
                        }

                        //check for any duplicate between add '+' and remove '-' sets, not allowed
                        if (removeCompressionConfig.contains(s)) {
                            Tr.warning(tc, "compression.duplicateTypeAddRemove", s, defaultConfiguration);
                            hasConfigError = true;
                            break;
                        }
                    }
                }
            }

            if (!hasConfigError) {
                //process sets
                if (!configuredCompressionTypes.isEmpty()) {
                    this.includedCompressionContentTypes = configuredCompressionTypes;
                }
                if (!addCompressionConfig.isEmpty()) {
                    for (String s : addCompressionConfig) {
                        this.includedCompressionContentTypes.add(s);
                    }
                }
                if (!removeCompressionConfig.isEmpty()) {
                    this.excludedCompressionContentTypes = removeCompressionConfig;
                }

            }

            Tr.event(tc, "Compression Config", "compressionContentTypes updated: " +
                                               !hasConfigError);
            if (!hasConfigError) {
                for (String s : this.includedCompressionContentTypes) {
                    Tr.event(tc, "Include list of content-types: " + s);
                }
                for (String s : this.excludedCompressionContentTypes) {
                    Tr.event(tc, "Exclude list of content-types: " + s);
                }
            }

        }

    }

    /**
     * Check the configuration to see if the compression element has been configured
     * to modify the server's preferred compression algorithm.
     *
     * @param props
     */
    protected void parseCompressionPreferredAlgorithm(Object option) {
        if (Objects.nonNull(option) && this.useCompressionOptions) {
            String value = String.valueOf(option).toLowerCase(Locale.ENGLISH);
            boolean isSupportedConfiguration = Boolean.TRUE;

            //Validate parameter, if not supported default to none.
            switch (value) {
                case ("gzip"):
                    break;
                case ("deflate"):
                    break;
                case ("x-gzip"):
                    break;
                case ("identity"):
                    break;
                case ("zlib"):
                    break;
                case ("none"):
                    break;
                default:
                    Tr.warning(tc, "compression.unsupportedAlgorithm", value, preferredCompressionAlgorithm);
                    isSupportedConfiguration = Boolean.FALSE;
                    break;

            }

            if (isSupportedConfiguration) {
                this.preferredCompressionAlgorithm = value;
            }

            Tr.event(tc, "Compression Config", "preferred compression algorithm set to: " + this.preferredCompressionAlgorithm);

        }
    }

    /**
     * Check the configuration to see if the remoteIp element has been configured
     * to consider forwarding header values in the NCSA Access Log
     *
     * @param props
     */
    protected void parseRemoteIp(Object option) {
        if (Objects.nonNull(option)) {

            this.useRemoteIpOptions = convertBoolean(option);

            if (this.useRemoteIpOptions) {
                Tr.event(tc, "HTTP Channel Config: remoteIp has been enabled");
            }
        }

    }

    /**
     * @param object
     */
    protected void parseRemoteIpProxies(Object option) {

        if (Objects.nonNull(option) && this.useRemoteIpOptions) {
            String value = String.valueOf(option);

            Tr.event(tc, "RemoteIp Config: proxies regex set to: " + value);

            this.proxiesPattern = Pattern.compile(this.proxiesRegex);
        }
    }

    protected void parseRemoteIpAccessLog(Object option) {

        MSP.log("parseRemoteIpAccessLog: " + Objects.nonNull(option) + " useForwarding: " + this.useRemoteIpOptions);

        if (Objects.nonNull(option) && this.useRemoteIpOptions) {
            this.useForwardingHeadersInAccessLog = convertBoolean(option);

            Tr.event(tc, "RemoteIp Config: useRemoteIpInAccessLog set to: " + useForwardingHeadersInAccessLog);
            MSP.log("RemoteIp Config: useRemoteIpInAccessLog set to: " + useForwardingHeadersInAccessLog);
        }
    }

    /**
     * Check the configuration to see if the headers element has been configured
     * to consider response header values to be added to each response.
     *
     * @param props
     */
    protected void parseHeaders(Map<Object, Object> options) {

        if (Objects.nonNull(options)) {
            this.useHeadersOptions = convertBoolean(options.get(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS));

            if (this.useHeadersOptions) {
                this.configuredHeadersToAdd = new HashMap<Integer, List<Map.Entry<String, String>>>();
                this.configuredHeadersToSet = new HashMap<Integer, Map.Entry<String, String>>();
                this.configuredHeadersToSetIfMissing = new HashMap<Integer, Map.Entry<String, String>>();
                this.configuredHeadersToRemove = new HashMap<Integer, String>();
                this.configuredHeadersErrorSet = new HashSet<String>();

                Tr.event(tc, "Http Channel Config: <headers> config has been enabled");

                parseHeadersToRemove(options.get(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE_INTERNAL));
                parseHeadersToAdd(options.get(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD_INTERNAL));
                parseHeadersToSet(options.get(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_INTERNAL));
                parseHeadersToSetIfMissing(options.get(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING_INTERNAL));
                logHeadersConfig();
            }
        }
    }

    /**
     * Parse the configuration to map all defined response headers to be removed from responses if
     * present.
     *
     * @param props
     */
    protected void parseHeadersToRemove(Object option) {
        if (Objects.nonNull(option) && this.useHeadersOptions) {

            if (option instanceof String[]) {
                String[] headers = (String[]) option;
                //Parse all headers
                for (String headerName : headers) {
                    if (headerName.isEmpty()) {
                        Tr.warning(tc, "headers.emptyName", "remove");
                    } else {

                        int hashcode = headerName.trim().toLowerCase().hashCode();
                        if (!this.configuredHeadersToRemove.containsKey(hashcode)) {
                            this.configuredHeadersToRemove.put(hashcode, headerName);
                            Tr.event(tc, "Headers remove configuration: parsed name [" + headerName + "]");

                        }
                    }
                }
            }
            Tr.event(tc, "Http Headers Config: <headers> remove configuration finished parsing.");

        }
    }

    /**
     * Parse the configuration to map all defined response headers to be added with the appendHeader
     * method during responses.
     *
     * @param props
     */
    protected void parseHeadersToAdd(Object option) {
        if (Objects.nonNull(option) && this.useHeadersOptions) {

            if (option instanceof String[]) {
                String[] headers = (String[]) option;

                //Parse all headers as a key value pair and add them to the map
                for (String headerEntry : headers) {
                    this.setHeaderToCollection(headerEntry, HttpConfigConstants.Headers.ADD);
                }
            }
            Tr.event(tc, "Http Headers Config: <headers> add configuration finished parsing.");

        }

    }

    /**
     * Parse the configuration to map all defined headers to be set on responses using the setHeader
     * method. This will overwrite the header value if already set on the response.
     *
     * @param props
     */
    protected void parseHeadersToSet(Object option) {
        if (Objects.nonNull(option) && this.useHeadersOptions) {

            if (option instanceof String[]) {
                String[] headers = (String[]) option;
                //Parse all headers as a key value pair and add them to the map
                for (String headerEntry : headers) {

                    this.setHeaderToCollection(headerEntry, HttpConfigConstants.Headers.SET);
                }
                Tr.event(tc, "Http Headers Config: <headers> set configuration finished parsing.");

            }
        }
    }

    /**
     * Parse the configuration to map all defined headers to be set on responses using the setHeader
     * method. This will only be done if the header is not already present in the response.
     *
     * @param props
     */
    protected void parseHeadersToSetIfMissing(Object option) {
        if (Objects.nonNull(option) && this.useHeadersOptions) {

            if (option instanceof String[]) {
                String[] headers = (String[]) option;
                //Parse all headers as a key value pair and add them to the map
                for (String headerEntry : headers) {

                    this.setHeaderToCollection(headerEntry, HttpConfigConstants.Headers.SET_IF_MISSING);
                }
                Tr.event(tc, "Http Headers Config: <headers> setIfMissing configuration finished parsing.");

            }
        }
    }

    protected void logHeadersConfig() {

        //If tracing is enabled, print out the state of these maps.
        if (this.useHeadersOptions) {

            List<String> addHeaders = new LinkedList<String>();

            StringBuilder sb = new StringBuilder();
            sb.append("Http Channel Config: Headers configuration complete. The following values are set:\n");
            for (List<Entry<String, String>> headerList : this.configuredHeadersToAdd.values()) {
                for (Entry<String, String> header : headerList) {
                    addHeaders.add(header.getKey() + ":" + header.getValue());
                }
            }

            //Construct the lax names
            sb.append("Headers Remove ").append(this.configuredHeadersToRemove.values()).append("\n");
            sb.append("Headers Add ").append(addHeaders).append("\n");
            sb.append("Headers Set ").append(this.configuredHeadersToSet.values()).append("\n");
            sb.append("Headers SetIfMissing ").append(this.configuredHeadersToSetIfMissing.values());
            if (!this.configuredHeadersErrorSet.isEmpty()) {
                sb.append("\n").append("Misconfigured headers ").append(this.configuredHeadersErrorSet);
            }
            Tr.event(tc, sb.toString());
        }
    }

    private void setHeaderToCollection(String header, HttpConfigConstants.Headers collectionType) {

        int delimiterIndex = -1;
        String headerName, headerValue = null;

        //Find the first occurrence of the delimiter and obtain
        //the name and value from the configured header
        delimiterIndex = header.indexOf(":");
        if (delimiterIndex == -1) {
            headerName = header.trim();
            headerValue = "";
        } else {
            headerName = header.substring(0, delimiterIndex).trim();
            headerValue = header.substring(delimiterIndex + 1).trim();
        }

        if (headerName.isEmpty()) {
            Tr.warning(tc, "headers.emptyName", collectionType.getName());

        } else {
            //No configuration error so far, check that no other list defines this, as
            //that would create ambiguity. If found elsewhere, warn the user and take it
            //out of all lists.
            String normalizedHeaderName = headerName.trim().toLowerCase();
            int headerNameHashCode = normalizedHeaderName.hashCode();

            if (this.configuredHeadersErrorSet.contains(normalizedHeaderName)) {

                Tr.warning(tc, "headers.knownDuplicateHeader", header, collectionType.getName());
            }
            //If this header has already been parsed on the 'remove', 'add', 'overwrite' or 'setIfMissing' collections, then this is an
            //erroneous configuration. Remove from all collections and put on the error set. Log a configuration warning message.
            //The only exception to this is on the 'add' list, where multiple headers with the same name can be appended. So if the
            //header is not unique AND we are entering this logic from the Headers.ADD enum, allow it.
            else if (this.configuredHeadersToRemove.containsKey(headerNameHashCode) ||
                     (this.configuredHeadersToAdd.containsKey(headerNameHashCode) && (collectionType != HttpConfigConstants.Headers.ADD)) ||
                     this.configuredHeadersToSet.containsKey(headerNameHashCode) ||
                     this.configuredHeadersToSetIfMissing.containsKey(headerNameHashCode)) {

                this.configuredHeadersToRemove.remove(headerNameHashCode);
                this.configuredHeadersToAdd.remove(headerNameHashCode);
                this.configuredHeadersToSet.remove(headerNameHashCode);
                this.configuredHeadersToSetIfMissing.remove(headerNameHashCode);

                this.configuredHeadersErrorSet.add(normalizedHeaderName);
                Tr.warning(tc, "headers.duplicateHeaderName", header, collectionType.getName());

            }

            else {
                //The ADD configuration can have multiple header names with the same name, so
                //add all occurrences into lists for the ADD collection
                if (collectionType == HttpConfigConstants.Headers.ADD) {
                    if (!this.configuredHeadersToAdd.containsKey(headerNameHashCode)) {
                        this.configuredHeadersToAdd.put(headerNameHashCode, new LinkedList<Map.Entry<String, String>>());
                    }
                    this.configuredHeadersToAdd.get(headerNameHashCode).add(new AbstractMap.SimpleEntry<String, String>(headerName, headerValue));
                }

                else if (collectionType == HttpConfigConstants.Headers.SET) {

                    this.configuredHeadersToSet.put(headerNameHashCode, new AbstractMap.SimpleEntry<String, String>(headerName, headerValue));

                }

                else if (collectionType == HttpConfigConstants.Headers.SET_IF_MISSING) {
                    this.configuredHeadersToSetIfMissing.put(headerNameHashCode, new AbstractMap.SimpleEntry<String, String>(headerName, headerValue));
                }

                Tr.event(tc, "Header " + collectionType.getName() + " configuration: parsed name [" + headerName + "] and value [" + headerValue + "]");

            }

        }
    }

    /**
     * Parse the input configuration for the flag on whether to allow retries
     * or not.
     *
     * @param props
     */
    protected void parseAllowRetries(Object option) {
        if (Objects.nonNull(option)) {
            this.bAllowRetries = convertBoolean(option);
            Tr.event(tc, "Config: allow retries is " + allowsRetries());

        }
    }

    /**
     * Parse the configuration on whether to perform header validation or not.
     *
     * @param props
     */
    protected void parseHeaderValidation(Object option) {
        if (Objects.nonNull(option)) {
            this.bHeaderValidation = convertBoolean(option);
            Tr.event(tc, "Config: header validation is " + isHeaderValidationEnabled());

        }
    }

    /**
     * Parse the configuration on whether to perform JIT allocate only reads
     * or leave it to the default behavior.
     *
     * @param props
     */
    protected void parseJITOnlyReads(Object option) {
        if (Objects.nonNull(option)) {
            this.bJITOnlyReads = convertBoolean(option);
            Tr.event(tc, "Config: JIT only reads is " + isJITOnlyReads());

        }
    }

    /**
     * Check the input configuration to decide whether to enforce a strict RFC
     * compliance while parsing URLs.
     *
     * @param props
     */
    protected void parseStrictURLFormat(Object option) {
        if (Objects.nonNull(option)) {
            this.bStrictURLFormat = convertBoolean(option);
            Tr.event(tc, "Config: Strict URL formatting is " + isStrictURLFormat());

        }
    }

    /**
     * Check the input configuration map for the parameters that control the
     * Server header value.
     *
     * @param props
     */
    protected void parseServerHeader(Object optionServerHeader, Object optionServerHeaderValue) {
        // @PK15848
        String option = Objects.nonNull(optionServerHeaderValue) ? String.valueOf(optionServerHeaderValue) : null;
        if (Objects.isNull(option) || option.isEmpty()) {
            // due to security change, do not default value in Server header. // PM87013 Start
        } else {
            if ("DefaultServerVersion".equalsIgnoreCase(option)) {
                option = "WebSphere Application Server";
            }
            this.baServerHeaderValue = GenericUtils.getEnglishBytes(option);
            Tr.event(tc, "Config: server header value [" + option + "]");

        }
        // PM87013 (PM75371) End
        if (Objects.nonNull(optionServerHeader)) {
            this.bRemoveServerHeader = convertBoolean(optionServerHeader);
            Tr.event(tc, "Config: remove server header is " + removeServerHeader());

        }
    }

    /**
     * Parse the date header range value from the input properties.
     *
     * @param props
     */
    protected void parseDateHeaderRange(Object option) {
        // @313642
        if (Objects.nonNull(option)) {
            try {
                this.lDateHeaderRange = minLimit(convertLong(option), 0L);
                Tr.event(tc, "Config: date header range is " + option);

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseDateHeaderRange", "1");
                Tr.event(tc, "Config: Invalid date header range; " + option);

            }
        }
    }

    /**
     * Check the configuration map for the Set-Cookie updating no-cache value.
     *
     * @param props
     */
    protected void parseCookieUpdate(Object option, Object option2) {
        //This property needed to be documented using a new name because
        //the original property contains a banned word for metatype: 'config'
        //This change will verify if either (or both) original/documented properties
        //are set. The instance variable they reference will be set to false if
        //either property is set to false.

        boolean documentedProperty = Boolean.TRUE;
        boolean originalProperty = Boolean.TRUE;

        if (Objects.nonNull(option)) {
            documentedProperty = convertBoolean(option);
            Tr.event(tc, "Config: set no-cache cookie control is " + documentedProperty);

        }

        if (Objects.nonNull(option2)) {
            originalProperty = convertBoolean(option2);
            Tr.event(tc, "Config: set-cookie configures no-cache is " + originalProperty);

        }
        this.bCookiesConfigureNoCache = originalProperty && documentedProperty;
    }

    /**
     * Parse the header change limit property.
     *
     * @param props
     */
    protected void parseHeaderChangeLimit(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.headerChangeLimit = convertInteger(option);
                Tr.event(tc, "Config: header change limit is " + getHeaderChangeLimit());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseHeaderChangeLimit", "1");
                Tr.event(tc, "Config: Invalid header change count of " + option);

            }
        }
    }

    /**
     * Check whether or not the request smuggling protection has been changed.
     *
     * @param props
     */
    protected void parseRequestSmugglingProtection(Object option) {
        // PK53193 - allow this to be disabled
        if (Objects.nonNull(option)) {
            this.bEnableSmugglingProtection = convertBoolean(option);
            Tr.debug(tc, "Config: request smuggling protection is " + this.bEnableSmugglingProtection);

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
    protected void parseAutoDecompression(Object option) {
        // PK41619 - allow this to be turned off
        if (Objects.nonNull(option)) {
            this.bAutoDecompression = convertBoolean(option);
            Tr.event(tc, "Config: autodecompression is " + isAutoDecompressionEnabled());

        }
    }

    /**
     * Check the for the property v0CookieDateRFC1123compat
     *
     * @param props
     */
    protected void parsev0CookieDateRFC1123compat(Object option) {
        if (Objects.nonNull(option)) {
            this.v0CookieDateRFC1123compat = convertBoolean(option);
            Tr.event(tc, "Config: v0CookieDateRFC1123compat is " + isv0CookieDateRFC1123compat() + " this = " + this);
        }

    }

    /**
     * Check the configuration map for if we should skip adding the quote
     * to the cookie path attribute
     *
     * @ param props
     */
    protected void parseSkipCookiePathQuotes(Object option) {
        //738893 - Skip adding the quotes to the cookie path attribute
        if (Objects.nonNull(option)) {
            this.skipCookiePathQuotes = convertBoolean(option);
            Tr.event(tc, "Config: SkipCookiePathQuotes is " + shouldSkipCookiePathQuotes());

        }
    }

    /**
     * Check the configuration map for the property to tell us if we should prevent multiple set-cookies with the same name
     *
     * @ param props
     */
    protected void parseDoNotAllowDuplicateSetCookies(Object option) {
        //PI31734 - prevent multiple Set-Cookies with the same name
        if (Objects.nonNull(option)) {
            this.doNotAllowDuplicateSetCookies = convertBoolean(option);
            Tr.event(tc, "Config: DoNotAllowDuplicateSetCookies is " + doNotAllowDuplicateSetCookies());

        }
    }

    /**
     * Check the configuration map for using WaitForEndOfMessage
     *
     * @param props
     */
    protected void parseWaitForEndOfMessage(Object option) {
        //PI11176
        if (Objects.nonNull(option)) {
            this.waitForEndOfMessage = convertBoolean(option);
            Tr.event(tc, "Config: PI33453:WaitForEndOfMessage is " + shouldWaitForEndOfMessage());

        }
    }

    /**
     * Check the configuration map for to see if we should send or not content-length on 1xx and 204 responses
     *
     * @ param props
     */
    protected void parseRemoveCLHeaderInTempStatusRespRFC7230compat(Object option) {
        //PI35277
        if (Objects.nonNull(option)) {
            this.removeCLHeaderInTempStatusRespRFC7230compat = convertBoolean(option);
            Tr.debug(tc, "Config: RemoveCLHeaderInTempStatusRespRFC7230compat "
                         + shouldRemoveCLHeaderInTempStatusRespRFC7230compat());

        }
    }

    /**
     * Check the configuration map for if we should prevent response splitting
     *
     * @ param props
     */
    protected void parsePreventResponseSplit(Object option) {
        //PI45266
        if (Objects.nonNull(option)) {
            this.preventResponseSplit = convertBoolean(option);
            Tr.event(tc, "Config: PreventResponseSplit is " + shouldPreventResponseSplit());

        }
    }

    /**
     * Check the configuration map for using purge behavior at the close of the connection
     *
     * @ param props
     */
    protected void parseAttemptPurgeData(Object option) {
        //PI11176
        if (Objects.nonNull(option)) {
            this.attemptPurgeData = convertBoolean(option);
            Tr.event(tc, "Config: PI11176:PurgeDataDuringClose is " + shouldAttemptPurgeData());

        }
    }

    /**
     * Check the configuration map for if we should swallow inbound connections IOEs
     *
     * @ param props
     */
    protected void parseThrowIOEForInboundConnections(Object option) {
        //PI57542
        if (Objects.nonNull(option)) {
            this.throwIOEForInboundConnections = convertBoolean(option);
            Tr.event(tc, "Config: ThrowIOEForInboundConnections is " + throwIOEForInboundConnections());

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
    protected void parsePurgeRemainingResponseBody() {

        String option = AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
            @Override
            public String run() {
                return (System.getProperty(HttpConfigConstants.PROPNAME_PURGE_REMAINING_RESPONSE));
            }
        });

        if (Objects.nonNull(option)) {
            this.purgeRemainingResponseBody = convertBoolean(option);
            Tr.event(tc, "Config: PurgeRemainingResponseBody is " + shouldPurgeRemainingResponseBody());

        }
    }

    /**
     * Check the configuration to see if there is a desired http protocol version
     * that has been provided for this HTTP Channel
     *
     * @param props
     */
    protected void parseProtocolVersion(Object option) {
        if (Objects.nonNull(option)) {

            String protocolVersion = ((String) option).toLowerCase();
            if (HttpConfigConstants.PROTOCOL_VERSION_11.equals(protocolVersion)) {
                this.useH2ProtocolAttribute = Boolean.FALSE;
            } else if (HttpConfigConstants.PROTOCOL_VERSION_2.equals(protocolVersion)) {
                this.useH2ProtocolAttribute = Boolean.TRUE;

            }

            if (Objects.nonNull(option)) {
                Tr.event(tc, "HTTP Channel Config: versionProtocol has been set to " + protocolVersion);
            }

        }

    }

    /**
     * Check the configuration to see if the decompression ratio limit
     * has changed.
     *
     * @param props
     */
    protected void parseDecompressionRatioLimit(Object option) {
        if (Objects.nonNull(option)) {
            try {
                this.decompressionRatioLimit = convertInteger(option);
                Tr.event(tc, "Config: Decompression ratio limit is set to: " + getDecompressionRatioLimit());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseDecompressionRatioLimit", "1");
                Tr.event(tc, "Config: Invalid decompression ratio limit; " + option);

            }
        }
    }

    /**
     * Check the configuration to see if the decompression tolerance has
     * changed.
     *
     * @param props
     */

    protected void parseDecompressionTolerance(Object option) {

        if (Objects.nonNull(option)) {
            try {
                this.decompressionTolerance = convertInteger(option);
                Tr.event(tc, "Config: Decompression tolerance is set to: " + getDecompressionTolerance());

            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".parseDecompressionTolerance", "1");
                Tr.event(tc, "Config: Invalid decompression tolerance; " + option);

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

    public boolean getH2LimitWindowUpdateFrames() {
        return this.http2LimitWindowUpdateFrames;
    }

    public long getH2ConnCloseTimeout() {
        return http2ConnectionCloseTimeout;
    }

    public int getH2SettingsInitialWindowSize() {
        return http2SettingsInitialWindowSize;
    }

    public int getH2ConnectionWindowSize() {
        return http2ConnectionWindowSize;
    }

    public int getH2MaxResetFrames() {
        return http2MaxResetFrames;
    }

    public int getH2ResetFramesWindow() {
        return http2ResetFramesWindow;
    }

    public int getH2MaxStreamsRefused() {
        return http2MaxStreamsRefused;
    }

    public long getH2MaxHeaderBlockSize() {
        return http2MaxHeaderBlockSize;
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
            Tr.debug(tc, "Config: " + size + " too small");

            return min;
        } else if (size > max) {
            Tr.debug(tc, "Config: " + size + " too large");

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
            Tr.debug(tc, "Config: " + input + " too small.");

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
            Tr.debug(tc, "Config: " + input + " too small.");

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
        MSP.log("getting access logger");
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
        return this.useRemoteIpOptions;
    }

    public Pattern getForwardedProxiesRegex() {
        if (Objects.isNull(proxiesPattern)) {
            this.proxiesPattern = Pattern.compile(this.proxiesRegex);
        }

        return this.proxiesPattern;
    }

    /**
     * @return
     */
    public boolean useForwardingHeadersInAccessLog() {
        return (this.useForwardingHeadersInAccessLog && this.useRemoteIpOptions);
    }

    public boolean useAutoCompression() {
        return this.useCompressionOptions;
    }

    public Pattern getCompressionQValueRegex() {
        if (this.compressionQValuePattern == null) {
            this.compressionQValuePattern = Pattern.compile(compressionQValueRegex);

        }

        return this.compressionQValuePattern;
    }

    public Set<String> getCompressionContentTypes() {
        return this.includedCompressionContentTypes;
    }

    public Set<String> getExcludedCompressionContentTypes() {
        return this.excludedCompressionContentTypes;
    }

    public String getPreferredCompressionAlgorithm() {
        return this.preferredCompressionAlgorithm;
    }

    /**
     * Specifies whether the <httpEndpoint> is configured to considered the <samesite> sub element configurations.
     *
     * @return
     */
    public boolean useSameSiteConfig() {
        return this.useSameSiteOptions;
    }

    /**
     * Returns a Map of all the configured explicit cookie names and their corresponding SameSite
     * value. In the case that '*' is the only configured pattern, it is added as key to this map.
     *
     * @return
     */
    public Map<String, String> getSameSiteCookies() {
        return this.sameSiteCookies == null ? new HashMap<String, String>() : this.sameSiteCookies;
    }

    /**
     * Returns a Map of all configured cookie name patterns and the corresponding SameSite value.
     * This map is pre-sorted in alphabetical ordering and ranging from the most specific pattern to
     * the least specific.
     *
     * @return
     */
    public Map<Pattern, String> getSameSitePatterns() {
        return this.sameSitePatterns == null ? new HashMap<Pattern, String>() : this.sameSitePatterns;
    }

    /**
     * Returns whether the only defined SameSite pattern is the standalone wildcard '*'. This is
     * used to avoid regular expression compilation. If this is true, the corresponding SameSite
     * value is accessible by using '*' as the key on the getSameSiteCookies map.
     *
     * @return
     */
    public boolean onlySameSiteStar() {
        return this.onlySameSiteStar;
    }

    /*
     * Returns a boolean which indicates whether Partitioned should be added to the SameSite=None cookies. 
     */
    public boolean getPartitioned() {
        return this.isPartitioned;
    }

    /**
     * Query the maximum ratio the HTTP Channel will permit when decompressing
     * a response that has been encoded.
     *
     * @return int
     */
    public int getDecompressionRatioLimit() {
        return this.decompressionRatioLimit;
    }

    /**
     * Query the maximum number of times the HTTP Channel will tolerate the decompression
     * ratio to be above the set decompression ratio limit.
     *
     * @return int
     */
    public int getDecompressionTolerance() {
        return this.decompressionTolerance;
    }

    /**
     * Specifies whether the <httpEndpoint> is configured to use the <headers> sub element configurations.
     */
    public boolean useHeadersConfiguration() {
        return this.useHeadersOptions;
    }

    /**
     * Returns a List of all configured header names and corresponding values that will be added using
     * the appendHeader method.
     */
    public Map<Integer, List<Map.Entry<String, String>>> getConfiguredHeadersToAdd() {
        return this.configuredHeadersToAdd;
    }

    /**
     * Returns a Map of all configured header names and corresponding values that will be set using
     * the setHeader method. Headers in this list will overwrite existing values if already present.
     */
    public Map<Integer, Map.Entry<String, String>> getConfiguredHeadersToSet() {
        return this.configuredHeadersToSet;
    }

    /**
     * Returns a Map of all configured header names and corresponding values that will be set using
     * the setHeader method. Headers in this list will only be set if missing from the response.
     */
    public Map<Integer, Map.Entry<String, String>> getConfiguredHeadersToSetIfMissing() {
        return this.configuredHeadersToSetIfMissing;
    }

    /**
     * Returns a Set of all configured header names that will be removed from responses.
     *
     * @return
     */
    public Map<Integer, String> getConfiguredHeadersToRemove() {
        return this.configuredHeadersToRemove;
    }

    public boolean useNetty() {
        return useNetty;
    }

}
