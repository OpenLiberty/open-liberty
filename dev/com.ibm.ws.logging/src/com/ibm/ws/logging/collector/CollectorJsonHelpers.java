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

import java.text.SimpleDateFormat;

/**
 * CollectorJsonHelpers contains methods shared between CollectorjsonUtils and CollectorJsonUtils1_1
 */
public class CollectorJsonHelpers {

    private static String start_message_json = null;
    private static String start_message_json_1_1 = null;
    private static String start_trace_json = null;
    private static String start_trace_json_1_1 = null;
    private static String start_ffdc_json = null;
    private static String start_ffdc_json_1_1 = null;
    private static String start_accesslog_json = null;
    private static String start_accesslog_json_1_1 = null;
    private static String start_gc_json = null;
    private static String start_gc_json_1_1 = null;
    private static String middle_gc_json = null;
    private static String middle_gc_json_1_1 = null;

    protected static String getEventType(String source, String location) {
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

    protected static ThreadLocal<BurstDateFormat> dateFormatTL = new ThreadLocal<BurstDateFormat>() {
        @Override
        protected BurstDateFormat initialValue() {
            return new BurstDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        }
    };

    protected static boolean addToJSON(StringBuilder sb, String name, String value, boolean jsonEscapeName,
                                       boolean jsonEscapeValue, boolean trim, boolean isFirstField) {

        boolean b = addToJSON(sb, name, value, jsonEscapeName, jsonEscapeValue, trim, isFirstField, false);
        return b;
    }

    protected static boolean addToJSON(StringBuilder sb, String name, String value, boolean jsonEscapeName,
                                       boolean jsonEscapeValue, boolean trim, boolean isFirstField, boolean isNumber) {

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

        //If the type of the field is NUMBER, then do not add quotations around the value
        if (isNumber) {

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

    /**
     * Escape \b, \f, \n, \r, \t, ", \, / characters and appends to a string builder
     *
     * @param sb String builder to append to
     * @param s String to escape
     */
    protected static void jsonEscape3(StringBuilder sb, String s) {
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

    protected static boolean startMessageJson(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                              boolean isFirstField) {
        if (start_message_json != null) {
            if (!start_message_json.isEmpty()) {
                sb.append(start_message_json);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", "liberty_message", false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "wlpUserDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "serverName", serverName, false, false, false, isFirstField);

            start_message_json = temp.toString();
            sb.append(start_message_json);
        }

        return isFirstField;
    }

    protected static boolean startTraceJson(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                            boolean isFirstField) {
        if (start_trace_json != null) {
            if (!start_trace_json.isEmpty()) {
                sb.append(start_trace_json);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", "liberty_trace", false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "wlpUserDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "serverName", serverName, false, false, false, isFirstField);

            start_trace_json = temp.toString();
            sb.append(start_trace_json);
        }

        return isFirstField;
    }

    protected static boolean startFFDCJson(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                           boolean isFirstField) {
        if (start_ffdc_json != null) {
            if (!start_ffdc_json.isEmpty()) {
                sb.append(start_ffdc_json);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", "liberty_ffdc", false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "wlpUserDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "serverName", serverName, false, false, false, isFirstField);

            start_ffdc_json = temp.toString();
            sb.append(start_ffdc_json);
        }

        return isFirstField;
    }

    protected static boolean startAccessLogJson(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                                boolean isFirstField) {
        if (start_accesslog_json != null) {
            if (!start_accesslog_json.isEmpty()) {
                sb.append(start_accesslog_json);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", "liberty_ffdc", false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "wlpUserDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "serverName", serverName, false, false, false, isFirstField);

            start_accesslog_json = temp.toString();
            sb.append(start_accesslog_json);
        }

        return isFirstField;
    }

    protected static boolean startGenDataGCJson(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                                boolean isFirstField) {
        if (start_gc_json != null) {
            if (!start_gc_json.isEmpty()) {
                sb.append(start_gc_json);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", "liberty_ffdc", false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "wlpUserDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "serverName", serverName, false, false, false, isFirstField);

            start_gc_json = temp.toString();
            sb.append(start_gc_json);
        }

        return isFirstField;
    }

    protected static boolean startMessageJson1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                                 boolean isFirstField) {
        if (start_message_json_1_1 != null) {
            if (!start_message_json_1_1.isEmpty()) {
                sb.append(start_message_json_1_1);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", "liberty_message", false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_userDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_serverName", serverName, false, false, false, isFirstField);

            start_message_json_1_1 = temp.toString();
            sb.append(start_message_json_1_1);
        }

        return isFirstField;
    }

    protected static boolean startTraceJson1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                               boolean isFirstField) {
        if (start_trace_json_1_1 != null) {
            if (!start_trace_json_1_1.isEmpty()) {
                sb.append(start_trace_json_1_1);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", "liberty_trace", false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_userDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_serverName", serverName, false, false, false, isFirstField);

            start_trace_json_1_1 = temp.toString();
            sb.append(start_trace_json_1_1);
        }

        return isFirstField;
    }

    protected static boolean startFFDCJson1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                              boolean isFirstField) {
        if (start_ffdc_json_1_1 != null) {
            if (!start_ffdc_json_1_1.isEmpty()) {
                sb.append(start_ffdc_json_1_1);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", CollectorConstants.FFDC_EVENT_TYPE, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_userDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_serverName", serverName, false, false, false, isFirstField);

            start_ffdc_json_1_1 = temp.toString();
            sb.append(start_ffdc_json_1_1);
        }

        return isFirstField;
    }

    protected static boolean startAccessLogJson1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                                   boolean isFirstField) {
        if (start_accesslog_json_1_1 != null) {
            if (!start_accesslog_json_1_1.isEmpty()) {
                sb.append(start_accesslog_json_1_1);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", CollectorConstants.ACCESS_LOG_EVENT_TYPE, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_userDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_serverName", serverName, false, false, false, isFirstField);

            start_accesslog_json_1_1 = temp.toString();
            sb.append(start_accesslog_json_1_1);
        }

        return isFirstField;
    }

