/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class HpelJsonFormatter extends HpelPlainFormatter {
    private long headerDatetime = 0;
    private final AtomicLong seq = new AtomicLong();

    @Override
    public String formatRecord(RepositoryLogRecord record, Locale locale) {
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        String loggerName = record.getLoggerName();
        String methodName = record.getSourceMethodName();
        String message = record.getFormattedMessage();
        if (record.getStackTrace() != null) {
            message = message.concat("\n");
            message = message.concat(record.getStackTrace());
        }
        String className = record.getSourceClassName();
        String logLevel = mapLevelToType(record);
        long date = record.getMillis();
        String datetime = dateFormatGmt.format(date);
        String sequence = date + "_" + String.format("%013X", seq.incrementAndGet());
        String messageID = record.getMessageID();
        String logType = null;
        StringBuilder threadSB = new StringBuilder();
        Map<String, String> extensions = record.getExtensions();
        formatThreadID(record, threadSB);
        String threadID = threadSB.toString();
        Object[] parms = record.getParameters();
        String rawMessage = record.getRawMessage();
        return jsonify(loggerName, methodName, message, className, logLevel,
                       datetime, messageID, threadID, sequence,
                       extensions, logType, rawMessage, parms);

    }

    private static String jsonify(String loggerName, String methodName,
                                  String message, String className, String level, String datetime,
                                  String messageID, String threadID,
                                  String sequence, Map<String, String> extensions, String logType, String rawMessage, Object[] parms) {

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean isFirstField = true;

        //                                           name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
        isFirstField = isFirstField & !addToJSON(sb, "datetime", datetime, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "threadId", threadID, false, false, true, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "message", message, false, true, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "severity", level, false, false, true, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "messageId", messageID, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "sequence", sequence, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "loggerName", loggerName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "methodName", methodName, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "className", className, false, false, false, isFirstField);
        if (rawMessage != null && rawMessage.equals(message)) {
            // formatted message is the same as the raw message meaning no parms were added during message formatting
            // check for unused parms and if any are present put them in unusedParms json field
            if (parms != null) {
                isFirstField = isFirstField & !addListToJSON(sb, "unusedParms", parms, true, isFirstField);
            }
        }
        if (extensions != null) {
            for (Map.Entry<String, String> entry : extensions.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null)
                    isFirstField = isFirstField & !addToJSON(sb, key, value, true, true, true, isFirstField);
            }
        }
        sb.append("}");
        //for fat test case
        return sb.toString();
    }

    @Override
    protected String appendUnusedParms(String message, Object[] args) {
        return "";
    }

    @Override
    public String getFooter() {
        return "";
    }

    private static String jsonEscape2(String s) {

        String r = s;
        r = r.replace("\\", "\\\\"); // \ -> \\
        r = r.replace("\"", "\\\""); // " -> \"
        r = r.replace("/", "\\/"); // / -> \/
        r = r.replace("\b", "\\b"); // esc-b -> \b
        r = r.replace("\f", "\\f"); // esc-f -> \f
        r = r.replace("\n", "\\n"); // esc-n -> \n
        r = r.replace("\r", "\\r"); // esc-r -> \r
        r = r.replace("\t", "\\t"); // esc-t -> \t
        r = r.replace("_", "\\u005f"); // _ -> \u005f

        return r;
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
        // escape value if requested
        if (jsonEscapeValue)
            value = jsonEscape2(value);
        // escape name if requested
        if (jsonEscapeName)
            name = jsonEscape2(name);
        // append name : value to sb
        sb.append("\"" + name + "\":\"").append(value).append("\"");
        return true;
    }

    /*
     * returns true if name value pair was added to the string buffer
     */
    static boolean addListToJSON(StringBuilder sb, String name, Object[] list, boolean jsonEscapeValues, boolean isFirstField) {
        if (name == null || list == null)
            return false;

        // add comma if isFirstField == false
        if (!isFirstField)
            sb.append(",");

        sb.append("\"" + name + "\":[");
        boolean firstParm = true;
        for (Object element : list) {
            if (firstParm)
                sb.append("\"");
            else
                sb.append(",\"");
            if (element != null) {
                if (jsonEscapeValues)
                    sb.append(jsonEscape2(element.toString()));
                else
                    sb.append(element.toString());
            }
            sb.append("\"");
            firstParm = false;
        }
        sb.append("]");
        return true;
    }

    @Override
    public String[] getHeader() {
        StringBuilder result = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (customHeader.length > 0) {
            for (CustomHeaderLine line : customHeader) {
                String formattedLine = line.formatLine(headerProps);
                if (!first && formattedLine != null) {
                    result.append("\n");
                }
                if (formattedLine != null) {
                    result.append(formattedLine);
                }
                first = false;
            }
        } else {
            for (String prop : headerProps.stringPropertyNames()) {
                if (!first) {
                    result.append("\n");
                }
                result.append(prop + " = " + headerProps.getProperty(prop));
                first = false;
            }
        }
        boolean isFirstField = true;
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        String sequence = headerDatetime + "_" + String.format("%013X", seq.incrementAndGet());
        String headerDateTime = dateFormatGmt.format(headerDatetime);
        sb.append("{");
        isFirstField = isFirstField & !addToJSON(sb, "datetime", headerDateTime, false, false, true, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "sequence", sequence, false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "message", result.toString(), false, true, true, isFirstField);
        sb.append("}");
        String s[] = { sb.toString() };
        return s;
    }

    @Override
    public void setStartDatetime(long datetime) {
        this.headerDatetime = datetime;
    }

}
