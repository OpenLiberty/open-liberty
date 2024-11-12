/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer.ConfigElement;

import io.openliberty.http.options.SslOption;

/**
 * Configuration class for Netty-based HTTP channels.
 */
public class NettyHttpChannelConfig extends HttpChannelConfig {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(NettyHttpChannelConfig.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    
    private boolean suppressHandshakeError;
    private int suppressHandshakeErrorCount;


    

    /**
     * Constructor for NettyHttpChannelConfig.
     *
     * @param builder The NettyConfigBuilder used to construct this configuration.
     */
    private NettyHttpChannelConfig(NettyConfigBuilder builder) {
        Objects.nonNull(builder);
        this.useNetty = Boolean.TRUE;
        this.useCompressionOptions = builder.useCompression;
        this.useRemoteIpOptions = builder.useForwardingHeaders;
        this.useHeadersOptions = builder.useHeaders;
        this.useSameSiteOptions = builder.useSameSite;

        this.parseConfig(builder.options);

        parseSslOptions(builder.options);

    }

    public void clear() {

        clearSameSiteOptions();
    }

    public void registerAccessLog(String endpointName){
        parseAccessLog(endpointName);
    }

    /**
     * Parses the provided configuration options.
     *
     * @param config The configuration options to parse.
     */
    private void parseConfig(Map<String, Object> config) {

        if (Objects.isNull(config) || config.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "parseConfig", "The configuration provided was invalid, default values will be used.");
            }
            return;
        }
        

        if (useCompressionOptions) {
            parseCompressionOptions(config);
        }

        if (useHeadersOptions) {
            parseHeaderOptions(config);
        }

        if (useRemoteIpOptions) {
            parseRemoteIpOptions(config);
        }

        parseSameSiteOptions(config);

