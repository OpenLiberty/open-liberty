/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.collector;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.ws.logging.source.MessageLogData;
import com.ibm.ws.logging.source.TraceLogData;

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
    private static ThreadLocal<SimpleDateFormat> dateFormatTL = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
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
        if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE)) {
            return jsonifyMessageLogEvent(serverHostName, wlpUserDir, serverName, (MessageLogData) event, isHigherVer, tags, maxFieldLength);
        } else if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE)) {
            return jsonifyTraceLogEvent(serverHostName, wlpUserDir, serverName, (TraceLogData) event, isHigherVer, tags, maxFieldLength);
        }
        return "";
    }

    //Date date = new Date(HCGCData.getTime());

    public static String jsonifyMessageLogEvent(String hostName, String wlpUserDir, String serverName, MessageLogData messageLogData, boolean isHigherVer, String[] tags,
                                                int maxFieldLength) {

        Date date = new Date(messageLogData.getDatetime());

        String sequenceNum = messageLogData.getSequence();
        String message;

        StringBuilder msgBldr = new StringBuilder();
        msgBldr.append(messageLogData.getMessage());

        if (messageLogData.getThrown() != null) {
            String stackTrace = DataFormatHelper.throwableToString(messageLogData.getThrown());
            if (stackTrace != null)
                msgBldr.append(LINE_SEPARATOR).append(stackTrace);
        }

        message = formatMessage(msgBldr.toString(), maxFieldLength);

        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;

        sb.append("{");
//      name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
        /* Common fields for all event types */
        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, date, sequenceNum, isHigherVer, isFirstField);
        /* Message log specific fields */
        isFirstField = isFirstField
                       & !addToJSON(sb, isHigherVer ? "loglevel" : "severity", isHigherVer ? messageLogData.getLogLevelRaw() : messageLogData.getLogLevel(), false, false, false,
                                    isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_messageId" : "messageId", messageLogData.getMessageID(), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "module" : "loggerName", messageLogData.getLoggerName(), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_methodName" : "methodName", messageLogData.getMethodName(), false, false, false, isFirstField);
        isFirstField = isFirstField
                       & !addToJSON(sb, isHigherVer ? "ibm_threadId" : "threadId", DataFormatHelper.padHexString(messageLogData.getThreadID(), 8), false, false, false,
                                    isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_className" : "className", messageLogData.getClassName(), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "message", message, false, true, false, isFirstField);

        if (tags != null) {
            addTagNameForVersion(sb, isHigherVer).append(jsonifyTags(tags));
        }
        sb.append("}");

        return sb.toString();
    }

    public static String jsonifyTraceLogEvent(String hostName, String wlpUserDir, String serverName, TraceLogData traceLogData, boolean isHigherVer, String[] tags,
                                              int maxFieldLength) {

        Date date = new Date(traceLogData.getDatetime());

        String sequenceNum = traceLogData.getSequence();
        String message = formatMessage(traceLogData.getMessage(), maxFieldLength);

        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;

        sb.append("{");

//      name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
        /* Common fields for all event types */
        isFirstField = addCommonFields(sb, hostName, wlpUserDir, serverName, date, sequenceNum, isHigherVer, isFirstField);

        /* Message log specific fields */
        isFirstField = isFirstField
                       & !addToJSON(sb, isHigherVer ? "loglevel" : "severity", isHigherVer ? traceLogData.getLogLevelRaw() : traceLogData.getLogLevel(), false, false, false,
                                    isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "module" : "loggerName", traceLogData.getLoggerName(), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_methodName" : "methodName", traceLogData.getMethodName(), false, false, false, isFirstField);
        isFirstField = isFirstField
                       & !addToJSON(sb, isHigherVer ? "ibm_threadId" : "threadId", DataFormatHelper.padHexString(traceLogData.getThreadID(), 8), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_className" : "className", traceLogData.getClassName(), false, false, false, isFirstField);
        isFirstField = isFirstField & !addToJSON(sb, "message", message, false, true, false, isFirstField);

        if (tags != null) {
            addTagNameForVersion(sb, isHigherVer).append(jsonifyTags(tags));
        }
        sb.append("}");

        return sb.toString();
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
     * Method for handling the common code.
     * Returns true if the added filed is the first one on JSON.
     */
    static boolean addCommonFields(StringBuilder sb, String hostName, String wlpUserDir, String serverName, Date date, String sequenceNum, boolean isHigherVer,
                                   boolean isFirstField) {
        String datetime = dateFormatTL.get().format(date);

        /* Common fields for all event types */

        isFirstField = isFirstField & !addToJSON(sb, isHigherVer ? "ibm_datetime" : "datetime", datetime, false, false, false, isFirstField);
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

            tags[i] = jsonEscape2(tags[i].trim());
            if (tags[i].contains(" ") || tags[i].contains("-")) {
                continue;
            }
            sb.append("\"").append(jsonEscape2(tags[i].trim())).append("\"");
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
