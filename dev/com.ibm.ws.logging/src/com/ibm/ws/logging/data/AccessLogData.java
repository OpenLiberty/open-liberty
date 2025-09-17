/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.LogFieldConstants;

/**
 *
 */
public class AccessLogData extends GenericData {
    public static final String[] NAMES_JSON = {
                                                LogFieldConstants.IBM_REQUESTSTARTTIME,
                                                LogFieldConstants.IBM_URIPATH,
                                                LogFieldConstants.IBM_REQUESTMETHOD,
                                                LogFieldConstants.IBM_QUERYSTRING,
                                                LogFieldConstants.IBM_REQUESTHOST,
                                                LogFieldConstants.IBM_REQUESTPORT,
                                                LogFieldConstants.IBM_REMOTEHOST,
                                                LogFieldConstants.IBM_USERAGENT,
                                                LogFieldConstants.IBM_REQUESTPROTOCOL,
                                                LogFieldConstants.IBM_BYTESRECEIVED,
                                                LogFieldConstants.IBM_RESPONSECODE,
                                                LogFieldConstants.IBM_ELAPSEDTIME,
                                                LogFieldConstants.IBM_DATETIME,
                                                LogFieldConstants.IBM_SEQUENCE,
                                                LogFieldConstants.HOST,
                                                LogFieldConstants.IBM_USERDIR,
                                                LogFieldConstants.IBM_SERVERNAME,
                                                LogFieldConstants.TYPE,
                                                LogFieldConstants.IBM_REMOTEIP,
                                                LogFieldConstants.IBM_BYTESSENT,
                                                LogFieldConstants.IBM_COOKIE,
                                                LogFieldConstants.IBM_REQUESTELAPSEDTIME,
                                                LogFieldConstants.IBM_REQUESTHEADER,
                                                LogFieldConstants.IBM_RESPONSEHEADER,
                                                LogFieldConstants.IBM_REQUESTFIRSTLINE,
                                                LogFieldConstants.IBM_ACCESSLOGDATETIME,
                                                LogFieldConstants.IBM_REMOTEUSERID,
                                                LogFieldConstants.IBM_REMOTEPORT

    };

    private final static String[] NAMES_LC = {
                                               LogFieldConstants.REQUESTSTARTTIME,
                                               LogFieldConstants.URIPATH,
                                               LogFieldConstants.REQUESTMETHOD,
                                               LogFieldConstants.QUERYSTRING,
                                               LogFieldConstants.REQUESTHOST,
                                               LogFieldConstants.REQUESTPORT,
                                               LogFieldConstants.REMOTEHOST,
                                               LogFieldConstants.USERAGENT,
                                               LogFieldConstants.REQUESTPROTOCOL,
                                               LogFieldConstants.BYTESRECEIVED,
                                               LogFieldConstants.RESPONSECODE,
                                               LogFieldConstants.ELAPSEDTIME,
                                               LogFieldConstants.DATETIME,
                                               LogFieldConstants.SEQUENCE,
                                               LogFieldConstants.HOSTNAME,
                                               LogFieldConstants.WLPUSERDIR,
                                               LogFieldConstants.SERVERNAME,
                                               LogFieldConstants.TYPE,
                                               LogFieldConstants.REMOTEIP,
                                               LogFieldConstants.BYTESSENT,
                                               LogFieldConstants.COOKIE,
                                               LogFieldConstants.REQUESTELAPSEDTIME,
                                               LogFieldConstants.REQUESTHEADER,
                                               LogFieldConstants.RESPONSEHEADER,
                                               LogFieldConstants.REQUESTFIRSTLINE,
                                               LogFieldConstants.ACCESSLOGDATETIME,
                                               LogFieldConstants.REMOTEUSERID,
                                               LogFieldConstants.REMOTEPORT
    };

    private static final short JSON_KEY = CollectorConstants.KEYS_JSON;

    // For renaming/omitting fields
    private static Map<String, String> cookieMap = new HashMap<>();
    private static Map<String, String> requestHeaderMap = new HashMap<>();
    private static Map<String, String> responseHeaderMap = new HashMap<>();

    private static NameAliases jsonLoggingNameAliases = new NameAliases(NAMES_JSON);
    private static NameAliases jsonLoggingNameAliasesLogstash = new NameAliases(NAMES_LC);
    private static NameAliases loggingNameAliasesTelemetry = new NameAliases(NAMES_LC);