        parseHttpOptions(config);

        

    }

    private void parseCompressionOptions(Map<String, Object> options) {
        Objects.requireNonNull(options);

        if (this.useCompressionOptions) {
            this.includedCompressionContentTypes = new HashSet<String>();
            this.includedCompressionContentTypes.add("text/*");
            this.includedCompressionContentTypes.add("application/javascript");
            this.excludedCompressionContentTypes = new HashSet<String>();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "parseCompressionOptions", "Http Channel Config: compression has been enabled");
            }

            this.parseCompressionTypes(options.get(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES));
            this.parseCompressionPreferredAlgorithm(options.get(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM));
        }

    }

    private void parseHeaderOptions(Map<String, Object> options) {
        String method = "parseHeaderOptions";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, method);
        }

        if (options.containsKey(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD) ||
            options.containsKey(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET) ||
            options.containsKey(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE) ||
            options.containsKey(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING)) {

            this.configuredHeadersToAdd = new HashMap<Integer, List<Map.Entry<String, String>>>();
            this.configuredHeadersToSet = new HashMap<Integer, Map.Entry<String, String>>();
            this.configuredHeadersToSetIfMissing = new HashMap<Integer, Map.Entry<String, String>>();
            this.configuredHeadersToRemove = new HashMap<Integer, String>();
            this.configuredHeadersErrorSet = new HashSet<String>();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, method, "Http Channel Config: <headers> config has been enabled");
            }

            parseHeadersToRemove(options.get(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE));
            parseHeadersToAdd(options.get(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD));
            parseHeadersToSet(options.get(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET));
            parseHeadersToSetIfMissing(options.get(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING));
            logHeadersConfig();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, method);
        }
    }

    private void parseHttpOptions(Map<String, Object> options) {

        //TODO -> Netty Needed

        //parseAccessLog(options.get(HttpConfigConstants.PROPNAME_ACCESSLOG_ID));
        parseAllowRetries(options.get(HttpConfigConstants.PROPNAME_ALLOW_RETRIES));
        parseAttemptPurgeData(options.get(HttpConfigConstants.PROPNAME_PURGE_DATA_DURING_CLOSE));
        parseAutoDecompression(options.get(HttpConfigConstants.PROPNAME_AUTODECOMPRESSION));
        parseBufferType(options.get(HttpConfigConstants.PROPNAME_DIRECT_BUFF));
        parseCookieUpdate(options.get(HttpConfigConstants.PROPNAME_NO_CACHE_COOKIES_CONTROL), options.get(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE));
        parseDateHeaderRange(options.get(HttpConfigConstants.PROPNAME_DATE_HEADER_RANGE));
        parseDecompressionRatioLimit(options.get(HttpConfigConstants.PROPNAME_DECOMPRESSION_RATIO_LIMIT));
        parseDecompressionTolerance(options.get(HttpConfigConstants.PROPNAME_DECOMPRESSION_TOLERANCE));

        parseDoNotAllowDuplicateSetCookies(options.get(HttpConfigConstants.PROPNAME_DO_NOT_ALLOW_DUPLICATE_SET_COOKIES));
        parseIncomingBodyBufferSize(options.get(HttpConfigConstants.PROPNAME_INCOMING_BODY_BUFFSIZE));
        parseIncomingHdrBufferSize(options.get(HttpConfigConstants.PROPNAME_INCOMING_HDR_BUFFSIZE));
        parseLimitFieldSize(options.get(HttpConfigConstants.PROPNAME_LIMIT_FIELDSIZE));
        parseLimitMessageSize(options.get(HttpConfigConstants.PROPNAME_MSG_SIZE_LIMIT));
        parseLimitNumberHeaders(options.get(HttpConfigConstants.PROPNAME_LIMIT_NUMHEADERS));
        parseLimitNumberResponses(options.get(HttpConfigConstants.PROPNAME_LIMIT_NUMBER_RESPONSES));
        parseOutgoingBufferSize(options.get(HttpConfigConstants.PROPNAME_OUTGOING_HDR_BUFFSIZE));
        parseOutgoingVersion(options.get(HttpConfigConstants.PROPNAME_OUTGOING_VERSION));
        parsePersistTimeout(options.get(HttpConfigConstants.PROPNAME_PERSIST_TIMEOUT));
        parsePersistence(options.get(HttpConfigConstants.PROPNAME_KEEPALIVE_ENABLED), options.get(HttpConfigConstants.PROPNAME_MAX_PERSIST));

        parsePurgeRemainingResponseBody();
        parseReadTimeout(options.get(HttpConfigConstants.PROPNAME_READ_TIMEOUT));
        parseRequestSmugglingProtection(options.get(HttpConfigConstants.PROPNAME_ENABLE_SMUGGLING_PROTECTION));
        parseServerHeader(options.get(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER), options.get(HttpConfigConstants.PROPNAME_SERVER_HEADER_VALUE));
        parseSkipCookiePathQuotes(options.get(HttpConfigConstants.PROPNAME_SKIP_PATH_QUOTE));

        parseWriteTimeout(options.get(HttpConfigConstants.PROPNAME_WRITE_TIMEOUT));
        parsev0CookieDateRFC1123compat(options.get(HttpConfigConstants.PROPNAME_V0_COOKIE_RFC1123_COMPAT));

        // HTTP/2 Options
        parseHttp2Options(options);

    }


    private void parseSslOptions(Map<String, Object> options) {
        this.suppressHandshakeError =  SslOption.SUPPRESS_HANDSHAKE_ERRORS.parse(options);
        this.suppressHandshakeErrorCount = SslOption.SUPPRESS_HANDSHAKE_ERRORS_COUNT.parse(options);
    }

    public boolean suppressHandshakeError() {
        return suppressHandshakeError;
    }

    public int suppressHandshakeErrorCount(){
        return suppressHandshakeErrorCount;
    }

    private void parseHttp2Options(Map<String, Object> options) {
        parseH2ConnCloseTimeout(options.get(HttpConfigConstants.PROPNAME_H2_CONN_CLOSE_TIMEOUT));
        parseH2ConnectionIdleTimeout(options.get(HttpConfigConstants.PROPNAME_H2_CONNECTION_IDLE_TIMEOUT));
        parseH2ConnectionWindowSize(options.get(HttpConfigConstants.PROPNAME_H2_CONN_WINDOW_SIZE));
        parseH2LimitWindowUpdateFrames(options.get(HttpConfigConstants.PROPNAME_H2_LIMIT_WINDOW_UPDATE_FRAMES));
        parseH2MaxConcurrentStreams(options.get(HttpConfigConstants.PROPNAME_H2_MAX_CONCURRENT_STREAMS));
        parseH2MaxFrameSize(options.get(HttpConfigConstants.PROPNAME_H2_MAX_FRAME_SIZE));
        parseProtocolVersion(options.get(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION));
        parseH2SettingsInitialWindowSize(options.get(HttpConfigConstants.PROPNAME_H2_SETTINGS_INITIAL_WINDOW_SIZE));
        parseH2MaxHeaderBlockSize(options.get(HttpConfigConstants.PROPNAME_H2_MAX_HEADER_BLOCK_SIZE));
    }

    private void parseRemoteIpOptions(Map<String, Object> options) {
        String method = "parseRemoteIpOptions";

        if (TraceComponent.isAnyTracingEnabled()) {

            if (tc.isEntryEnabled()) {
                Tr.entry(tc, method);
            }
            if (tc.isEventEnabled()) {
                Tr.event(tc, method, "HTTP Channel Config: remoteIp has been enabled");
            }
        }

        parseRemoteIpProxies(options.get("proxies"));
        parseRemoteIpAccessLog(options.get("useRemoteIpInAccessLog"));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, method);
        }
    }

    private void clearSameSiteOptions() {

        this.useSameSiteOptions = false;
        this.sameSiteCookies = new HashMap<String, String>();
        this.sameSiteErrorCookies = new HashSet<String>();
        this.sameSiteStringPatterns = new HashMap<String, String>();
        this.sameSitePatterns = null;
        this.onlySameSiteStar = false;
        this.isPartitioned = false;
    }

    private void parseSameSiteOptions(Map<String, Object> options) {
        String method = "parseSameSiteOptions";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "parseSameSiteOptions");
        }

        this.sameSiteCookies = new HashMap<String, String>();
        this.sameSiteErrorCookies = new HashSet<String>();
        this.sameSiteStringPatterns = new HashMap<String, String>();
        this.sameSitePatterns = null;
        this.onlySameSiteStar = false;
        this.isPartitioned = false;

        if (this.useSameSiteOptions && (options.containsKey(HttpConfigConstants.PROPNAME_SAMESITE_LAX) ||
                                        options.containsKey(HttpConfigConstants.PROPNAME_SAMESITE_NONE) ||
                                        options.containsKey(HttpConfigConstants.PROPNAME_SAMESITE_STRICT) ||
                                        options.containsKey("partitioned"))) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, method, "Http Channel Config: SameSite configuration has been enabled");
            }

            parseCookiesSameSiteLax(options.get(HttpConfigConstants.PROPNAME_SAMESITE_LAX));
            parseCookiesSameSiteNone(options.get(HttpConfigConstants.PROPNAME_SAMESITE_NONE));
            parseCookiesSameSiteStrict(options.get(HttpConfigConstants.PROPNAME_SAMESITE_STRICT));
            parseCookiesSameSitePartitioned(options.get("partitioned"));

            initSameSiteCookiesPatterns();

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, method);
        }
    }

   

    

    public void disableRemoteIp() {
        useRemoteIpOptions = Boolean.FALSE;
    }

    /**
     * A builder class for constructing NettyHttpChannelConfig instances with specific configurations.
     */
    public static class NettyConfigBuilder {

        private boolean useCompression;
        private boolean useHeaders;
        private boolean useForwardingHeaders;
        private boolean useSameSite;

        private final Map<String, Object> options;
        private Map<String, Object> compressionOptions;
        private Map<String, Object> headerOptions;
        private Map<String, Object> httpOptions;
        private Map<String, Object> forwardingOptions;
        private Map<String, Object> sameSiteOptions;
        private Map<String, Object> sslOptions;

        public NettyConfigBuilder() {
            options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            useCompression = Boolean.FALSE;
            useHeaders = Boolean.FALSE;
            useForwardingHeaders = Boolean.FALSE;
            useSameSite = Boolean.FALSE;

            compressionOptions = Collections.emptyMap();
            headerOptions = Collections.emptyMap();
            httpOptions = Collections.emptyMap();
            forwardingOptions = Collections.emptyMap();
            sameSiteOptions = Collections.emptyMap();
            sslOptions = Collections.emptyMap();

        }

        public NettyConfigBuilder with(ConfigElement config, Map<String, Object> options) {
            if (Objects.isNull(options) || options.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No properties provided, exiting");
                }
                return this;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Http Configuration - " + config);
            }

            switch (config) {

                case COMPRESSION: {
                    useCompression = Boolean.TRUE;
                    compressionOptions = options;
                    break;
                }

                case HEADERS: {
                    useHeaders = Boolean.TRUE;
                    headerOptions = options;
                    break;
                }

                case HTTP_OPTIONS: {
                    httpOptions = options;
                    break;
                }

                case REMOTE_IP: {
                    useForwardingHeaders = Boolean.TRUE;
                    forwardingOptions = options;
                    break;
                }

                case SAMESITE: {
                    useSameSite = Boolean.TRUE;
                    sameSiteOptions = options;
                    break;
                }

                case SSL_OPTIONS: {
                    this.sslOptions = options;
                    break;
                }

                default:
                    break;
            }

            return this;
        }

        private void collectOptions() {

            compressionOptions.forEach(options::putIfAbsent);
            headerOptions.forEach(options::putIfAbsent);
            httpOptions.forEach(options::putIfAbsent);
            forwardingOptions.forEach(options::putIfAbsent);
            sameSiteOptions.forEach(options::putIfAbsent);
            sslOptions.forEach(options::putIfAbsent);

        }

        /**
         * Constructs a NettyHttpChannelConfig instance with the specified configurations.
         *
         * @return A NettyHttpChannelConfig instance.
         */
        public NettyHttpChannelConfig build() {

            collectOptions();

            return new NettyHttpChannelConfig(this);
        }

    }

}