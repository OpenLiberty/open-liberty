/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.ws.health.center.data.HCGCData;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.Pair;

/*
 * Utility class for converting events into JSON strings
 * Logstash and logmet collector use these methods for converting the relevant
 * events into json strings
 */
public class CollectorJsonUtils {

    public static final int MAX_USER_AGENT_LENGTH = 2048;
    public static final String LINE_SEPARATOR;
    static {
        LINE_SEPARATOR = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("line.separator");
            }
        });
    }
    private static ThreadLocal<BurstDateFormat> dateFormatTL = new ThreadLocal<BurstDateFormat>() {
        @Override
        protected BurstDateFormat initialValue() {
            return new BurstDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        }
    };

    //Date date = new Date(HCGCData.getTime());

    public static String getEventType(String source, String location) {
        if (source.equals(CollectorConstants.GC_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.GC_EVENT_TYPE;
        } else if (source.equals(CollectorConstants.MESSAGES_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.MESSAGES_LOG_EVENT_TYPE;
        } else if (source.endsWith(CollectorConstants.TRACE_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.TRACE_LOG_EVENT_TYPE;
        } else if (source.endsWith(CollectorConstants.FFDC_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.FFDC_EVENT_TYPE;
        } else if (source.endsWith(CollectorConstants.ACCESS_LOG_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.ACCESS_LOG_EVENT_TYPE;
        } else
            return "";
    }

    //public static String jsonifyEvent(Object event, String eventType, String serverName, String wlpUserDir, String serverHostName, String collectorVersion) {
    public static String jsonifyEvent(Object event, String eventType, String serverName, String wlpUserDir, String serverHostName, String collectorVersion, String[] tags,
                                      int maxFieldLength) {
        boolean isHigherVer = collectorVersion.startsWith("1.1");
        if (eventType.equals(CollectorConstants.GC_EVENT_TYPE)) {
            return jsonifyGCEvent(serverHostName, wlpUserDir, serverName, (HCGCData) event, isHigherVer, tags);
        } else if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE)) {
            return jsonifyEvent(serverHostName, wlpUserDir, serverName, event, isHigherVer, tags, maxFieldLength, CollectorConstants.MESSAGES_LOG_EVENT_TYPE);
        } else if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE)) {
            return jsonifyEvent(serverHostName, wlpUserDir, serverName, event, isHigherVer, tags, maxFieldLength, CollectorConstants.TRACE_LOG_EVENT_TYPE);
        } else if (eventType.equals(CollectorConstants.FFDC_EVENT_TYPE)) {
            return jsonifyEvent(serverHostName, wlpUserDir, serverName, event, isHigherVer, tags, maxFieldLength, CollectorConstants.FFDC_EVENT_TYPE);
        } else if (eventType.equals(CollectorConstants.ACCESS_LOG_EVENT_TYPE)) {
            return jsonifyEvent(serverHostName, wlpUserDir, serverName, event, isHigherVer, tags, 0, CollectorConstants.ACCESS_LOG_EVENT_TYPE);
        }
        return "";
    }

//    public static String jsonifyEvent(Object event, String eventType, String serverName, String wlpUserDir, String serverHostName, String collectorVersion, String[] tags,
//                                      int maxFieldLength) {
//        boolean isHigherVer = collectorVersion.startsWith("1.1");
//        if (eventType.equals(CollectorConstants.GC_EVENT_TYPE)) {
//            return jsonifyGCEvent(serverHostName, wlpUserDir, serverName, (HCGCData) event, isHigherVer, tags);
//        } else if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE)) {
//            return jsonifyMessageLogEvent(serverHostName, wlpUserDir, serverName, (MessageLogData) event, isHigherVer, tags, maxFieldLength);
//        } else if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE)) {
//            return jsonifyTraceLogEvent(serverHostName, wlpUserDir, serverName, event, isHigherVer, tags, maxFieldLength);
//        } else if (eventType.equals(CollectorConstants.FFDC_EVENT_TYPE)) {
//            return jsonifyFFDCEvent(serverHostName, wlpUserDir, serverName, (FFDCData) event, isHigherVer, tags, maxFieldLength);
//        } else if (eventType.equals(CollectorConstants.ACCESS_LOG_EVENT_TYPE)) {
//            return jsonifyAccessLogEvent(serverHostName, wlpUserDir, serverName, (AccessLogData) event, isHigherVer, tags);
//        }
//        return "";
//    }

    //Date date = new Date(HCGCData.getTime());

    public static String jsonifyGCEvent(String hostName, String wlpUserDir, String serverName, HCGCData hcGCData, boolean isHigherVer, String[] tags) {
        String sequenceNum = hcGCData.getSequence();
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;

        sb.append("{");

        //                                           name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
        /* Common fields for all event types */
        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, hcGCData.getTime(), sequenceNum, isHigherVer, isFirstField, CollectorConstants.GC_EVENT_TYPE);
        /* GC specific fields */
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_heap" : "heap", String.valueOf((long) hcGCData.getHeap()), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_usedHeap" : "usedHeap", String.valueOf((long) hcGCData.getUsage()), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_maxHeap" : "maxHeap", String.valueOf(hcGCData.getMaxHeap()), false, false, false, isFirstField);
        isFirstField = isFirstField
                       & !addToJSON(sb, isHigherVer ? "ibm_duration" : "duration", String.valueOf((long) hcGCData.getDuration() * 1000), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_gcType" : "gcType", hcGCData.getType(), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_reason" : "reason", hcGCData.getReason(), false, false, false, isFirstField);

        if (tags != null) {
            addTagNameForVersion(sb, isHigherVer).append(jsonifyTags(tags));
        }
        sb.append("}");

        return sb.toString();
    }

    private static void jsonify(GenericData genData, boolean isFirstField, StringBuilder sb, int maxFieldLength, String wlpUserDir,
                                String serverName, String hostName, String eventType) {

        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;

        isFirstField = addCommonFields2(sb, hostName, wlpUserDir, serverName, isFirstField, eventType);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;

                if (!(kvp.getKey().equals("severity"))) {

                    if (kvp.getKey().equals("message") || kvp.getKey().equals("ibm_stackTrace")) {

                        String formattedValue = formatMessage(kvp.getValue(), maxFieldLength);
                        isFirstField = isFirstField & !addToJSON(sb, kvp.getKey(), formattedValue, false, true, false, isFirstField);

                    } else if (kvp.getKey().equals("ibm_threadId")) {

                        isFirstField = isFirstField
                                       & !addToJSON(sb, kvp.getKey(), DataFormatHelper.padHexString(Integer.parseInt(kvp.getValue()), 8), false, true, false, isFirstField);

                    } else if (kvp.getKey().equals("ibm_queryString")) {

                        String jsonQueryString = kvp.getValue();
                        if (jsonQueryString != null) {
                            try {
                                jsonQueryString = URLDecoder.decode(jsonQueryString, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                // ignore, use the original value;
                            }
                        }
                        isFirstField = isFirstField & !addToJSON(sb, kvp.getKey(), jsonQueryString, false, true, false, isFirstField);

                    } else if (kvp.getKey().equals("ibm_userAgent")) {

                        String userAgent = kvp.getValue();
                        if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
                            userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
                        }
                        isFirstField = isFirstField & !addToJSON(sb, kvp.getKey(), userAgent, false, false, false, isFirstField);

                    } else if (kvp.getKey().equals("ibm_datetime")) {

                        String datetime = dateFormatTL.get().format(Long.parseLong(kvp.getValue()));
                        isFirstField = isFirstField & !addToJSON(sb, kvp.getKey(), datetime, false, true, false, isFirstField);

                    } else {

                        isFirstField = isFirstField & !addToJSON(sb, kvp.getKey(), kvp.getValue(), false, true, false, isFirstField);

                    }
                }
            }
        }
    }

    public static String jsonifyEvent(String hostName, String wlpUserDir, String serverName, Object event, boolean isHigherVer, String[] tags,
                                      int maxFieldLength, String eventType) {

        GenericData genData = (GenericData) event;
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;

        sb.append("{");

        jsonify(genData, isFirstField, sb, maxFieldLength, wlpUserDir, serverName, hostName, eventType);//CollectorConstants.MESSAGES_LOG_EVENT_TYPE);

        if (tags != null) {
            addTagNameForVersion(sb, isHigherVer).append(jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    static boolean addCommonFields2(StringBuilder sb, String hostName, String wlpUserDir,
                                    String serverName, boolean isFirstField, String eventType) {

        /* Common fields for all event types */

        isFirstField = isFirstField & !addToJSON(sb, "type", eventType, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "host", hostName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_userDir", wlpUserDir, false, true, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_serverName", serverName, false, false, false, isFirstField);
        return isFirstField;
    }

//    public static String jsonifyMessageLogEvent(String hostName, String wlpUserDir, String serverName, MessageLogData messageLogData, boolean isHigherVer, String[] tags,
//                                                int maxFieldLength) {
//
//        String sequenceNum = messageLogData.getSequence();
//        String message;
//
//        StringBuilder msgBldr = new StringBuilder();
//        msgBldr.append(messageLogData.getMessage());
//
//        if (messageLogData.getThrown() != null) {
//            String stackTrace = DataFormatHelper.throwableToString(messageLogData.getThrown());
//            if (stackTrace != null)
//                msgBldr.append(LINE_SEPARATOR).append(stackTrace);
//        }
//
//        message = formatMessage(msgBldr.toString(), maxFieldLength);
//
//        StringBuilder sb = new StringBuilder();
//        boolean isFirstField = true;
//
//        sb.append("{");
//       //name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
//       /* Common fields for all event types */
//        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, messageLogData.getDatetime(), sequenceNum, isHigherVer, isFirstField,
//                                       CollectorConstants.MESSAGES_LOG_EVENT_TYPE);
//        /* Message log specific fields */
//        isFirstField = isFirstField
//                       & !addToJSON(sb, isHigherVer ? "loglevel" : "severity", isHigherVer ? messageLogData.getLogLevelRaw() : messageLogData.getLogLevel(), false, false, false,
//                                    isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_messageId" : "messageId", messageLogData.getMessageID(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "module" : "loggerName", messageLogData.getLoggerName(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_methodName" : "methodName", messageLogData.getMethodName(), false, false, false, isFirstField);
//        isFirstField = isFirstField
//                       & !addToJSON(sb, isHigherVer ? "ibm_threadId" : "threadId", DataFormatHelper.padHexString(messageLogData.getThreadID(), 8), false, false, false,
//                                    isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_className" : "className", messageLogData.getClassName(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, "message", message, false, true, false, isFirstField);
//
//        if (tags != null) {
//            addTagNameForVersion(sb, isHigherVer).append(jsonifyTags(tags));
//        }
//        sb.append("}");
//
//        return sb.toString();
//    }

//    public static String jsonifyTraceLogEvent(String hostName, String wlpUserDir, String serverName, TraceLogData traceLogData, boolean isHigherVer, String[] tags,
//                                              int maxFieldLength) {
//
//        String sequenceNum = traceLogData.getSequence();
//        String message = formatMessage(traceLogData.getMessage(), maxFieldLength);
//
//        StringBuilder sb = new StringBuilder();
//        boolean isFirstField = true;
//
//        sb.append("{");
//
////      name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
//        /* Common fields for all event types */
//        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, traceLogData.getDatetime(), sequenceNum, isHigherVer, isFirstField,
//                                       CollectorConstants.TRACE_LOG_EVENT_TYPE);
//        /* Message log specific fields */
//        isFirstField = isFirstField
//                       & !addToJSON(sb, isHigherVer ? "loglevel" : "severity", isHigherVer ? traceLogData.getLogLevelRaw() : traceLogData.getLogLevel(), false, false, false,
//                                    isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "module" : "loggerName", traceLogData.getLoggerName(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_methodName" : "methodName", traceLogData.getMethodName(), false, false, false, isFirstField);
//        isFirstField = isFirstField
//                       & !addToJSON(sb, isHigherVer ? "ibm_threadId" : "threadId", DataFormatHelper.padHexString(traceLogData.getThreadID(), 8), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_className" : "className", traceLogData.getClassName(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, "message", message, false, true, false, isFirstField);
//
//        if (tags != null) {
//            addTagNameForVersion(sb, isHigherVer).append(jsonifyTags(tags));
//        }
//        sb.append("}");
//
//        return sb.toString();
//    }

//    public static String jsonifyFFDCEvent(String hostName, String wlpUserDir, String serverName, FFDCData ffdcData, boolean isHigherVer, String[] tags, int maxFieldLength) {
//
//        String sequenceNum = ffdcData.getSequence();
//
//        String stackTrace = formatMessage(ffdcData.getStackTrace(), maxFieldLength);
//
//        StringBuilder sb = new StringBuilder();
//        boolean isFirstField = true;
//
//        sb.append("{");
//
////      name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
//        /* Common fields for all event types */
//        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, ffdcData.getTimeStamp(), sequenceNum, isHigherVer, isFirstField, CollectorConstants.FFDC_EVENT_TYPE);
//        /* FFDC specific fields */
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_className" : "className", ffdcData.getClassName(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_exceptionName" : "exceptionName", ffdcData.getExceptionName(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_probeID" : "probeID", ffdcData.getProbeID(), false, false, false, isFirstField);
//        isFirstField = isFirstField
//                       & !addToJSON(sb, isHigherVer ? "ibm_threadId" : "threadId", DataFormatHelper.padHexString(ffdcData.getThreadID(), 8), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_stackTrace" : "stackTrace", stackTrace, false, true, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_objectDetails" : "objectDetails", ffdcData.getObjectDetails(), false, true, false, isFirstField);
//
//        if (tags != null) {
//            addTagNameForVersion(sb, isHigherVer).append(jsonifyTags(tags));
//        }
//        sb.append("}");
//
//        return sb.toString();
//    }

//    public static String jsonifyAccessLogEvent(String hostName, String wlpUserDir, String serverName, AccessLogData accessLogData, boolean isHigherVer, String[] tags) {
//        //Date date = new Date(accessLogData.getRequestStartTime());
//        String sequenceNum = accessLogData.getSequence();
//
//        StringBuilder sb = new StringBuilder();
//
//        boolean isFirstField = true;
//
//        sb.append("{");
//
////      name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
//        /* Common fields for all event types */
//        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, accessLogData.getTimestamp(), sequenceNum, isHigherVer, isFirstField,
//                                       CollectorConstants.ACCESS_LOG_EVENT_TYPE);
//        /* access request specific fields */
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_requestHost" : "requestHost", accessLogData.getRequestHost(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_requestPort" : "requestPort", accessLogData.getRequestPort(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_remoteHost" : "remoteHost", accessLogData.getRemoteHost(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_requestMethod" : "requestMethod", accessLogData.getRequestMethod(), false, false, false, isFirstField);
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_uriPath" : "uriPath", accessLogData.getURIPath(), false, false, false, isFirstField);
//        isFirstField = isFirstField
//                       & !addToJSON(sb, isHigherVer ? "ibm_requestProtocol" : "requestProtocol", accessLogData.getRequestProtocol(), false, false, false, isFirstField);
//
//        // Note: In ElasticsSearch it is not legal to have '.'(dot) in the field name.
//        //       ES will throw a parsing exception, if dot is present in the name.
//        //       So, we will use the whole queryString, instead of converting to JSON.
//        // String jsonQueryString = jsonifyQueryString(accessLogData.getQueryString());
//        String jsonQueryString = accessLogData.getQueryString();
//        if (jsonQueryString != null) {
//            try {
//                jsonQueryString = URLDecoder.decode(jsonQueryString, "UTF-8");
//            } catch (UnsupportedEncodingException e) {
//                // ignore, use the original value;
//            }
//
//        }
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_queryString" : "queryString", jsonQueryString, false, true, false, isFirstField);
//        isFirstField = isFirstField
//                       & !addToJSON(sb, isHigherVer ? "ibm_elapsedTime" : "elapsedTime", String.valueOf(accessLogData.getElapsedTime()), false, false, false, isFirstField);
//        isFirstField = isFirstField
//                       & !addToJSON(sb, isHigherVer ? "ibm_responseCode" : "responseCode", String.valueOf(accessLogData.getResponseCode()), false, false, false, isFirstField);
//        isFirstField = isFirstField
//                       & !addToJSON(sb, isHigherVer ? "ibm_bytesReceived" : "bytesReceived", String.valueOf(accessLogData.getResponseSize()), false, false, false, isFirstField);
//
//        String userAgent = accessLogData.getUserAgent();
//        if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
//            userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
//        }
//        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_userAgent" : "userAgent", userAgent, false, false, false, isFirstField);
//
//        if (tags != null) {
//            addTagNameForVersion(sb, isHigherVer).append(jsonifyTags(tags));
//        }
//        sb.append("}");
//
//        return sb.toString();
//    }

    /**
     * Escape \b, \f, \n, \r, \t, ", \, / characters and appends to a string builder
     *
     * @param sb String builder to append to
     * @param s String to escape
     */
    private static void jsonEscape3(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;

                // Fall through because we just need to add \ (escaped) before the character
                case '\\':
                case '\"':
                case '/':
                    sb.append("\\");
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
            }
        }
    }

    /*
     * returns true if name value pair was added to the string buffer
     */
    static boolean addToJSON(StringBuilder sb, String name, String value, boolean jsonEscapeName, boolean jsonEscapeValue, boolean trim, boolean isFirstField) {

        // if name or value is null just return
        if (name == null || value == null)
            return false;

        // add comma if isFirstField == false
        if (!isFirstField)
            sb.append(",");

        // trim value if requested
        if (trim)
            value = value.trim();

        sb.append("\"");
        // escape name if requested

        if (jsonEscapeName)
            jsonEscape3(sb, name);
        else
            sb.append(name);

        sb.append("\":\"");

        // escape value if requested
        if (jsonEscapeValue)
            jsonEscape3(sb, value);
        else
            sb.append(value);

        sb.append("\"");
        return true;
    }

    /*
     * Method for handling the common code.
     * Returns true if the added filed is the first one on JSON.
     */
    static boolean addCommonFields(StringBuilder sb, String hostName, String wlpUserDir, String serverName, long timestamp, String sequenceNum, boolean isHigherVer,
                                   boolean isFirstField, String eventType) {
        String datetime = dateFormatTL.get().format(timestamp);

        /* Common fields for all event types */

        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_datetime" : "datetime", datetime, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "type", eventType, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "host" : "hostName", hostName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_userDir" : "wlpUserDir", wlpUserDir, false, true, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_serverName" : "serverName", serverName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_sequence" : "sequence", sequenceNum, false, false, false, isFirstField);
        return isFirstField;
    }

    private static String jsonifyTags(String[] tags) {
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (int i = 0; i < tags.length; i++) {

            tags[i] = tags[i].trim();
            if (tags[i].contains(" ") || tags[i].contains("-")) {
                continue;
            }
            sb.append("\"");
            jsonEscape3(sb, tags[i]);
            sb.append("\"");
            if (i != tags.length - 1) {
                sb.append(",");
            }
        }

        //Check if have extra comma due to last tag being dropped for
        if (sb.toString().lastIndexOf(",") == sb.toString().length() - 1) {
            sb.delete(sb.toString().lastIndexOf(","), sb.toString().lastIndexOf(",") + 1);
        }
        sb.append("]");
        return sb.toString();
    }

    private static String formatMessage(String message, int maxLength) {
        return (message.length() > maxLength && maxLength > 0) ? message.substring(0, maxLength) + "..." : message;
    }

    private static StringBuilder addTagNameForVersion(StringBuilder sb, boolean isHigherVer) {
        if (isHigherVer) {
            sb.append(",\"ibm_tags\":");
        } else {
            sb.append(",\"tags\":");
        }
        return sb;
    }
//
//  *** Commented-out the UnUsed method ***
//
//    private static String jsonifyQueryString(String string) {
//        StringBuilder sb = new StringBuilder();
//        if (string == null || string.isEmpty()) {
//            sb.append("{}");
//        } else {
//            String[] queryStringArray;
//            if (string.indexOf('&') != -1) {
//                queryStringArray = string.split("\\&");
//            } else {
//                queryStringArray = new String[] { string };
//            }
//            sb.append("{");
//            // TODO: check if multiple-values for single-param-name, parsing is required
//            int i = 0;
//            for (String paramAndValue : queryStringArray) {
//                if (i > 0) {
//                    sb.append(',');
//                }
//                i += 1;
//
//                if (paramAndValue.indexOf('=') != -1) {
//                    String[] arry = paramAndValue.split("\\=");
//                    String name = arry[0];
//                    String value = arry[1];
//                    try {
//                        value = URLDecoder.decode(value, "UTF-8");
//                    } catch (UnsupportedEncodingException e) {
//                        // ignore, use the original value;
//                    }
//                    sb.append("\"").append(name).append("\":");
//                    sb.append("\"").append(jsonEscape2(value)).append("\"");
//                } else {
//                    sb.append("\"").append(paramAndValue).append("\":null");
//                }
//            }
//            sb.append("}");
//        }
//        String result = sb.toString();
//        return result;
//    }
}