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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.netty.HttpInitializer.ConfigElement;

/**
 *
 */
public class NettyHttpChannelConfig extends HttpChannelConfig {

    private NettyHttpChannelConfig(NettyConfigBuilder builder) {
        this.useNetty = Boolean.TRUE;
        this.useCompression = builder.useCompression;
        this.useForwardingHeaders = builder.useForwardingHeaders;
        this.isHeadersConfigEnabled = builder.useHeaders;
        this.useSameSiteConfig = builder.useSameSite;

        this.parseConfig(builder.options);

    }

    public NettyHttpChannelConfig(Map<String, Object> config) {
        super(config);
        this.useNetty = Boolean.TRUE;
    }

    public NettyHttpChannelConfig() {
        this.useNetty = Boolean.TRUE;
    }

    private void parseConfig(Map<String, Object> config) {
        MSP.log("Parsing netty config by element");
        MSP.log("Using remote ip: " + this.useForwardingHeaders);

        if (Objects.isNull(config) || config.isEmpty()) {
            MSP.log("config is null or empty, returning");
            return;
        }

        //Compression Options

        //Header Options

        //Forwarded Header (RemoteIp) Options

        //SameSite Options
        if (this.useForwardingHeaders) {
            parseRemoteIpOptions(config);
        }
        //Http Options
        parseHttpOptions(config);

    }

    public void updateConfig(ConfigElement element, Map<String, Object> config) {

    }

    public void updateRemoteIp(Map<String, Object> options) {
        if (Objects.isNull(options) || options.isEmpty()) {
            useForwardingHeaders = Boolean.FALSE;
        }

        this.useForwardingHeaders = Boolean.TRUE;

        System.out.println("Using forwarding: " + this.useForwardingHeaders);

        //parseRemoteIpProxies(options);
        //parseRemoteIpAccessLog(options);

    }

    private void parseCompressionOptions(Map<String, Object> options) {
        if (this.useCompression) {

        }

    }

    private void parseHeaderOptions(Map<String, Object> options) {

    }

    private void parseHttpOptions(Map<String, Object> options) {

        this.parseAccessLog(options.get(HttpConfigConstants.PROPNAME_ACCESSLOG_ID));
    }

    private void parseRemoteIpOptions(Map<String, Object> options) {
        MSP.log("Parse remote Ip Options");
        parseRemoteIpProxies(options.get(HttpConfigConstants.PROPNAME_REMOTE_PROXIES));
        parseRemoteIpAccessLog(options.get(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG));
    }

    private void parseSameSiteOptions(Map<String, Object> options) {

    }

    public void disableRemoteIp() {
        useForwardingHeaders = Boolean.FALSE;
    }

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

        }

        public NettyConfigBuilder with(ConfigElement config, Map<String, Object> options) {

            if (Objects.isNull(options) || options.isEmpty()) {
                MSP.log("No properties, exit");
                return this;
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

            MSP.log("Collected all properties...");
            for (String s : options.keySet()) {
                MSP.log(s + ": " + options.get(s));
            }
        }

        public NettyHttpChannelConfig build() {

            collectOptions();

            return new NettyHttpChannelConfig(this);
        }

    }

}

