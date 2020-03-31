/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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

/*
 * Utility class for converting events into JSON strings
 * Logstash and logmet collector use these methods for converting the relevant
 * events into json strings
 */
public class CollectorJsonUtils {

    public static final int MAX_USER_AGENT_LENGTH = 2048;

    public static String getEventType(String source, String location) {
        return CollectorJsonHelpers.getEventType(source, location);
    }

    /**
     * Method to return log event data in json format. If the collector version passed is greater than 1.0
     * then the jsonifyEvent call is passed to another version of CollectorJsonUtils.
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
    public static String jsonifyEvent(Object event, String eventType, String serverName, String wlpUserDir, String serverHostName, String collectorVersion, String[] tags,
                                      int maxFieldLength) {

        if (!collectorVersion.startsWith(("1.0"))) {
            if (collectorVersion.startsWith("1.1")) {
                return CollectorJsonUtils1_1.jsonifyEvent(event, eventType, serverName, wlpUserDir, serverHostName, tags, maxFieldLength);
            } else if (collectorVersion.startsWith("JSON")) {
                return CollectorJsonUtils_JSON.jsonifyEvent(event, eventType, serverName, wlpUserDir, serverHostName, tags, maxFieldLength);
            }
        } else {

            if (eventType.equals(CollectorConstants.GC_EVENT_TYPE)) {

                return jsonifyGCEvent(wlpUserDir, serverName, serverHostName, event, tags);

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
        }
        return "";
    }

    private static String jsonifyGCEvent(String wlpUserDir,
                                         String serverName, String hostName, Object event, String[] tags) {
        GCData gcData = (GCData) event;

        StringBuilder sb = CollectorJsonHelpers.startGCJson(hostName, wlpUserDir, serverName);

        CollectorJsonHelpers.addToJSON(sb, gcData.getHeapKey(), Long.toString(gcData.getHeap()), false, false, false, false, true);
        CollectorJsonHelpers.addToJSON(sb, gcData.getUsedHeapKey(), Long.toString(gcData.getUsedHeap()), false, false, false, false, true);
        CollectorJsonHelpers.addToJSON(sb, gcData.getMaxHeapKey(), Long.toString(gcData.getMaxHeap()), false, false, false, false, true);

        long duration = gcData.getDuration() * 1000;
        CollectorJsonHelpers.addToJSON(sb, gcData.getDurationKey(), Long.toString(duration), false, false, false, false, true);

        CollectorJsonHelpers.addToJSON(sb, gcData.getGcTypeKey(), gcData.getGcType(), false, false, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, gcData.getReasonKey(), gcData.getReason(), false, false, false, false, false);

        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(gcData.getDatetime());
        CollectorJsonHelpers.addToJSON(sb, gcData.getDatetimeKey(), datetime, false, false, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, gcData.getSequenceKey(), gcData.getSequence(), false, false, false, false, false);

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

        if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE))
            sb = CollectorJsonHelpers.startMessageJson(hostName, wlpUserDir, serverName);
        if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE))
            sb = CollectorJsonHelpers.startTraceJson(hostName, wlpUserDir, serverName);

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

        CollectorJsonHelpers.addToJSON(sb, logData.getMessageKey(), formattedValue.toString(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getThreadIdKey(), DataFormatHelper.padHexString(logData.getThreadId(), 8), false, true, false, false);
        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(logData.getDatetime());
        CollectorJsonHelpers.addToJSON(sb, logData.getDatetimeKey(), datetime, false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getModuleKey(), logData.getModule(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getMessageIdKey(), logData.getMessageId(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getSeverityKey(), logData.getSeverity(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getMethodNameKey(), logData.getMethodName(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getClassNameKey(), logData.getClassName(), false, true, false, false);
        CollectorJsonHelpers.addToJSON(sb, logData.getSequenceKey(), logData.getSequence(), false, true, false, false);
        KeyValuePairList kvpl = logData.getExtensions();
        if (kvpl != null) {
            if (kvpl.getKey().equals(LogFieldConstants.EXTENSIONS_KVPL)) {
                ArrayList<KeyValuePair> extensions = kvpl.getList();
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

    private static String jsonifyFFDC(int maxFieldLength, String wlpUserDir,
                                      String serverName, String hostName, Object event, String[] tags) {

        FFDCData ffdcData = (FFDCData) event;

        StringBuilder sb = CollectorJsonHelpers.startFFDCJson(hostName, wlpUserDir, serverName);

        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(ffdcData.getDatetime());
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getDatetimeKey(), datetime, false, true, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, ffdcData.getMessageKey(), ffdcData.getMessage(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getClassNameKey(), ffdcData.getClassName(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getExceptionNameKey(), ffdcData.getExceptionName(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getProbeIdKey(), ffdcData.getProbeId(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getThreadIdKey(), DataFormatHelper.padHexString((int) ffdcData.getThreadId(), 8), false, true, false, false, false);

        String formattedValue = CollectorJsonHelpers.formatMessage(ffdcData.getStacktrace(), maxFieldLength);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getStacktraceKey(), formattedValue, false, true, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, ffdcData.getObjectDetailsKey(), ffdcData.getObjectDetails(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, ffdcData.getSequenceKey(), ffdcData.getSequence(), false, true, false, false, false);

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyAccess(String wlpUserDir,
                                        String serverName, String hostName, Object event, String[] tags) {

        AccessLogData accessLogData = (AccessLogData) event;

        StringBuilder sb = CollectorJsonHelpers.startAccessLogJson(hostName, wlpUserDir, serverName);

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getUriPathKey(), accessLogData.getUriPath(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRequestMethodKey(), accessLogData.getRequestMethod(), false, true, false, false, false);

        String jsonQueryString = accessLogData.getQueryString();
        if (jsonQueryString != null) {
            try {
                jsonQueryString = URLDecoder.decode(jsonQueryString, LogFieldConstants.UTF_8);
            } catch (UnsupportedEncodingException e) {
                // ignore, use the original value;
            }

        }
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getQueryStringKey(), jsonQueryString, false, true, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRequestHostKey(), accessLogData.getRequestHost(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRequestPortKey(), accessLogData.getRequestPort(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRemoteHostKey(), accessLogData.getRemoteHost(), false, true, false, false, false);

        String userAgent = accessLogData.getUserAgent();

        if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
            userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
        }

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getUserAgentKey(), userAgent, false, false, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getRequestProtocolKey(), accessLogData.getRequestProtocol(), false, true, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getBytesReceivedKey(), Long.toString(accessLogData.getBytesReceived()), false, true, false, false, true);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getResponseCodeKey(), Integer.toString(accessLogData.getResponseCode()), false, true, false, false, true);
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getElapsedTimeKey(), Long.toString(accessLogData.getElapsedTime()), false, true, false, false, true);

        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(accessLogData.getDatetime());
        CollectorJsonHelpers.addToJSON(sb, accessLogData.getDatetimeKey(), datetime, false, true, false, false, false);

        CollectorJsonHelpers.addToJSON(sb, accessLogData.getSequenceKey(), accessLogData.getSequence(), false, true, false, false, false);

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
        StringBuilder sb = CollectorJsonHelpers.startAuditJson(hostName, wlpUserDir, serverName);

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
                     *
                     * Note: we'll expect any external/thirdparty/additional source to be using IBM_* keys.
                     * This method is to parse and format into logstash_1.0 expected formatting.
                     */
                    if (key.equals(LogFieldConstants.IBM_DATETIME) || key.equals("loggingEventTime")) {
                        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(kvp.getLongValue());
                        CollectorJsonHelpers.addToJSON(sb, LogFieldConstants.DATETIME, datetime, false, true, false, false, false);
                    } else if (key.equals(LogFieldConstants.IBM_SEQUENCE) || key.equals("loggingSequenceNumber")) {
                        CollectorJsonHelpers.addToJSON(sb, LogFieldConstants.SEQUENCE, kvp.getStringValue(), false, false, false, false, !kvp.isString());
                    } else if (key.equals(LogFieldConstants.IBM_THREADID)) {
                        CollectorJsonHelpers.addToJSON(sb, LogFieldConstants.THREADID, DataFormatHelper.padHexString(kvp.getIntValue(), 8), false, true, false, false, false);
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

        sb.append(",\"tags\":");

        return sb;
    }

}
