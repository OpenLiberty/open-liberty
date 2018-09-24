/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.collector;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.ws.logging.data.AccessLogData;
import com.ibm.ws.logging.data.FFDCData;
import com.ibm.ws.logging.data.GCData;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.LogTraceData;

/**
 *
 */
public class CollectorJsonUtils1_1 {

    public static final int MAX_USER_AGENT_LENGTH = 2048;

    public static String getEventType(String source, String location) {
        return CollectorJsonHelpers.getEventType(source, location);
    }

    /**
     * Method to return log event data in json format. This method is for collector version greater than 1.0
     *
     * @param event The object originating from logging source which contains necessary fields
     * @param eventType The type of event
     * @param servername The name of the server
     * @param wlpUserDir The name of wlp user directory
     * @param serverHostName The name of server host
     * @param collectorVersion The version number
     * @param tags An array of tags
     * @param maxFieldLength The max character length of strings
     */
    public static String jsonifyEvent(Object event, String eventType, String serverName, String wlpUserDir, String serverHostName, String[] tags,
                                      int maxFieldLength) {

        if (eventType.equals(CollectorConstants.GC_EVENT_TYPE)) {

            if (event instanceof GCData) {

                return jsonifyGCEvent(wlpUserDir, serverName, serverHostName, event, tags);

            } else {

                return jsonifyGCEvent(-1, wlpUserDir, serverName, serverHostName, CollectorConstants.GC_EVENT_TYPE, event, tags);

            }

        } else if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE)) {

            return jsonifyTraceAndMessage(maxFieldLength, wlpUserDir, serverName, serverHostName, CollectorConstants.MESSAGES_LOG_EVENT_TYPE, event, tags);

        } else if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE)) {

            return jsonifyTraceAndMessage(maxFieldLength, wlpUserDir, serverName, serverHostName, CollectorConstants.TRACE_LOG_EVENT_TYPE, event, tags);

        } else if (eventType.equals(CollectorConstants.FFDC_EVENT_TYPE)) {

            return jsonifyFFDC(maxFieldLength, wlpUserDir, serverName, serverHostName, event, tags);

        } else if (eventType.equals(CollectorConstants.ACCESS_LOG_EVENT_TYPE)) {

            return jsonifyAccess(wlpUserDir, serverName, serverHostName, event, tags);

        } else if (eventType.equals(CollectorConstants.AUDIT_LOG_EVENT_TYPE)) {

            return jsonifyAudit(wlpUserDir, serverName, serverHostName, event, tags);

        }
        return "";

    }

    private static String jsonifyGCEvent(int maxFieldLength, String wlpUserDir,
                                         String serverName, String hostName, String eventType, Object event, String[] tags) {
        GenericData genData = (GenericData) event;
        KeyValuePair[] pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;

        StringBuilder sb = CollectorJsonHelpers.startGCJson1_1(hostName, wlpUserDir, serverName);

        for (KeyValuePair p : pairs) {

            if (p != null && !p.isList()) {

                kvp = p;
                key = kvp.getKey();

                if (key.equals(LogFieldConstants.IBM_DURATION)) {

                    long duration = kvp.getLongValue() * 1000;
                    CollectorJsonHelpers.addToJSON(sb, key, Long.toString(duration), false, true, false, false, true);

                } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                    String datetime = CollectorJsonHelpers.dateFormatTL.get().format(kvp.getLongValue());
                    CollectorJsonHelpers.addToJSON(sb, key, datetime, false, true, false, false, false);

                } else {

                    String value = null;
                    if (kvp.isInteger()) {
                        value = Integer.toString(kvp.getIntValue());
                    } else if (kvp.isLong()) {
                        value = Long.toString(kvp.getLongValue());
                    } else {
                        value = kvp.getStringValue();
                    }
                    CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, false, !kvp.isString());

                }
            }
        }

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyGCEvent(String wlpUserDir, String serverName, String hostName, Object event, String[] tags) {
        GCData gcData = (GCData) event;

        StringBuilder sb = CollectorJsonHelpers.startGCJson1_1(hostName, wlpUserDir, serverName);

        CollectorJsonHelpers.addToJSON(sb, gcData.getHeapKey1_1(), Long.toString(gcData.getHeap()), false, false, false, false, true);
        CollectorJsonHelpers.addToJSON(sb, gcData.getUsedHeapKey1_1(), Long.toString(gcData.getUsedHeap()), false, false, false, false, true);
        CollectorJsonHelpers.addToJSON(sb, gcData.getMaxHeapKey1_1(), Long.toString(gcData.getMaxHeap()), false, false, false, false, true);

        long duration = gcData.getDuration() * 1000;
        CollectorJsonHelpers.addToJSON(sb, gcData.getDurationKey1_1(), Long.toString(duration), false, false, false, false, true);

        CollectorJsonHelpers.addToJSON(sb, gcData.getGcTypeKey1_1(), gcData.getGcType(), false, false, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, gcData.getReasonKey1_1(), gcData.getReason(), false, false, false, false, false);

        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(gcData.getDatetime());
        CollectorJsonHelpers.addToJSON(sb, gcData.getDatetimeKey1_1(), datetime, false, false, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, gcData.getSequenceKey1_1(), gcData.getSequence(), false, false, false, false, false);

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyFFDC(int maxFieldLength, String wlpUserDir,
                                      String serverName, String hostName, Object event, String[] tags) {

        FFDCData ffdcData = (FFDCData) event;
        StringBuilder sb = CollectorJsonHelpers.startFFDCJson1_1(hostName, wlpUserDir, serverName);

        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(ffdcData.getDatetime());
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getDatetimeKey1_1(), datetime, false, true, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, ffdcData.getMessageKey1_1(), ffdcData.getMessage(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getClassNameKey1_1(), ffdcData.getClassName(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getExceptionNameKey1_1(), ffdcData.getExceptionName(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getProbeIdKey1_1(), ffdcData.getProbeId(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getThreadIdKey1_1(), DataFormatHelper.padHexString((int) ffdcData.getThreadId(), 8), false, true, false, false, false);

        String formattedValue = CollectorJsonHelpers.formatMessage(ffdcData.getStacktrace(), maxFieldLength);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getStacktraceKey1_1(), formattedValue, false, true, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, ffdcData.getObjectDetailsKey1_1(), ffdcData.getObjectDetails(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getSequenceKey1_1(), ffdcData.getSequence(), false, true, false, false, false);

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    public static String jsonifyAccess(String wlpUserDir, String serverName, String hostName, Object event, String[] tags) {

        AccessLogData accessLogData = (AccessLogData) event;

        StringBuilder sb = CollectorJsonHelpers.startAccessLogJson1_1(hostName, wlpUserDir, serverName);

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getUriPathKey1_1(), accessLogData.getUriPath(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRequestMethodKey1_1(), accessLogData.getRequestMethod(), false, true, false, false, false);

        String jsonQueryString = accessLogData.getQueryString();
        if (jsonQueryString != null) {
            try {
                jsonQueryString = URLDecoder.decode(jsonQueryString, LogFieldConstants.UTF_8);
            } catch (UnsupportedEncodingException e) {
                // ignore, use the original value;
            }
        }
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getQueryStringKey1_1(), jsonQueryString, false, true, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRequestHostKey1_1(), accessLogData.getRequestHost(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRequestPortKey1_1(), accessLogData.getRequestPort(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRemoteHostKey1_1(), accessLogData.getRemoteHost(), false, true, false, false, false);

        String userAgent = accessLogData.getUserAgent();

        if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
            userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
        }

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getUserAgentKey1_1(), userAgent, false, false, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRequestProtocolKey1_1(), accessLogData.getRequestProtocol(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getBytesReceivedKey1_1(), Long.toString(accessLogData.getBytesReceived()), false, true, false, false, true);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getResponseCodeKey1_1(), Integer.toString(accessLogData.getResponseCode()), false, true, false, false, true);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getElapsedTimeKey1_1(), Long.toString(accessLogData.getElapsedTime()), false, true, false, false, true);

        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(accessLogData.getDatetime());
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getDatetimeKey1_1(), datetime, false, true, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getSequenceKey1_1(), accessLogData.getSequence(), false, true, false, false, false);

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyTraceAndMessage(int maxFieldLength, String wlpUserDir,
                                                 String serverName, String hostName, String eventType, Object event, String[] tags) {

        LogTraceData logData = (LogTraceData) event;
        StringBuilder sb = null;

        ArrayList<KeyValuePair> extensions = null;
        KeyValuePairList kvpl = null;

        if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE))
            sb = CollectorJsonHelpers.startMessageJson1_1(hostName, wlpUserDir, serverName);
        if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE))
            sb = CollectorJsonHelpers.startTraceJson1_1(hostName, wlpUserDir, serverName);

        String message = logData.getMessage();
        String loglevel = logData.getLoglevel();
        if (loglevel != null) {
            if (loglevel.equals("ENTRY") || loglevel.equals("EXIT")) {
                message = CollectorJsonHelpers.jsonRemoveSpace(message);
            }
        }

        StringBuilder formattedValue = new StringBuilder(CollectorJsonHelpers.formatMessage(message, maxFieldLength));
        String throwable = logData.getThrowable();
        if (throwable != null) {
            formattedValue.append(CollectorJsonHelpers.LINE_SEPARATOR).append(throwable);
        }

        CollectorJsonHelpers.addToJSON(sb, logData.getMessageKey1_1(), formattedValue.toString(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getThreadIdKey1_1(), DataFormatHelper.padHexString(logData.getThreadId(), 8), false, true, false, false);
        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(logData.getDatetime());
        CollectorJsonHelpers.addToJSON(sb, logData.getDatetimeKey1_1(), datetime, false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getMessageIdKey1_1(), logData.getMessageId(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getModuleKey1_1(), logData.getModule(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getLoglevelKey1_1(), logData.getLoglevel(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getMethodNameKey1_1(), logData.getMethodName(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getClassNameKey1_1(), logData.getClassName(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getSequenceKey1_1(), logData.getSequence(), false, true, false, false);

        kvpl = logData.getExtensions();
        if (kvpl != null) {
            if (kvpl.getKey().equals(LogFieldConstants.EXTENSIONS_KVPL)) {
                extensions = kvpl.getList();
                for (KeyValuePair k : extensions) {
                    String extKey = k.getKey();
                    if (extKey.endsWith(CollectorJsonHelpers.INT_SUFFIX)) {
                        CollectorJsonHelpers.addToJSON(sb, extKey, Integer.toString(k.getIntValue()), false, true, false, false, true);
                    } else if (extKey.endsWith(CollectorJsonHelpers.FLOAT_SUFFIX)) {
                        CollectorJsonHelpers.addToJSON(sb, extKey, Float.toString(k.getFloatValue()), false, true, false, false, true);
                    } else if (extKey.endsWith(CollectorJsonHelpers.LONG_SUFFIX)) {
                        CollectorJsonHelpers.addToJSON(sb, extKey, Long.toString(k.getLongValue()), false, true, false, false, true);
                    } else if (extKey.endsWith(CollectorJsonHelpers.BOOL_SUFFIX)) {
                        CollectorJsonHelpers.addToJSON(sb, extKey, Boolean.toString(k.getBooleanValue()), false, true, false, false, true);
                    } else {
                        CollectorJsonHelpers.addToJSON(sb, extKey, k.getStringValue(), false, true, false, false, false);
                    }
                }
            }
        }

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    public static String jsonifyAudit(String wlpUserDir, String serverName, String hostName, Object event, String[] tags) {
        GenericData genData = (GenericData) event;
        KeyValuePair[] pairs = genData.getPairs();
        String key = null;
        StringBuilder sb = CollectorJsonHelpers.startAuditJson1_1(hostName, wlpUserDir, serverName);

        for (KeyValuePair kvp : pairs) {

            if (kvp != null) {
                //Logic for non-KeyValuePairList type Pairs
                if (!kvp.isList()) {

                    key = kvp.getKey();

                    /*
                     * Explicitly parse for ibm_datetime/loggingEventTime for special processing.
                     *
                     * Explicitly parse for ibm_sequence/loggingSequenceNumber for special processing.
                     *
                     * Explicitly parse for ibm_threadid for special processing.
                     *
                     * Audit is currently not using the logging constants for the datetime and sequence keys,
                     * we need to format the json output with the appropriate logging values for the keys.
                     *
                     * Parse the rest of audit GDO KVP - They are strings.
                     */
                    if (key.equals(LogFieldConstants.IBM_DATETIME) || key.equals("loggingEventTime")) {
                        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(kvp.getLongValue());
                        CollectorJsonHelpers.addToJSON(sb, LogFieldConstants.IBM_DATETIME, datetime, false, true, false, false, false);
                    } else if (key.equals(LogFieldConstants.IBM_SEQUENCE) || key.equals("loggingSequenceNumber")) {
                        CollectorJsonHelpers.addToJSON(sb, LogFieldConstants.IBM_SEQUENCE, kvp.getStringValue(), false, false, false, false, !kvp.isString());
                    } else if (key.equals(LogFieldConstants.IBM_THREADID)) {
                        CollectorJsonHelpers.addToJSON(sb, LogFieldConstants.IBM_THREADID, DataFormatHelper.padHexString(kvp.getIntValue(), 8), false, true, false, false, false);
                    } else {
                        CollectorJsonHelpers.addToJSON(sb, "ibm_audit_" + key, kvp.getStringValue(), false, false, false, false, !kvp.isString());
                    }

                } //There shouldn't be any list items from Audit's Generic Data object
            }

        }
        sb.append("}");
        return sb.toString();
    }

    private static StringBuilder addTagNameForVersion(StringBuilder sb) {

        sb.append(",\"ibm_tags\":");

        return sb;
    }

}