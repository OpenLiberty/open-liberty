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

import java.util.ArrayList;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.ws.logging.data.AccessLogData;
import com.ibm.ws.logging.data.AccessLogDataFormatter;
import com.ibm.ws.logging.data.AuditData;
import com.ibm.ws.logging.data.FFDCData;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.JSONObject.JSONObjectBuilder;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.LogTraceData;

/**
 *
 */
public class CollectorJsonUtils_JSON {

    public static final int MAX_USER_AGENT_LENGTH = 2048;

    public static String getEventType(String source, String location) {
        return CollectorJsonHelpers.getEventType(source, location);
    }

    /**
     * Method to return log event data in json format. This method is for collector version greater than 1.0
     *
     * @param event            The object originating from logging source which contains necessary fields
     * @param eventType        The type of event
     * @param servername       The name of the server
     * @param wlpUserDir       The name of wlp user directory
     * @param serverHostName   The name of server host
     * @param collectorVersion The version number
     * @param tags             An array of tags
     * @param maxFieldLength   The max character length of strings
     */
    public static String jsonifyEvent(Object event, String eventType, String serverName, String wlpUserDir, String serverHostName, String[] tags,
                                      int maxFieldLength) {

        if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE)) {

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

    private static String jsonifyFFDC(int maxFieldLength, String wlpUserDir,
                                      String serverName, String hostName, Object event, String[] tags) {

        FFDCData ffdcData = (FFDCData) event;
        int jsonKey = FFDCData.KEYS_JSON;

        JSONObjectBuilder jsonBuilder = CollectorJsonHelpers.startFFDCJsonFields(jsonKey);

        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(ffdcData.getDatetime());
        String formattedValue = CollectorJsonHelpers.formatMessage(ffdcData.getStacktrace(), maxFieldLength);

        //@formatter:off
        jsonBuilder.addField(FFDCData.getDatetimeKey(jsonKey), datetime, false, true)
                   .addField(FFDCData.getMessageKey(jsonKey), ffdcData.getMessage(), false, true)
                   .addField(FFDCData.getClassNameKey(jsonKey), ffdcData.getClassName(), false, true)
                   .addField(FFDCData.getExceptionNameKey(jsonKey), ffdcData.getExceptionName(), false, true)
                   .addField(FFDCData.getProbeIdKey(jsonKey), ffdcData.getProbeId(), false, true)
                   .addField(FFDCData.getThreadIdKey(jsonKey), DataFormatHelper.padHexString((int) ffdcData.getThreadId(), 8), false, true)
                   .addField(FFDCData.getStacktraceKey(jsonKey), formattedValue, false, true)
                   .addField(FFDCData.getObjectDetailsKey(jsonKey), ffdcData.getObjectDetails(), false, true)
                   .addField(FFDCData.getSequenceKey(jsonKey), ffdcData.getSequence(), false, true);
        //@formatter:on

        if (tags != null) {
            jsonBuilder.addPreformattedField("ibm_tags", CollectorJsonHelpers.jsonifyTags(tags));
        }

        return jsonBuilder.build().toString();
    }

    public static String jsonifyAccess(String wlpUserDir, String serverName, String hostName, Object event, String[] tags) {

        AccessLogData accessLogData = (AccessLogData) event;
        JSONObjectBuilder jsonBuilder = CollectorJsonHelpers.startAccessLogJsonFields(AccessLogData.KEYS_JSON);

        AccessLogDataFormatter[] formatters = accessLogData.getFormatters();

        // Only one of these will not be null - there is only one formatter per event. If both are not null, we made a mistake earlier in AccessLogSource
        if (formatters[1] != null)
            formatters[1].populate(jsonBuilder, accessLogData);
        else if (formatters[0] != null)
            formatters[0].populate(jsonBuilder, accessLogData);
        else
            throw new RuntimeException("There is no formatter available for this event.");

        if (tags != null) {
            jsonBuilder.addPreformattedField("ibm_tags", CollectorJsonHelpers.jsonifyTags(tags));
        }
        return jsonBuilder.build().toString();
    }

    private static String jsonifyTraceAndMessage(int maxFieldLength, String wlpUserDir,
                                                 String serverName, String hostName, String eventType, Object event, String[] tags) {

        LogTraceData logData = (LogTraceData) event;
        JSONObjectBuilder jsonBuilder = null;
        boolean isMessageEvent = eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE);
        int jsonKey = LogTraceData.KEYS_JSON;

        ArrayList<KeyValuePair> extensions = null;
        KeyValuePairList kvpl = null;

        if (isMessageEvent) {
            jsonBuilder = CollectorJsonHelpers.startMessageJsonFields(jsonKey);
        }
        if (!isMessageEvent) {
            jsonBuilder = CollectorJsonHelpers.startTraceJsonFields(jsonKey);
        }

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

        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(logData.getDatetime());

        //@formatter:off
        jsonBuilder.addField(LogTraceData.getMessageKey(jsonKey, isMessageEvent), formattedValue.toString(), false, true)
        .addField(LogTraceData.getThreadIdKey(jsonKey, isMessageEvent), DataFormatHelper.padHexString(logData.getThreadId(), 8), false, true)
        .addField(LogTraceData.getDatetimeKey(jsonKey, isMessageEvent), datetime, false, true)
        .addField(LogTraceData.getMessageIdKey(jsonKey, isMessageEvent), logData.getMessageId(), false, true)
        .addField(LogTraceData.getModuleKey(jsonKey, isMessageEvent), logData.getModule(), false, true)
        .addField(LogTraceData.getLoglevelKey(jsonKey, isMessageEvent), logData.getLoglevel(), false, true)
        .addField(LogTraceData.getMethodNameKey(jsonKey, isMessageEvent), logData.getMethodName(), false, true)
        .addField(LogTraceData.getClassNameKey(jsonKey, isMessageEvent), logData.getClassName(), false, true)
        .addField(LogTraceData.getSequenceKey(jsonKey, isMessageEvent), logData.getSequence(), false, true);
        //@formatter:on

        kvpl = logData.getExtensions();
        if (kvpl != null) {
            if (kvpl.getKey().equals(LogFieldConstants.EXTENSIONS_KVPL)) {
                extensions = kvpl.getList();
                for (KeyValuePair k : extensions) {
                    String extKey = k.getKey();
                    if (extKey.endsWith(CollectorJsonHelpers.INT_SUFFIX)) {
                        jsonBuilder.addField(LogTraceData.getExtensionNameKeyJSON(isMessageEvent, extKey), k.getIntValue(), false);
                    } else if (extKey.endsWith(CollectorJsonHelpers.FLOAT_SUFFIX)) {
                        jsonBuilder.addField(LogTraceData.getExtensionNameKeyJSON(isMessageEvent, extKey), k.getFloatValue(), false);
                    } else if (extKey.endsWith(CollectorJsonHelpers.LONG_SUFFIX)) {
                        jsonBuilder.addField(LogTraceData.getExtensionNameKeyJSON(isMessageEvent, extKey), k.getLongValue(), false);
                    } else if (extKey.endsWith(CollectorJsonHelpers.BOOL_SUFFIX)) {
                        jsonBuilder.addField(LogTraceData.getExtensionNameKeyJSON(isMessageEvent, extKey), k.getBooleanValue(), false);
                    } else {
                        jsonBuilder.addField(LogTraceData.getExtensionNameKeyJSON(isMessageEvent, extKey), k.getStringValue(), false, true);
                    }
                }
            }
        }

        //append tags with preformatted string field value
        if (tags != null) {
            jsonBuilder.addPreformattedField("ibm_tags", CollectorJsonHelpers.jsonifyTags(tags));
        }

        return jsonBuilder.build().toString();

    }

    public static String jsonifyAudit(String wlpUserDir, String serverName, String hostName, Object event, String[] tags) {
        GenericData genData = (GenericData) event;
        KeyValuePair[] pairs = genData.getPairs();
        String key = null;
        int jsonKey = AuditData.KEYS_JSON;

        JSONObjectBuilder jsonBuilder = CollectorJsonHelpers.startAuditJsonFields(jsonKey);

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
                    if (key.equals(LogFieldConstants.IBM_DATETIME) || key.equals("loggingEventTime") || AuditData.getDatetimeKeyJSON().equals(key)) {
                        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(kvp.getLongValue());
                        jsonBuilder.addField(AuditData.getDatetimeKey(jsonKey), datetime, false, true);
                    } else if (key.equals(LogFieldConstants.IBM_SEQUENCE) || key.equals("loggingSequenceNumber") || AuditData.getSequenceKeyJSON().equals(key)) {
                        jsonBuilder.addField(AuditData.getSequenceKey(jsonKey), kvp.getStringValue(), false, false);
                    } else if (key.equals(LogFieldConstants.IBM_THREADID) || AuditData.getThreadIDKeyJSON().equals(key)) {
                        jsonBuilder.addField(AuditData.getThreadIDKey(jsonKey), DataFormatHelper.padHexString(kvp.getIntValue(), 8), false, true);
                    } else {
                        //check this before leaving
                        jsonBuilder.addField("ibm_audit_" + key, kvp.getStringValue(), false, false);
                    }

                } //There shouldn't be any list items from Audit's Generic Data object

            }

        }

        return jsonBuilder.build().toString();
    }
}