    private static NameAliases[] nameAliases = { jsonLoggingNameAliases, jsonLoggingNameAliasesLogstash, loggingNameAliasesTelemetry };

    // Each formatter represents one type of access log format, and is null if not applicable
    // { <default JSON logging>, <logFormat JSON logging>, <default logstashCollector>, <logFormat logstashCollector> }
    public AccessLogDataFormatter[] formatters = new AccessLogDataFormatter[4];
    KeyValuePairList kvplCookies = new KeyValuePairList("cookies");
    KeyValuePairList kvplRequestHeaders = new KeyValuePairList("requestHeaders");
    KeyValuePairList kvplResponseHeaders = new KeyValuePairList("responseHeaders");

    public static void populateDataMaps(Map<String, String> cookies, Map<String, String> requestHeaders, Map<String, String> responseHeaders) {
        cookieMap = cookies;
        requestHeaderMap = requestHeaders;
        responseHeaderMap = responseHeaders;
    }

    public void addFormatters(AccessLogDataFormatter[] formatters) {
        this.formatters = formatters;
    }

    public AccessLogDataFormatter[] getFormatters() {
        return this.formatters;
    }

    public static void newJsonLoggingNameAliases(Map<String, String> newAliases) {
        jsonLoggingNameAliases.newAliases(newAliases);
    }

    public static void resetJsonLoggingNameAliases() {
        jsonLoggingNameAliases.resetAliases();
        cookieMap = new HashMap<>();
        requestHeaderMap = new HashMap<>();
        responseHeaderMap = new HashMap<>();
    }

    public AccessLogData() {
        super(24);
    }

    private void setPair(int index, String s) {
        setPair(index, NAMES_JSON[index], s);
    }

    private void setPair(int index, int i) {
        setPair(index, NAMES_JSON[index], i);
    }

    private void setPair(int index, long l) {
        setPair(index, NAMES_JSON[index], l);
    }

    private KeyValuePairList getValues(int index) {
        return (KeyValuePairList) getPairs()[index];
    }

    // @formatter:off
    public void setRequestStartTime(long l)    { setPair(0, l); }
    public void setUriPath(String s)           { setPair(1, s); }
    public void setRequestMethod(String s)     { setPair(2, s); }
    public void setQueryString(String s)       { setPair(3, s); }
    public void setRequestHost(String s)       { setPair(4, s); }
    public void setRequestPort(String s)       { setPair(5, s); }
    public void setRemoteHost(String s)        { setPair(6, s); }
    public void setUserAgent(String s)         { setPair(7, s); }
    public void setRequestProtocol(String s)   { setPair(8, s); }
    public void setBytesReceived(long l)       { setPair(9, l); }
    public void setResponseCode(int i)         { setPair(10, i); }
    public void setElapsedTime(long l)         { setPair(11, l); }
    public void setDatetime(long l)            { setPair(12, l); }
    public void setSequence(String s)          { setPair(13, s); }
    // New optional access log fields
    public void setRemoteIP(String s)          { setPair(14, s); }
    public void setBytesSent(long l)           { setPair(15, l); }
    public void setRequestElapsedTime(long l)  { setPair(17, l); }
    public void setRequestFirstLine(String s)  { setPair(20, s); }
    public void setAccessLogDatetime(long l)   { setPair(21, l); }
    public void setRemoteUser(String s)        { setPair(22, s); }
    public void setRemotePort(String s)        { setPair(23, s); }
    public void setCookies(String name, String value) {
        kvplCookies.addKeyValuePair(name, value);
        setPair(16, kvplCookies);
    }
    public void setRequestHeader(String name, String value) {
        kvplRequestHeaders.addKeyValuePair(name, value);
        setPair(18, kvplRequestHeaders);
    }
    public void setResponseHeader(String name, String value) {
        kvplResponseHeaders.addKeyValuePair(name, value);
        setPair(19, kvplResponseHeaders);
    }

    public long getRequestStartTime()            { return getLongValue(0); }
    public String getUriPath()                   { return getStringValue(1); }
    public String getRequestMethod()             { return getStringValue(2); }
    public String getQueryString()               { return getStringValue(3); }
    public String getRequestHost()               { return getStringValue(4); }
    public String getRequestPort()               { return getStringValue(5); }
    public String getRemoteHost()                { return getStringValue(6); }

