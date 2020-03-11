/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.collector;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.ibm.ws.logging.data.AccessLogData;
import com.ibm.ws.logging.data.AuditData;
import com.ibm.ws.logging.data.FFDCData;
import com.ibm.ws.logging.data.JSONObject;
import com.ibm.ws.logging.data.JSONObject.JSONObjectBuilder;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.ws.logging.data.Pair;

/**
 * CollectorJsonHelpers contains methods shared between CollectorjsonUtils and CollectorJsonUtils1_1
 */
public class CollectorJsonHelpers {

    private static String startMessageJson = null;
    private static String startMessageJsonFields = null;
    private static String startTraceJson = null;
    private static String startTraceJsonFields = null;
    private static String startFFDCJson = null;
    private static String startFFDCJsonFields = null;
    private static String startAccessLogJson = null;
    private static String startAccessLogJsonFields = null;
    private static String startGCJson = null;
    private static String startAuditJson = null;
    private static String startAuditJsonFields = null;
    private static final String TYPE_FIELD_KEY = "\"type";
    private static final String TYPE_FIELD_PREPPEND = "\":\"";
    private static final String TYPE_FIELD_APPEND = "\"";
    private static final String MESSAGE_JSON_TYPE_FIELD = TYPE_FIELD_PREPPEND + CollectorConstants.MESSAGES_LOG_EVENT_TYPE + TYPE_FIELD_APPEND;
    private static final String TRACE_JSON_TYPE_FIELD = TYPE_FIELD_PREPPEND + CollectorConstants.TRACE_LOG_EVENT_TYPE + TYPE_FIELD_APPEND;
    private static final String ACCESS_JSON_TYPE_FIELD = TYPE_FIELD_PREPPEND + CollectorConstants.ACCESS_LOG_EVENT_TYPE + TYPE_FIELD_APPEND;
    private static final String FFDC_JSON_TYPE_FIELD = TYPE_FIELD_PREPPEND + CollectorConstants.FFDC_EVENT_TYPE + TYPE_FIELD_APPEND;
    private static final String GC_JSON_TYPE_FIELD = TYPE_FIELD_PREPPEND + CollectorConstants.GC_EVENT_TYPE + TYPE_FIELD_APPEND;
    private static final String AUDIT_JSON_TYPE_FIELD = TYPE_FIELD_PREPPEND + CollectorConstants.AUDIT_LOG_EVENT_TYPE + TYPE_FIELD_APPEND;
    private static String unchangingFieldsJson = null;
    public final static String TRUE_BOOL = "true";
    public final static String FALSE_BOOL = "false";
    public final static String INT_SUFFIX = "_int";
    public final static String FLOAT_SUFFIX = "_float";
    public final static String BOOL_SUFFIX = "_bool";
    public final static String LONG_SUFFIX = "_long";
    public static final String LINE_SEPARATOR;
    public static final String OMIT_FIELDS_STRING = "@@@OMIT@@@";

