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

import com.ibm.ws.logging.data.KeyValuePair;

/**
 * CollectorJsonHelpers contains methods shared between CollectorjsonUtils and CollectorJsonUtils1_1
 */
public class CollectorJsonHelpers {

    private final static String TRUE_BOOL = "true";
    private final static String FALSE_BOOL = "false";
    public final static String INT_SUFFIX = "_int";
    public final static String FLOAT_SUFFIX = "_float";
    private final static String BOOL_SUFFIX = "_bool";

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

    protected static boolean addCommonFields(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                             boolean isFirstField, String eventType) {

        isFirstField = isFirstField & !addToJSON(sb, "type", eventType, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "hostName", hostName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "wlpUserDir", wlpUserDir, false, true, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "serverName", serverName, false, false, false, isFirstField);

        return isFirstField;
    }

    protected static boolean addCommonFields1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName,
                                                boolean isFirstField, String eventType) {

        isFirstField = isFirstField & !addToJSON(sb, "type", eventType, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "host", hostName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_userDir", wlpUserDir, false, true, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "ibm_serverName", serverName, false, false, false, isFirstField);

        return isFirstField;
    }

    protected static boolean addCommonFieldsGC1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName, long timestamp, String sequenceNum,
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

    protected static boolean addCommonFieldsGC(StringBuilder sb, String hostName, String wlpUserDir, String serverName, long timestamp, String sequenceNum,
                                               boolean isFirstField, String eventType) {
        String datetime = dateFormatTL.get().format(timestamp);

        /* Common fields for all event types */

        isFirstField = isFirstField & !addToJSON(sb, "datetime", datetime, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "type", eventType, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "hostName", hostName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "wlpUserDir", wlpUserDir, false, true, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "serverName", serverName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "sequence", sequenceNum, false, false, false, isFirstField);
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

    protected static boolean checkExtSuffixValidity(KeyValuePair kvp) {
        boolean isValidExt = false;
        String key = kvp.getKey();
        String value = kvp.getValue();

        if (key.endsWith(INT_SUFFIX)) {
            isValidExt = verifyIntValue(value);
        } else if (key.endsWith(FLOAT_SUFFIX)) {
            isValidExt = verifyFloatValue(value);
        } else if (key.endsWith(BOOL_SUFFIX)) {
            if (value.equals(TRUE_BOOL) || value.equals(FALSE_BOOL)) {
                isValidExt = true;
            }
        } else {
            isValidExt = true;
        }
        return isValidExt;
    }

    public static boolean verifyIntValue(String value) {
        boolean isValid = true;
        char[] arr = value.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            if (i == 0) {
                if (arr[i] == '-' && arr.length > 1) {
                    continue;
                }
            }
            if (arr[i] >= '0' && arr[i] <= '9') {
                continue;
            } else {
                isValid = false;
                break;
            }
        }
        return isValid;
    }

    public static boolean verifyFloatValue(String s) {
        boolean isValid = true;
        boolean decimalFlag = false;
        char[] arr = s.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            if (i == 0) {
                // If the string has more than 1 characters, and the first position in the string has a -, then it is okay.
                if (arr[i] == '-' && arr.length > 1) {
                    continue;
                }
            }
            // If a '.' is found
            if (arr[i] == '.') {
                // If the decimal is not in the first spot, not in the last spot and the onlt decimal found
                if (i > 0 && i < arr.length - 1 && decimalFlag == false) {
                    // Check if there is a digit before the decimal
                    if (arr[i - 1] >= '0' && arr[i - 1] <= '9') {
                        // Set decimal flag
                        decimalFlag = true;
                        continue;
                    } else {
                        return false;
                    }
                } else {
                    //if the decimal conditions are violated, then the number is invalid
                    return false;
                }
            }
            // Check if the current character is a digit
            if (arr[i] >= '0' && arr[i] <= '9') {
                continue;
            } else {
                return false;
            }

        }

        return isValid;
    }

    protected static boolean checkIfExtNum(String extKey) {
        if (extKey.endsWith(CollectorJsonHelpers.INT_SUFFIX) || extKey.endsWith(CollectorJsonHelpers.FLOAT_SUFFIX)) {
            return true;
        } else {
            return false;
        }
    }

}