//Keep-Alive -> dispatcher controlled. Can be also cleaned up from top level dispatcherHandler
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_KEEPALIVE_ENABLED)) {

//Same as keep alive
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_MAX_PERSIST)) {

//next read done on a connection -> handled by IO handler
//      if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PERSIST_TIMEOUT)) {

//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_READ_TIMEOUT)) {
//        props.put(HttpConfigConstants.PROPNAME_READ_TIMEOUT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_WRITE_TIMEOUT)) {
//        props.put(HttpConfigConstants.PROPNAME_WRITE_TIMEOUT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_JIT_ONLY_READS)) {
//        props.put(HttpConfigConstants.PROPNAME_JIT_ONLY_READS, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_OUTGOING_VERSION)) {
//        props.put(HttpConfigConstants.PROPNAME_OUTGOING_VERSION, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DIRECT_BUFF)) {
//        props.put(HttpConfigConstants.PROPNAME_DIRECT_BUFF, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_OUTGOING_HDR_BUFFSIZE)) {
//        props.put(HttpConfigConstants.PROPNAME_OUTGOING_HDR_BUFFSIZE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_INCOMING_HDR_BUFFSIZE)) {
//        props.put(HttpConfigConstants.PROPNAME_INCOMING_HDR_BUFFSIZE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_INCOMING_BODY_BUFFSIZE)) {
//        props.put(HttpConfigConstants.PROPNAME_INCOMING_BODY_BUFFSIZE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_BYTE_CACHE_SIZE)) {
//        props.put(HttpConfigConstants.PROPNAME_BYTE_CACHE_SIZE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_EXTRACT_VALUE)) {
//        props.put(HttpConfigConstants.PROPNAME_EXTRACT_VALUE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_BINARY_TRANSPORT)) {
//        props.put(HttpConfigConstants.PROPNAME_BINARY_TRANSPORT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_LIMIT_FIELDSIZE)) {
//        props.put(HttpConfigConstants.PROPNAME_LIMIT_FIELDSIZE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_LIMIT_NUMHEADERS)) {
//        props.put(HttpConfigConstants.PROPNAME_LIMIT_NUMHEADERS, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_LIMIT_NUMBER_RESPONSES)) {
//        props.put(HttpConfigConstants.PROPNAME_LIMIT_NUMBER_RESPONSES, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_MSG_SIZE_LIMIT)) {
//        props.put(HttpConfigConstants.PROPNAME_MSG_SIZE_LIMIT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_ALLOW_RETRIES)) {
//        props.put(HttpConfigConstants.PROPNAME_ALLOW_RETRIES, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_HEADER_VALIDATION)) {
//        props.put(HttpConfigConstants.PROPNAME_HEADER_VALIDATION, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_STRICT_URL_FORMAT)) {
//        props.put(HttpConfigConstants.PROPNAME_STRICT_URL_FORMAT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SERVER_HEADER_VALUE)) {
//        props.put(HttpConfigConstants.PROPNAME_SERVER_HEADER_VALUE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER)) {
//        props.put(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DATE_HEADER_RANGE)) {
//        props.put(HttpConfigConstants.PROPNAME_DATE_HEADER_RANGE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE)) {
//        props.put(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_HEADER_CHANGE_LIMIT)) {
//        props.put(HttpConfigConstants.PROPNAME_HEADER_CHANGE_LIMIT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_AUTODECOMPRESSION)) {
//        props.put(HttpConfigConstants.PROPNAME_AUTODECOMPRESSION, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_ENABLE_SMUGGLING_PROTECTION)) {
//        props.put(HttpConfigConstants.PROPNAME_ENABLE_SMUGGLING_PROTECTION, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RUNNING_ON_ZOS)) {
//        props.put(HttpConfigConstants.PROPNAME_RUNNING_ON_ZOS, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SERVANT_REGION)) {
//        props.put(HttpConfigConstants.PROPNAME_SERVANT_REGION, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_CONTROL_REGION)) {
//        props.put(HttpConfigConstants.PROPNAME_CONTROL_REGION, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_V0_COOKIE_RFC1123_COMPAT)) {
//        props.put(HttpConfigConstants.PROPNAME_V0_COOKIE_RFC1123_COMPAT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DO_NOT_ALLOW_DUPLICATE_SET_COOKIES)) {
//        props.put(HttpConfigConstants.PROPNAME_DO_NOT_ALLOW_DUPLICATE_SET_COOKIES, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_WAIT_FOR_END_OF_MESSAGE)) {
//        props.put(HttpConfigConstants.PROPNAME_WAIT_FOR_END_OF_MESSAGE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.REMOVE_CLHEADER_IN_TEMP_STATUS_RFC7230_COMPAT)) {
//        props.put(HttpConfigConstants.REMOVE_CLHEADER_IN_TEMP_STATUS_RFC7230_COMPAT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PREVENT_RESPONSE_SPLIT)) {
//        props.put(HttpConfigConstants.PROPNAME_PREVENT_RESPONSE_SPLIT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PURGE_DATA_DURING_CLOSE)) {
//        props.put(HttpConfigConstants.PROPNAME_PURGE_DATA_DURING_CLOSE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_THROW_IOE_FOR_INBOUND_CONNECTIONS)) {
//        props.put(HttpConfigConstants.PROPNAME_THROW_IOE_FOR_INBOUND_CONNECTIONS, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SKIP_PATH_QUOTE)) {
//        props.put(HttpConfigConstants.PROPNAME_SKIP_PATH_QUOTE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_CONN_CLOSE_TIMEOUT)) {
//        props.put(HttpConfigConstants.PROPNAME_H2_CONN_CLOSE_TIMEOUT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_SETTINGS_INITIAL_WINDOW_SIZE)) {
//        props.put(HttpConfigConstants.PROPNAME_H2_SETTINGS_INITIAL_WINDOW_SIZE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_CONNECTION_IDLE_TIMEOUT)) {
//        props.put(HttpConfigConstants.PROPNAME_H2_CONNECTION_IDLE_TIMEOUT, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_MAX_CONCURRENT_STREAMS)) {
//        props.put(HttpConfigConstants.PROPNAME_H2_MAX_CONCURRENT_STREAMS, value);
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_MAX_FRAME_SIZE)) {
//        props.put(HttpConfigConstants.PROPNAME_H2_MAX_FRAME_SIZE, value);
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_H2_LIMIT_WINDOW_UPDATE_FRAMES)) {
//        props.put(HttpConfigConstants.PROPNAME_H2_LIMIT_WINDOW_UPDATE_FRAMES, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PURGE_REMAINING_RESPONSE)) {
//        props.put(HttpConfigConstants.PROPNAME_PURGE_REMAINING_RESPONSE, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION)) {
//        props.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, value);
//        continue;
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_REMOTE_PROXIES)) {
//        props.put(HttpConfigConstants.PROPNAME_REMOTE_PROXIES, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_REMOTE_IP)) {
//        props.put(HttpConfigConstants.PROPNAME_REMOTE_IP, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG)) {
//        props.put(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_COMPRESSION)) {
//        props.put(HttpConfigConstants.PROPNAME_COMPRESSION, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES)) {
//        props.put(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM)) {
//        props.put(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM, value);
//    }
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DECOMPRESSION_RATIO_LIMIT)) {
//        props.put(HttpConfigConstants.PROPNAME_DECOMPRESSION_RATIO_LIMIT, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_DECOMPRESSION_TOLERANCE)) {
//        props.put(HttpConfigConstants.PROPNAME_DECOMPRESSION_TOLERANCE, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SAMESITE)) {
//        props.put(HttpConfigConstants.PROPNAME_SAMESITE, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SAMESITE_LAX)) {
//        props.put(HttpConfigConstants.PROPNAME_SAMESITE_LAX, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SAMESITE_NONE)) {
//        props.put(HttpConfigConstants.PROPNAME_SAMESITE_NONE, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_SAMESITE_STRICT)) {
//        props.put(HttpConfigConstants.PROPNAME_SAMESITE_STRICT, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS)) {
//        props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD)) {
//        props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET)) {
//        props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING)) {
//        props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING, value);
//    }
//
//    if (key.equalsIgnoreCase(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE)) {
//        props.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE, value);
//    }
//
//    props.put(key, value);
//}
