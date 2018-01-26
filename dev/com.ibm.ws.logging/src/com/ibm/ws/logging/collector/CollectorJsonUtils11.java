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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.ws.health.center.data.HCGCData;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.LogFieldConstants;
import com.ibm.ws.logging.data.Pair;

/**
 *
 */
public class CollectorJsonUtils11 {

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

    public static String jsonifyEvent(Object event, String eventType, String serverName, String wlpUserDir, String serverHostName, String[] tags,
                                      int maxFieldLength) {

        if (eventType.equals(CollectorConstants.GC_EVENT_TYPE)) {

            return jsonifyGCEvent(serverHostName, wlpUserDir, serverName, (HCGCData) event, tags);

        } else if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE)) {

            return jsonifyTraceAndMessage(maxFieldLength, wlpUserDir, serverName, serverHostName, CollectorConstants.MESSAGES_LOG_EVENT_TYPE, event, tags);

        } else if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE)) {

            return jsonifyTraceAndMessage(maxFieldLength, wlpUserDir, serverName, serverHostName, CollectorConstants.TRACE_LOG_EVENT_TYPE, event, tags);

        } else if (eventType.equals(CollectorConstants.FFDC_EVENT_TYPE)) {

            return jsonifyFFDC(maxFieldLength, wlpUserDir, serverName, serverHostName, CollectorConstants.FFDC_EVENT_TYPE, event, tags);

        } else if (eventType.equals(CollectorConstants.ACCESS_LOG_EVENT_TYPE)) {

            return jsonifyAccess(-1, wlpUserDir, serverName, serverHostName, CollectorConstants.ACCESS_LOG_EVENT_TYPE, event, tags);

        }
        return "";

    }

    public static String jsonifyGCEvent(String hostName, String wlpUserDir, String serverName, HCGCData hcGCData, String[] tags) {
        String sequenceNum = hcGCData.getSequence();
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;

        sb.append("{");

        //                                           name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
        /* Common fields for all event types */
        isFirstField = addCommonFieldsGC(sb, hostName, wlpUserDir, serverName, hcGCData.getTime(), sequenceNum, isFirstField, CollectorConstants.GC_EVENT_TYPE);
        /* GC specific fields */
        isFirstField = isFirstField & !addToJSON(sb, "ibm_heap", String.valueOf((long) hcGCData.getHeap()), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_usedHeap", String.valueOf((long) hcGCData.getUsage()), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_maxHeap", String.valueOf(hcGCData.getMaxHeap()), false, false, false, isFirstField);
        isFirstField = isFirstField
                       & !addToJSON(sb, "ibm_duration", String.valueOf((long) hcGCData.getDuration() * 1000), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_gcType", hcGCData.getType(), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_reason", hcGCData.getReason(), false, false, false, isFirstField);

        if (tags != null) {
            addTagNameForVersion(sb).append(jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyFFDC(int maxFieldLength, String wlpUserDir,
                                      String serverName, String hostName, String eventType, Object event, String[] tags) {

        GenericData genData = (GenericData) event;
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        sb.append("{");

        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, isFirstField, eventType);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (key.equals(LogFieldConstants.IBM_STACKTRACE)) {

                    String formattedValue = formatMessage(value, maxFieldLength);
                    isFirstField = isFirstField & !addToJSON(sb, key, formattedValue, false, true, false, isFirstField, kvp);

                } else if (key.equals(LogFieldConstants.IBM_THREADID)) {

                    isFirstField = isFirstField
                                   & !addToJSON(sb, key, DataFormatHelper.padHexString(Integer.parseInt(value), 8), false, true, false, isFirstField, kvp);

                } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                    String datetime = dateFormatTL.get().format(Long.parseLong(value));
                    isFirstField = isFirstField & !addToJSON(sb, key, datetime, false, true, false, isFirstField, kvp);

                } else if (key.equals("correlationId") || key.equals("org") || key.equals("product") || key.equals("component") || key.equals("wsSourceThreadName")
                           || key.equals("levelValue") || key.equals("objectId")) {
                    //don't include it
                } else {

                    isFirstField = isFirstField & !addToJSON(sb, key, value, false, true, false, isFirstField, kvp);

                }
            }

        }

        if (tags != null) {
            addTagNameForVersion(sb).append(jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    public static String jsonifyAccess(int maxFieldLength, String wlpUserDir,
                                       String serverName, String hostName, String eventType, Object event, String[] tags) {

        GenericData genData = (GenericData) event;
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        sb.append("{");

        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, isFirstField, eventType);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (key.equals(LogFieldConstants.IBM_QUERYSTRING)) {

                    String jsonQueryString = value;
                    if (jsonQueryString != null) {
                        try {
                            jsonQueryString = URLDecoder.decode(jsonQueryString, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            // ignore, use the original value;
                        }
                    }
                    isFirstField = isFirstField & !addToJSON(sb, key, jsonQueryString, false, true, false, isFirstField, kvp);

                } else if (key.equals(LogFieldConstants.IBM_USERAGENT)) {

                    String userAgent = value;

                    if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
                        userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
                    }

                    isFirstField = isFirstField & !addToJSON(sb, key, userAgent, false, false, false, isFirstField, kvp);

                } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                    String datetime = dateFormatTL.get().format(Long.parseLong(value));
                    isFirstField = isFirstField & !addToJSON(sb, key, datetime, false, true, false, isFirstField, kvp);

                } else if (key.equals("correlationId") || key.equals("org") || key.equals("product") || key.equals("component") || key.equals("wsSourceThreadName")
                           || key.equals("levelValue") || key.equals("objectId")) {
                    //don't include it
                } else {

                    isFirstField = isFirstField & !addToJSON(sb, key, value, false, true, false, isFirstField, kvp);

                }
            }
        }

        if (tags != null) {
            addTagNameForVersion(sb).append(jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyTraceAndMessage(int maxFieldLength, String wlpUserDir,
                                                 String serverName, String hostName, String eventType, Object event, String[] tags) {

        GenericData genData = (GenericData) event;
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        sb.append("{");

        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, isFirstField, eventType);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (!(key.equals(LogFieldConstants.SEVERITY))) {

                    if (key.equals(LogFieldConstants.MESSAGE)) {

                        String formattedValue = formatMessage(value, maxFieldLength);
                        isFirstField = isFirstField & !addToJSON(sb, key, formattedValue, false, true, false, isFirstField, kvp);

                    } else if (key.equals(LogFieldConstants.IBM_THREADID)) {

                        isFirstField = isFirstField
                                       & !addToJSON(sb, key, DataFormatHelper.padHexString(Integer.parseInt(value), 8), false, true, false, isFirstField, kvp);

                    } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                        String datetime = dateFormatTL.get().format(Long.parseLong(value));
                        isFirstField = isFirstField & !addToJSON(sb, key, datetime, false, true, false, isFirstField, kvp);

                    } else if (key.equals("correlationId") || key.equals("org") || key.equals("product") || key.equals("component") || key.equals("wsSourceThreadName")
                               || key.equals("levelValue") || key.equals("objectId")) {
                        //don't include it
                    } else {

                        isFirstField = isFirstField & !addToJSON(sb, key, value, false, true, false, isFirstField, kvp);

                    }
                }
            }
        }

        if (tags != null) {
            addTagNameForVersion(sb).append(jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    static boolean addCommonFields(StringBuilder sb, String hostName, String wlpUserDir,
                                   String serverName, boolean isFirstField, String eventType) {

        /* Common fields for all event types */

        isFirstField = isFirstField & !addToJSON(sb, LogFieldConstants.TYPE, eventType, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, LogFieldConstants.HOST, hostName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, LogFieldConstants.IBM_USERDIR, wlpUserDir, false, true, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, LogFieldConstants.IBM_SERVERNAME, serverName, false, false, false, isFirstField);
        return isFirstField;
    }

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

    static boolean addToJSON(StringBuilder sb, String name, String value, boolean jsonEscapeName,
                             boolean jsonEscapeValue, boolean trim, boolean isFirstField) {

        boolean b = addToJSON(sb, name, value, jsonEscapeName, jsonEscapeValue, trim, isFirstField, null);
        return b;
    }

    static boolean addToJSON(StringBuilder sb, String name, String value, boolean jsonEscapeName,
                             boolean jsonEscapeValue, boolean trim, boolean isFirstField, KeyValuePair kvp) {

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

        if (kvp != null && !kvp.getKey().equals("ibm_datetime") && kvp.getType() == KeyValuePair.ValueTypes.NUMBER) {

            sb.append("\":");

            if (jsonEscapeValue)
                jsonEscape3(sb, value);
            else
                sb.append(value);

        } else {

            sb.append("\":\"");

            // escape value if requested
            if (jsonEscapeValue)
                jsonEscape3(sb, value);
            else
                sb.append(value);

            sb.append("\"");

        }
        return true;
    }

    /*
     * Method for handling the common code.
     * Returns true if the added filed is the first one on JSON.
     */
    static boolean addCommonFieldsGC(StringBuilder sb, String hostName, String wlpUserDir, String serverName, long timestamp, String sequenceNum,
                                     boolean isFirstField, String eventType) {
        String datetime = dateFormatTL.get().format(timestamp);

        /* Common fields for all event types */

        isFirstField = isFirstField & !addToJSON(sb, "ibm_datetime", datetime, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "type", eventType, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "host", hostName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_userDir", wlpUserDir, false, true, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_serverName", serverName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_sequence", sequenceNum, false, false, false, isFirstField);
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

    private static StringBuilder addTagNameForVersion(StringBuilder sb) {

        sb.append(",\"ibm_tags\":");

        return sb;
    }
}