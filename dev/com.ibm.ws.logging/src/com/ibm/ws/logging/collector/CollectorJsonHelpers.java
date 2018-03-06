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
    private static String message_event_type_field_json = "\"type\":\"liberty_message\"";
    private static String trace_event_type_field_json = "\"type\":\"liberty_trace\"";
    private static String accesslog_event_type_field_json = "\"type\":\"liberty_accesslog\"";
    private static String ffdc_event_type_field_json = "\"type\":\"liberty_ffdc\"";
    private static String gc_event_type_field_json = "\"type\":\"liberty_gc\"";

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

    private static String unchanging_fields_json = null;

    private static void addUnchangingFields(StringBuilder sb, String hostName, String wlpUserDir, String serverName) {
        if (unchanging_fields_json != null) {
            sb.append(unchanging_fields_json);
        } else {
            StringBuilder temp = new StringBuilder(512);
            addToJSON(temp, "host", hostName, false, false, false, false);
            addToJSON(temp, "wlpUserDir", wlpUserDir, false, true, false, false);
            addToJSON(temp, "serverName", serverName, false, false, false, false);
            unchanging_fields_json = temp.toString();
            sb.append(unchanging_fields_json);
        }
    }

    private static String unchanging_fields_json_1_1 = null;

    private static void addUnchangingFields1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName) {
        if (unchanging_fields_json_1_1 != null) {
            sb.append(unchanging_fields_json_1_1);
        } else {
            StringBuilder temp = new StringBuilder(512);
            addToJSON(temp, "host", hostName, false, false, false, false);
            addToJSON(temp, "ibm_userDir", wlpUserDir, false, true, false, false);
            addToJSON(temp, "ibm_serverName", serverName, false, false, false, false);
            unchanging_fields_json_1_1 = temp.toString();
            sb.append(unchanging_fields_json_1_1);
        }
    }

    protected static StringBuilder startMessageJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_message_json != null) {
            sb.append(start_message_json);
        } else {
            sb.append("{");
            sb.append(message_event_type_field_json);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            start_message_json = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startTraceJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_trace_json != null) {
            sb.append(start_trace_json);
        } else {
            sb.append("{");
            sb.append(trace_event_type_field_json);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            start_trace_json = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startFFDCJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_ffdc_json != null) {
            sb.append(start_ffdc_json);
        } else {
            sb.append("{");
            sb.append(ffdc_event_type_field_json);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            start_ffdc_json = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startAccessLogJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_accesslog_json != null) {
            sb.append(start_accesslog_json);
        } else {
            sb.append("{");
            sb.append(accesslog_event_type_field_json);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            start_accesslog_json = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startGCJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_gc_json != null) {
            sb.append(start_gc_json);
        } else {
            sb.append("{");
            sb.append(gc_event_type_field_json);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            start_gc_json = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startMessageJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_message_json_1_1 != null) {
            sb.append(start_message_json_1_1);
        } else {
            sb.append("{");
            sb.append(message_event_type_field_json);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            start_message_json_1_1 = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startTraceJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_trace_json_1_1 != null) {
            sb.append(start_trace_json_1_1);
        } else {
            sb.append("{");
            sb.append(trace_event_type_field_json);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            start_trace_json_1_1 = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startFFDCJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_ffdc_json_1_1 != null) {
            sb.append(start_ffdc_json_1_1);
        } else {
            sb.append("{");
            sb.append(ffdc_event_type_field_json);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            start_ffdc_json_1_1 = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startAccessLogJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_accesslog_json_1_1 != null) {
            sb.append(start_accesslog_json_1_1);
        } else {
            sb.append("{");
            sb.append(accesslog_event_type_field_json);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            start_accesslog_json_1_1 = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startGCJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (start_gc_json_1_1 != null) {
            sb.append(start_gc_json_1_1);
        } else {
            sb.append("{");
            sb.append(gc_event_type_field_json);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            start_gc_json_1_1 = sb.toString();
        }

        return sb;
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
        StringBuilder sb = new StringBuilder(64);

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