    static {
        LINE_SEPARATOR = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("line.separator");
            }
        });
    }

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
        } else if (source.contains(CollectorConstants.AUDIT_LOG_SOURCE)) {
            return CollectorConstants.AUDIT_LOG_EVENT_TYPE;
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
                                       boolean jsonEscapeValue, boolean trim, boolean isFirstField, boolean isQuoteless) {

        // if name or value is null just return
        if (name == null || value == null)
            return false;

        // if the field name is to be omitted for the event type
        if (name.equals(OMIT_FIELDS_STRING))
            return false;

        // add comma if isFirstField == false
        if (!isFirstField) {
            sb.append(",");
        }

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
        if (isQuoteless) {

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

    public static void updateFieldMappings() {
        startMessageJsonFields = null;
        startTraceJsonFields = null;
        startAccessLogJsonFields = null;
        startAuditJsonFields = null;
        startFFDCJsonFields = null;
    }

    private static void addUnchangingFields(StringBuilder sb, String hostName, String wlpUserDir, String serverName) {
        if (unchangingFieldsJson == null) {
            StringBuilder temp = new StringBuilder(512);
            addToJSON(temp, "hostName", hostName, false, false, false, false);
            addToJSON(temp, "wlpUserDir", wlpUserDir, false, true, false, false);
            addToJSON(temp, "serverName", serverName, false, false, false, false);
            unchangingFieldsJson = temp.toString();
        }
        sb.append(unchangingFieldsJson);
    }

    protected static StringBuilder startMessageJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startMessageJson != null) {
            sb.append(startMessageJson);
        } else {
            sb.append("{");
            sb.append(TYPE_FIELD_KEY);
            sb.append(MESSAGE_JSON_TYPE_FIELD);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startMessageJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startTraceJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startTraceJson != null) {
            sb.append(startTraceJson);
        } else {
            sb.append("{");
            sb.append(TYPE_FIELD_KEY);
            sb.append(TRACE_JSON_TYPE_FIELD);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startTraceJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startFFDCJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startFFDCJson != null) {
            sb.append(startFFDCJson);
        } else {
            sb.append("{");
            sb.append(TYPE_FIELD_KEY);
            sb.append(FFDC_JSON_TYPE_FIELD);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startFFDCJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startAccessLogJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startAccessLogJson != null) {
            sb.append(startAccessLogJson);
        } else {
            sb.append("{");
            sb.append(TYPE_FIELD_KEY);
            sb.append(ACCESS_JSON_TYPE_FIELD);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startAccessLogJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startGCJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startGCJson != null) {
            sb.append(startGCJson);
        } else {
            sb.append("{");
            sb.append(TYPE_FIELD_KEY);
            sb.append(GC_JSON_TYPE_FIELD);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startGCJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startAuditJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(2048);

        if (startAuditJson != null) {
            sb.append(startAuditJson);
        } else {
            sb.append("{");
            sb.append(TYPE_FIELD_KEY);
            sb.append(AUDIT_JSON_TYPE_FIELD);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);
            startAuditJson = sb.toString();
        }

        return sb;
    }

    protected static JSONObjectBuilder startAuditJsonFields(String hostName, String wlpUserDir, String serverName) {
        JSONObjectBuilder jsonBuilder = new JSONObject.JSONObjectBuilder();
        String tempStartFields = startAuditJsonFields;

        if (tempStartFields != null) {
            jsonBuilder.addFields(tempStartFields);
        } else {
            //@formatter:off
            jsonBuilder.addField(AuditData.getTypeKeyJSON(), CollectorConstants.AUDIT_LOG_EVENT_TYPE, false, false)
            .addField(AuditData.getHostKeyJSON(), hostName, false, false)
            .addField(AuditData.getUserDirKeyJSON(), wlpUserDir, false, true)
            .addField(AuditData.getServerNameKeyJSON(), serverName, false, false);
            //@formatter:on
            startAuditJsonFields = jsonBuilder.toString();
        }
        return jsonBuilder;
    }

    protected static JSONObjectBuilder startMessageJsonFields(String hostName, String wlpUserDir, String serverName) {
        JSONObjectBuilder jsonBuilder = new JSONObject.JSONObjectBuilder();
        String tempStartFields = startMessageJsonFields;

        if (tempStartFields != null)
            jsonBuilder.addFields(tempStartFields);
        else {
            //@formatter:off
            jsonBuilder.addField(LogTraceData.getTypeKeyJSON(true), CollectorConstants.MESSAGES_LOG_EVENT_TYPE, false, false)
            .addField(LogTraceData.getHostKeyJSON(true), hostName, false, false)
            .addField(LogTraceData.getUserDirKeyJSON(true), wlpUserDir, false, true)
            .addField(LogTraceData.getServerNameKeyJSON(true), serverName, false, false);
            //@formatter:on
            startMessageJsonFields = jsonBuilder.toString();
        }
        return jsonBuilder;
    }

    protected static JSONObjectBuilder startTraceJsonFields(String hostName, String wlpUserDir, String serverName) {
        JSONObjectBuilder jsonBuilder = new JSONObject.JSONObjectBuilder();
        String tempStartFields = startTraceJsonFields;

        if (tempStartFields != null)
            jsonBuilder.addFields(tempStartFields);
        else {
            //@formatter:off
            jsonBuilder.addField(LogTraceData.getTypeKeyJSON(false), CollectorConstants.TRACE_LOG_EVENT_TYPE, false, false)
            .addField(LogTraceData.getHostKeyJSON(false), hostName, false, false)
            .addField(LogTraceData.getUserDirKeyJSON(false), wlpUserDir, false, true)
            .addField(LogTraceData.getServerNameKeyJSON(false), serverName, false, false);
            //@formatter:on
            startTraceJsonFields = jsonBuilder.toString();
        }
        return jsonBuilder;
    }

    protected static JSONObjectBuilder startFFDCJsonFields(String hostName, String wlpUserDir, String serverName) {
        JSONObjectBuilder jsonBuilder = new JSONObject.JSONObjectBuilder();
        String tempStartFields = startFFDCJsonFields;

        if (tempStartFields != null)
            jsonBuilder.addFields(tempStartFields);
        else {
            //@formatter:off
            jsonBuilder.addField(FFDCData.getTypeKeyJSON(), CollectorConstants.FFDC_EVENT_TYPE, false, false)
            .addField(FFDCData.getHostKeyJSON(), hostName, false, false)
            .addField(FFDCData.getUserDirKeyJSON(), wlpUserDir, false, true)
            .addField(FFDCData.getServerNameKeyJSON(), serverName, false, false);
            //@formatter:on
            startFFDCJsonFields = jsonBuilder.toString();
        }
        return jsonBuilder;
    }

    protected static JSONObjectBuilder startAccessLogJsonFields(String hostName, String wlpUserDir, String serverName) {
        JSONObjectBuilder jsonBuilder = new JSONObject.JSONObjectBuilder();
        String tempStartFields = startAccessLogJsonFields;

        if (tempStartFields != null)
            jsonBuilder.addFields(tempStartFields);
        else {
            //@formatter:off
            jsonBuilder.addField(AccessLogData.getTypeKeyJSON(), CollectorConstants.ACCESS_LOG_EVENT_TYPE, false, false)
            .addField(AccessLogData.getHostKeyJSON(), hostName, false, false)
            .addField(AccessLogData.getUserDirKeyJSON(), wlpUserDir, false, true)
            .addField(AccessLogData.getServerNameKeyJSON(), serverName, false, false);
            //@formatter:on
            startAccessLogJsonFields = jsonBuilder.toString();
        }
        return jsonBuilder;
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

    protected static String jsonRemoveSpace(String s) {
        StringBuilder sb = new StringBuilder();
        boolean isLine = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                sb.append(c);
                isLine = true;
            } else if (c == ' ' && isLine) {
            } else if (isLine && c != ' ') {
                isLine = false;
                sb.append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    protected static String getLogLevel(ArrayList<Pair> pairs) {
        KeyValuePair kvp = null;
        String loglevel = null;
        for (Pair p : pairs) {
            if (p instanceof KeyValuePair) {
                kvp = (KeyValuePair) p;
                if (kvp.getKey().equals(LogFieldConstants.LOGLEVEL)) {
                    loglevel = kvp.getStringValue();
                    break;
                }
            }
        }
        return loglevel;
    }

    public static void handleExtensions(KeyValuePairList extensions, String extKey, String extValue) {
        extKey = LogFieldConstants.EXT_PREFIX + extKey;
        if (extKey.indexOf('_', 4) != -1) {
            if (extKey.endsWith(CollectorJsonHelpers.INT_SUFFIX)) {
                try {
                    extensions.addKeyValuePair(extKey, Integer.parseInt(extValue));
                } catch (NumberFormatException e) {
                }
            } else if (extKey.endsWith(CollectorJsonHelpers.FLOAT_SUFFIX)) {
                try {
                    extensions.addKeyValuePair(extKey, Float.parseFloat(extValue));
                } catch (NumberFormatException e) {
                }
            } else if (extKey.endsWith(CollectorJsonHelpers.BOOL_SUFFIX)) {
                if (extValue.toLowerCase().trim().equals(TRUE_BOOL)) {
                    extensions.addKeyValuePair(extKey, true);
                } else if (extValue.toLowerCase().trim().equals(FALSE_BOOL)) {
                    extensions.addKeyValuePair(extKey, false);
                }
            } else if (extKey.endsWith(CollectorJsonHelpers.LONG_SUFFIX)) {
                try {
                    extensions.addKeyValuePair(extKey, Long.parseLong(extValue));
                } catch (NumberFormatException e) {
                }
            } else {
                extensions.addKeyValuePair(extKey, extValue);
            }
        } else {
            extensions.addKeyValuePair(extKey, extValue);
        }
    }
}