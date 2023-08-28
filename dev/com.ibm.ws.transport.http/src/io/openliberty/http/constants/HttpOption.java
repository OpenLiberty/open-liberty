/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.constants;

/**
 *
 */
public enum HttpOption {

    KEEP_ALIVE("keepAliveEnabled", true),
    MAX_KEEP_ALIVE("keepAliveEnabled", -1),
    PERSIST_TIMEOUT("persistTimeout", 30000),
    READ_TIMEOUT("readTimeout", 60000),
    WRITE_TIMEOUT("writeTimeout", 60000),
    REMOVE_SERVER_HEADER("removeServerHeader", false),
    NO_CACHE_COOKIES_CONTROL("NoCacheCookiesControl", true),
    AUTO_DECOMPRESSION("AutoDecompression", true),
    LIMIT_NUM_HEADERS("limitNumHeaders", 500),
    LIMIT_FIELD_SIZE("limitFieldSize", 32768),
    DO_NOT_ALLOW_DUPLICATE_SET_COOKIES("DoNotAllowDuplicateSetCookies", false),
    MESSAGE_SIZE_LIMIT("MessageSizeLimit", -1),
    INCOMING_BODY_BUFFER_SIZE("incomingBodyBufferSize", 32768),
    THROW_IOE_FOR_INBOUND_CONNECTIONS("ThrowIOEForInboundConnections", null),
    DECOMPRESSION_RATIO_LIMIT("decompressionRatioLimit", 200),
    DECOMPRESSION_TOLERANCE("decompressionTolerance", 3);

    String id;
    Object value;

    HttpOption(String id, Object value) {
        this.id = id;
        this.value = value;
    }

    public Object value() {
        return value;
    }

    @Override
    public String toString() {
        return id;
    }

}

//AD name="%http.keepAlive" description="%http.keepAlive.desc"
//id="keepAliveEnabled" required="false" type="Boolean" default="true" />
//
//<AD name="%http.maxKeepAliveRequests" description="%http.maxKeepAliveRequests.desc"
//id="maxKeepAliveRequests" required="false" type="Integer" default="-1" min="-1" />
//
//<AD name="%http.persistTimeout" description="%http.persistTimeout.desc"
//id="persistTimeout" required="false" type="String" ibm:type="duration(s)" min="0" default="30s" />
//
//<AD name="%http.readTimeout" description="%http.readTimeout.desc"
//id="readTimeout" required="false" type="String" ibm:type="duration(s)" min="0" default="60s" />
//
//<AD name="%http.writeTimeout" description="%http.writeTimeout.desc"
//id="writeTimeout" required="false" type="String" ibm:type="duration(s)" min="0" default="60s" />
//
//<AD name="%http.removeServerHeader" description="%http.removeServerHeader.desc"
//id="removeServerHeader" required="false" type="Boolean" default="false" />
//
//<AD name="%http.noCacheCookiesControl" description="%http.noCacheCookiesControl.desc"
//id="NoCacheCookiesControl" required="false" type="Boolean" default="true" />
//
//<AD name="%http.autoDecompression" description="%http.autoDecompression.desc"
//id="AutoDecompression" required="false" type="Boolean" default="true" />
//
//<AD name="%http.limitNumHeaders" description="%http.limitNumHeaders.desc"
//id="limitNumHeaders" required="false" type="Integer" min="50" max="500" default="500" />
//
//<AD name="%http.limitFieldSize" description="%http.limitFieldSize.desc"
//id="limitFieldSize" required="false" type="Integer" min="50" max="32768" default="32768" />
//
//<AD name="%http.doNotAllowDuplicateSetCookies" description="%http.doNotAllowDuplicateSetCookies.desc"
//id="DoNotAllowDuplicateSetCookies" required="false" type="String" default="false" />
//
//<AD name="%http.messageSizeLimit" description="%http.messageSizeLimit.desc"
//id="MessageSizeLimit" required="false" type="Long" default="-1" />
//
//<AD name="%http.incomingBodyBufferSize" description="%http.incomingBodyBufferSize.desc"
//id="incomingBodyBufferSize" required="false" type="Integer" min="1024" max="1048576" default="32768" />
//
//<AD name="%http.throwIOEForInboundConnections" description="%http.throwIOEForInboundConnections.desc"
//id="ThrowIOEForInboundConnections" required="false" type="Boolean"/>
//
//<AD name="%http.decompressionRatioLimit" description="%http.decompressionRatioLimit.desc"
//id="decompressionRatioLimit" required="false" type="Integer" min="1" default="200" />
//
//<AD name="%http.decompressionTolerance" description="%http.decompressionTolerance.desc"
//id="decompressionTolerance" required="false" type="Integer" min="0" default="3" />
//
//<AD name="%http2.connectionIdleTimeout" description="%http2.connectionIdleTimeout.desc"
//id="http2ConnectionIdleTimeout" required="false" type="String" ibm:type="duration(s)" min="0" default="0" />
//
//<AD name="%http2.maxConcurrentStreams" description="%http2.maxConcurrentStreams.desc"
//id="maxConcurrentStreams" required="false" type="Integer" default="200" />
//
//<AD name="%http2.maxFrameSize" description="%http2.maxFrameSize.desc"
//id="maxFrameSize" required="false" type="Integer" min="16384" max="16777215" default="57344" />
//
//<AD name="%http2.settingsInitialWindowSize" description="%http2.settingsInitialWindowSize.desc"
//id="settingsInitialWindowSize" required="false" type="Integer" min="1" max="2147483647" default="65535" />
//
//<AD name="%http2.connectionWindowSize" description="%http2.connectionWindowSize.desc"
//id="connectionWindowSize" required="false" type="Integer" min="65535" max="2147483647" default="65535" />
//
//<AD name="%http2.limitWindowUpdateFrames" description="%http2.limitWindowUpdateFrames.desc"
//id="limitWindowUpdateFrames" required="false" type="Boolean" default="false" />
//