    public String getUserAgent()                 { return getStringValue(7); }
    public String getRequestProtocol()           { return getStringValue(8); }
    public long getBytesReceived()               { return getLongValue(9); }
    public int getResponseCode()                 { return getIntValue(10); }
    public long getElapsedTime()                 { return getLongValue(11); }
    public long getDatetime()                    { return getLongValue(12); }
    public String getSequence()                  { return getStringValue(13); }
    public String getRemoteIP()                  { return getStringValue(14); }
    public long getBytesSent()                   { return getLongValue(15); }
    public KeyValuePairList getCookies()         { return getValues(16); }
    public long getRequestElapsedTime()          { return getLongValue(17); }
    public KeyValuePairList getRequestHeaders()  { return getValues(18); }
    public KeyValuePairList getResponseHeaders() { return getValues(19); }
    public String getRequestFirstLine()          { return getStringValue(20); }
    public long getAccessLogDatetime()           { return getLongValue(21); }
    public String getRemoteUser()                { return getStringValue(22); }
    public String getRemotePort()                { return getStringValue(23); }

    public static String getRequestStartTimeKey(int format)   { return nameAliases[format].aliases[0]; }
    public static String getUriPathKey(int format)            { return nameAliases[format].aliases[1]; }
    public static String getRequestMethodKey(int format)      { return nameAliases[format].aliases[2]; }
    public static String getQueryStringKey(int format)        { return nameAliases[format].aliases[3]; }
    public static String getRequestHostKey(int format)        { return nameAliases[format].aliases[4]; }
    public static String getRequestPortKey(int format)        { return nameAliases[format].aliases[5]; }
    public static String getRemoteHostKey(int format)         { return nameAliases[format].aliases[6]; }
    public static String getUserAgentKey(int format)          { return nameAliases[format].aliases[7]; }
    public static String getRequestProtocolKey(int format)    { return nameAliases[format].aliases[8]; }
    public static String getBytesReceivedKey(int format)      { return nameAliases[format].aliases[9]; }
    public static String getResponseCodeKey(int format)       { return nameAliases[format].aliases[10]; }
    public static String getElapsedTimeKey(int format)        { return nameAliases[format].aliases[11]; }
    public static String getDatetimeKey(int format)           { return nameAliases[format].aliases[12]; }
    public static String getSequenceKey(int format)           { return nameAliases[format].aliases[13]; }
    public static String getHostKey(int format)               { return nameAliases[format].aliases[14]; }
    public static String getUserDirKey(int format)            { return nameAliases[format].aliases[15]; }
    public static String getServerNameKey(int format)         { return nameAliases[format].aliases[16]; }
    public static String getTypeKey(int format)               { return nameAliases[format].aliases[17]; }
    public static String getRemoteIPKey(int format)           { return nameAliases[format].aliases[18]; }
    public static String getBytesSentKey(int format)          { return nameAliases[format].aliases[19]; }
    public static String getRequestElapsedTimeKey(int format) { return nameAliases[format].aliases[21]; }
    public static String getRequestFirstLineKey(int format)   { return nameAliases[format].aliases[24]; }
    public static String getAccessLogDatetimeKey(int format)  { return nameAliases[format].aliases[25]; }
    public static String getRemoteUserKey(int format)         { return nameAliases[format].aliases[26]; }
    public static String getRemotePortKey(int format)         { return nameAliases[format].aliases[27]; }

    public static String getCookieKey(int format, KeyValuePair kvp) {
        String cookieName = kvp.getKey();
         // We only support renaming JSON fields, not logstashCollector fields - so check that it's JSON before renaming field
        if (cookieMap.containsKey(cookieName) && (format == JSON_KEY)) {
            return cookieMap.get(cookieName);
        }
        return nameAliases[format].aliases[20] + "_" + cookieName;
    }

    public static String getRequestHeaderKey(int format, KeyValuePair kvp) {
        String requestHeader = kvp.getKey();
        if (requestHeaderMap.containsKey(requestHeader) && (format == JSON_KEY)) {
            return requestHeaderMap.get(requestHeader);
        }
        return nameAliases[format].aliases[22] + "_" + requestHeader;
    }

    public static String getResponseHeaderKey(int format, KeyValuePair kvp) {
        String responseHeader = kvp.getKey();
        if (responseHeaderMap.containsKey(responseHeader) && (format == JSON_KEY)) {
            return responseHeaderMap.get(responseHeader);
        }
        return nameAliases[format].aliases[23] + "_" + responseHeader;
    }

}
