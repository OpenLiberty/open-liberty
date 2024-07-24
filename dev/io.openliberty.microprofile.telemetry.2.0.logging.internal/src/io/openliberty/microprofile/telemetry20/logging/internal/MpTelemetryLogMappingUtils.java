/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.logging.internal;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.CollectorJsonHelpers;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.FFDCData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.LogTraceData;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.SemanticAttributes;

/**
 *
 */
@Trivial
public class MpTelemetryLogMappingUtils {

    /**
     * Get the event type from the Liberty log source.
     *
     * @param source The source where the Liberty event originated from.
     */
    public static String getLibertyEventType(String source) {
        if (source.equals(CollectorConstants.MESSAGES_SOURCE)) {
            return CollectorConstants.MESSAGES_LOG_EVENT_TYPE;
        } else if (source.endsWith(CollectorConstants.TRACE_SOURCE)) {
            return CollectorConstants.TRACE_LOG_EVENT_TYPE;
        } else if (source.endsWith(CollectorConstants.FFDC_SOURCE)) {
            return CollectorConstants.FFDC_EVENT_TYPE;
        } else
            return "";
    }

    /**
     * Map the log event data to the OpenTelemetry Logs Data Model Format.
     *
     * @param event     The object originating from logging source which contains necessary fields
     * @param eventType The type of event
     */
    public static void mapLibertyEventToOpenTelemetry(LogRecordBuilder builder, String eventType, Object event) {
        if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE)) {
            mapMessageAndTraceToOpenTelemetry(builder, eventType, event);
        } else if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE)) {
            mapMessageAndTraceToOpenTelemetry(builder, eventType, event);
        } else if (eventType.equals(CollectorConstants.FFDC_EVENT_TYPE)) {
            mapFFDCToOpenTelemetry(builder, eventType, event);
        }
    }

    /**
     * Maps the Message and Trace log events to the OpenTelemetry Logs Data Model.
     *
     * @param builder   The OpenTelemetry LogRecordBuilder, which is used to construct the LogRecord.
     * @param eventType The object originating from logging source which contains necessary fields
     * @param event     The type of event
     */
    private static void mapMessageAndTraceToOpenTelemetry(LogRecordBuilder builder, String eventType, Object event) {
        LogTraceData logData = (LogTraceData) event;

        // Get Timestamp from LogData and set it in the LogRecordBuilder
        builder.setTimestamp(logData.getDatetime(), TimeUnit.MILLISECONDS);

        // Get Log Level from LogData and set it in the LogRecordBuilder
        String loglevel = logData.getLoglevel();
        builder.setSeverity(mapWsLevelToSeverity(loglevel));

        // Get Log Severity from LogData and set it in the LogRecordBuilder
        String logSeverity = logData.getSeverity();
        builder.setSeverityText(logSeverity);

        // Get message from LogData and set it in the LogRecordBuilder
        String message = logData.getMessage();
        if (loglevel != null) {
            if (loglevel.equals("ENTRY") || loglevel.equals("EXIT")) {
                message = removeSpace(message);
            }
        }
        builder.setBody(message);

        // Get Attributes builder to add additional Log fields
        AttributesBuilder attributes = Attributes.builder();

        // Add Thread information to Attributes Builder
        attributes.put(SemanticAttributes.THREAD_NAME, logData.getThreadName());
        attributes.put(SemanticAttributes.THREAD_ID, logData.getThreadId());

        // Add Throwable information to Attribute Builder
        String exceptionName = logData.getExceptionName();
        String throwable = logData.getThrowable();
        if (exceptionName != null && throwable != null) {
            attributes.put(SemanticAttributes.EXCEPTION_TYPE, exceptionName);
            attributes.put(SemanticAttributes.EXCEPTION_STACKTRACE, throwable);
        }

        // Add additional log information from LogData to Attributes Builder
        attributes.put(MpTelemetryLogFieldConstants.LIBERTY_TYPE, eventType)
                        .put(MpTelemetryLogFieldConstants.LIBERTY_MESSAGEID, logData.getMessageId())
                        .put(MpTelemetryLogFieldConstants.LIBERTY_METHODNAME, logData.getMethodName())
                        .put(MpTelemetryLogFieldConstants.LIBERTY_MODULE, logData.getModule())
                        .put(MpTelemetryLogFieldConstants.LIBERTY_CLASSNAME, logData.getClassName())
                        .put(MpTelemetryLogFieldConstants.LIBERTY_SEQUENCE, logData.getSequence());

        // Get Extensions (LogRecordContext) from LogData and add it as attributes.
        ArrayList<KeyValuePair> extensions = null;
        KeyValuePairList kvpl = null;
        kvpl = logData.getExtensions();
        if (kvpl != null) {
            if (kvpl.getKey().equals(LogFieldConstants.EXTENSIONS_KVPL)) {
                extensions = kvpl.getList();
                for (KeyValuePair k : extensions) {
                    String extKey = k.getKey();
                    if (extKey.equals(MpTelemetryLogFieldConstants.EXT_APPNAME)) {
                        // Map correct OTel Attribute key name for ext_appName
                        attributes.put(MpTelemetryLogFieldConstants.LIBERTY_EXT_APP_NAME, k.getStringValue());
                        continue;
                    }
                    if (extKey.equals(MpTelemetryLogFieldConstants.EXT_THREAD)) {
                        // Since, the thread name is already set using OTel Semantic naming,
                        // to avoid duplicates, we are skipping the mapping.
                        continue;
                    }
                    // Format the extension key to map to the OTel Attribute Naming convention.
                    if (extKey.endsWith(CollectorJsonHelpers.INT_SUFFIX)) {
                        extKey = formatExtensionKey(extKey);
                        attributes.put(extKey, k.getIntValue());
                    } else if (extKey.endsWith(CollectorJsonHelpers.FLOAT_SUFFIX)) {
                        extKey = formatExtensionKey(extKey);
                        attributes.put(extKey, k.getFloatValue());
                    } else if (extKey.endsWith(CollectorJsonHelpers.LONG_SUFFIX)) {
                        extKey = formatExtensionKey(extKey);
                        attributes.put(extKey, k.getLongValue());
                    } else if (extKey.endsWith(CollectorJsonHelpers.BOOL_SUFFIX)) {
                        extKey = formatExtensionKey(extKey);
                        attributes.put(extKey, k.getBooleanValue());
                    } else {
                        extKey = formatExtensionKey(extKey);
                        attributes.put(extKey, k.getStringValue());
                    }
                }
            }
        }

        // Set the Attributes to the builder.
        builder.setAllAttributes(attributes.build());

        // Set the Span and Trace IDs from the current context.
        builder.setContext(Context.current());
    }

    /**
     * Maps the FFDC log events to the OpenTelemetry Logs Data Model.
     *
     * @param builder   The OpenTelemetry LogRecordBuilder, which is used to construct the LogRecord.
     * @param eventType The object originating from logging source which contains necessary fields
     * @param event     The type of event
     */
    private static void mapFFDCToOpenTelemetry(LogRecordBuilder builder, String eventType, Object event) {
        FFDCData ffdcData = (FFDCData) event;

        // Get Timestamp from LogData and set it in the LogRecordBuilder
        builder.setTimestamp(ffdcData.getDatetime(), TimeUnit.MILLISECONDS);

        // Set FFDC log level to WARNING in the LogRecordBuilder
        builder.setSeverity(Severity.WARN);

        // Get Attributes builder to add additional Log fields
        AttributesBuilder attributes = Attributes.builder();

        // Add Thread information to Attributes Builder
        attributes.put(SemanticAttributes.THREAD_ID, ffdcData.getThreadId());

        // Add FFDC information to Semantic Convention Attributes
        attributes.put(SemanticAttributes.EXCEPTION_TYPE, ffdcData.getExceptionName());
        attributes.put(SemanticAttributes.EXCEPTION_MESSAGE, ffdcData.getMessage());
        attributes.put(SemanticAttributes.EXCEPTION_STACKTRACE, ffdcData.getStacktrace());

        // Add additional log information from FFDCData to Attributes Builder
        attributes.put(MpTelemetryLogFieldConstants.LIBERTY_TYPE, eventType)
                        .put(MpTelemetryLogFieldConstants.LIBERTY_PROBEID, ffdcData.getProbeId())
                        .put(MpTelemetryLogFieldConstants.LIBERTY_OBJECTDETAILS, ffdcData.getObjectDetails())
                        .put(MpTelemetryLogFieldConstants.LIBERTY_CLASSNAME, ffdcData.getClassName())
                        .put(MpTelemetryLogFieldConstants.LIBERTY_SEQUENCE, ffdcData.getSequence());

        // Set the Attributes to the builder.
        builder.setAllAttributes(attributes.build());

        // Set the Span and Trace IDs from the current context.
        builder.setContext(Context.current());
    }

    /**
     * Maps the Liberty Log levels to the OpenTelemetry Severity.
     *
     * @param level
     */
    private static Severity mapWsLevelToSeverity(String level) {
        if (level.equals(WsLevel.FATAL.toString())) {
            return Severity.FATAL;
        } else if (level.equals(WsLevel.SEVERE.toString()) || level.equals(WsLevel.ERROR.toString())) {
            return Severity.ERROR;
        } else if (level.equals(WsLevel.WARNING.toString()) || level.equals("SystemErr")) {
            return Severity.WARN;
        } else if (level.equals(WsLevel.AUDIT.toString())) {
            return Severity.INFO2;
        } else if (level.equals(WsLevel.INFO.toString()) || level.equals("SystemOut")) {
            return Severity.INFO;
        } else if (level.equals(WsLevel.CONFIG.toString())) {
            return Severity.DEBUG4;
        } else if (level.equals(WsLevel.DETAIL.toString())) {
            return Severity.DEBUG3;
        } else if (level.equals(WsLevel.FINE.toString()) || level.equals(WsLevel.EVENT.toString())) {
            return Severity.DEBUG2;
        } else if (level.equals(WsLevel.FINER.toString()) || level.equals("ENTRY") || level.equals("EXIT")) {
            return Severity.DEBUG;
        } else if (level.equals(WsLevel.FINEST.toString())) {
            return Severity.TRACE;
        } else {
            return Severity.INFO;
        }
    }

    private static String formatExtensionKey(String extKey) {
        StringBuffer sb = new StringBuffer();

        // Get the extensionName substring without the "ext_" prefix and data type suffix.
        String extName = "";
        int extStartIdx = extKey.indexOf("_");
        int extEndIdx = extKey.lastIndexOf("_", extStartIdx + 1);
        if (extEndIdx != -1) {
            // The "_<dataType>" is appended to the extension name, remove it.
            extName = extKey.substring(extStartIdx + 1, extEndIdx);
        } else {
            extName = extKey.substring(extStartIdx + 1);
        }

        // Map extension name using OTel Attributes naming convention
        sb.append(MpTelemetryLogFieldConstants.IO_OPENLIBERTY_EXT_TAG).append(extName.toLowerCase());

        return sb.toString();
    }

    private static String removeSpace(String s) {
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

}