    protected static boolean startGenDataGCJson1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                                   boolean isFirstField) {
        if (start_gc_json_1_1 != null) {
            if (!start_gc_json_1_1.isEmpty()) {
                sb.append(start_gc_json_1_1);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", CollectorConstants.GC_EVENT_TYPE, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_userDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_serverName", serverName, false, false, false, isFirstField);

            start_gc_json_1_1 = temp.toString();
            sb.append(start_gc_json_1_1);
        }

        return isFirstField;
    }

    protected static boolean startGCJson1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName, long timestamp, String sequenceNum,
                                            boolean isFirstField) {
        String datetime = dateFormatTL.get().format(timestamp);

        /* Common fields for all event types */

        isFirstField = isFirstField & !addToJSON(sb, "ibm_datetime", datetime, false, false, false, isFirstField);

        middleGCJson1_1(sb, hostName, wlpUserDir, serverName, timestamp, sequenceNum, isFirstField);

        isFirstField = isFirstField & !addToJSON(sb, "ibm_sequence", sequenceNum, false, false, false, isFirstField);
        return isFirstField;
    }

    protected static boolean middleGCJson1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName, long timestamp, String sequenceNum,
                                             boolean isFirstField) {

        if (middle_gc_json_1_1 != null) {
            if (!middle_gc_json_1_1.isEmpty()) {
                if (!isFirstField) {
                    sb.append(",");
                }
                sb.append(middle_gc_json_1_1);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", CollectorConstants.GC_EVENT_TYPE, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_userDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "ibm_serverName", serverName, false, false, false, isFirstField);

            middle_gc_json_1_1 = temp.toString();
            sb.append(middle_gc_json_1_1);
        }

        return isFirstField;
    }

    protected static boolean startGCJson(StringBuilder sb, String hostName, String wlpUserDir, String serverName, long timestamp, String sequenceNum,
                                         boolean isFirstField) {
        String datetime = dateFormatTL.get().format(timestamp);

        /* Common fields for all event types */

        isFirstField = isFirstField & !addToJSON(sb, "datetime", datetime, false, false, false, isFirstField);

        middleGCJson(sb, hostName, wlpUserDir, serverName, timestamp, sequenceNum,
                     isFirstField);

        isFirstField = isFirstField & !addToJSON(sb, "sequence", sequenceNum, false, false, false, isFirstField);
        return isFirstField;
    }

    protected static boolean middleGCJson(StringBuilder sb, String hostName, String wlpUserDir, String serverName, long timestamp, String sequenceNum,
                                          boolean isFirstField) {

        if (middle_gc_json != null) {
            if (!middle_gc_json.isEmpty()) {
                if (!isFirstField) {
                    sb.append(",");
                }
                sb.append(middle_gc_json);
                isFirstField = false;
            }
        } else {
            StringBuilder temp = new StringBuilder(512);

            /* Common fields for all event types */

            isFirstField = isFirstField & !addToJSON(temp, "type", CollectorConstants.GC_EVENT_TYPE, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "host", hostName, false, false, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "wlpUserDir", wlpUserDir, false, true, false, isFirstField);
            isFirstField = isFirstField & !addToJSON(temp, "serverName", serverName, false, false, false, isFirstField);

            middle_gc_json = temp.toString();
            sb.append(middle_gc_json);
        }

        return isFirstField;
    }

    protected static String formatMessage(String message, int maxLength) {
        return (message.length() > maxLength && maxLength > 0) ? message.substring(0, maxLength) + "..." : message;
    }

    protected static String removeIBMTag(String s) {
        s = s.replace(LogFieldConstants.IBM_TAG, "");
        return s;
    }

    protected static StringBuilder addTagNameForVersion(StringBuilder sb) {

        sb.append(",\"tags\":");

        return sb;
    }

    protected static String jsonifyTags(String[] tags) {
        StringBuilder sb = new StringBuilder(512);

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

}
