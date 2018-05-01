/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.source;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.collector.manager.buffer.BufferManagerEMQHelper;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.collector.CollectorJsonHelpers;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.utils.LogFormatUtils;
import com.ibm.ws.logging.utils.SequenceNumber;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;

public class LogSource implements Source, WsLogHandler {

    private static final TraceComponent tc = Tr.register(LogSource.class);

    private final String sourceName = "com.ibm.ws.logging.source.message";
    private final String location = "memory";
    private BufferManager bufferMgr = null;
    static Pattern messagePattern;
    private final SequenceNumber sequenceNumber = new SequenceNumber();
    public static final String LINE_SEPARATOR;

    static {
        messagePattern = Pattern.compile("^([A-Z][\\dA-Z]{3,4})(\\d{4})([A-Z])(:)");

        LINE_SEPARATOR = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("line.separator");
            }
        });
    }

    //private final AtomicLong seq = new AtomicLong();

    protected void activate(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating " + this);
        }
    }

    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting buffer manager " + this);
        }
        this.bufferMgr = bufferMgr;
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Un-setting buffer manager " + this);
        }
        //Indication that the buffer will no longer be available
        this.bufferMgr = null;
    }

    public BufferManager getBufferManager() {
        return bufferMgr;
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceName() {
        return sourceName;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocation() {
        return location;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public void publish(RoutedMessage routedMessage) {
        //Publish the message if it is not coming from a handler thread

        LogRecord logRecord = routedMessage.getLogRecord();
        if (logRecord != null && bufferMgr != null) {

            LogTraceData parsedMessage = parse(routedMessage);
            if (!BufferManagerEMQHelper.getEMQRemovedFlag() && extractMessage(routedMessage, logRecord).startsWith("CWWKF0011I")) {
                BufferManagerEMQHelper.removeEMQTrigger();
            }
            bufferMgr.add(parsedMessage);
        }

    }

    private String extractMessage(RoutedMessage routedMessage, LogRecord logRecord) {
        String messageVal = routedMessage.getFormattedVerboseMsg();
        if (messageVal == null) {
            messageVal = logRecord.getMessage();
        }
        return messageVal;
    }

    public LogTraceData parse(RoutedMessage routedMessage) {
        return parse(routedMessage, routedMessage.getLogRecord());
    }

    public LogTraceData parse(RoutedMessage routedMessage, LogRecord logRecord) {

        LogTraceData logData = new LogTraceData();

        long dateVal = logRecord.getMillis();
        logData.setDatetime(dateVal);

        String messageIdVal = null;
        String messageVal = extractMessage(routedMessage, logRecord);
        if (messageVal != null) {
            messageIdVal = parseMessageId(messageVal);
        }
        logData.setMessageId(messageIdVal);

        int threadIdVal = (int) Thread.currentThread().getId();
        logData.setThreadId(threadIdVal);
        logData.setModule(logRecord.getLoggerName());
        logData.setSeverity(LogFormatUtils.mapLevelToType(logRecord));
        logData.setLoglevel(LogFormatUtils.mapLevelToRawType(logRecord));
        logData.setMethodName(logRecord.getSourceMethodName());
        logData.setClassName(logRecord.getSourceClassName());

        logData.setLevelValue(logRecord.getLevel().intValue());
        String threadName = Thread.currentThread().getName();
        logData.setThreadName(threadName);

        WsLogRecord wsLogRecord = getWsLogRecord(logRecord);
        if (wsLogRecord != null) {
            logData.setCorrelationId(wsLogRecord.getCorrelationId());
            logData.setOrg(wsLogRecord.getOrganization());
            logData.setProduct(wsLogRecord.getProduct());
            logData.setComponent(wsLogRecord.getComponent());

            if (wsLogRecord.getExtensions() != null) {
                KeyValuePairList extensions = new KeyValuePairList(LogFieldConstants.EXTENSIONS_KVPL);
                Map<String, String> extMap = wsLogRecord.getExtensions();
                for (Map.Entry<String, String> entry : extMap.entrySet()) {
                    CollectorJsonHelpers.handleExtensions(extensions, entry.getKey(), entry.getValue());
                }
                logData.setExtensions(extensions);
            }
        } else {
            logData.setCorrelationId(null);
            logData.setOrg(null);
            logData.setProduct(null);
            logData.setComponent(null);
            logData.setExtensions(null);
        }

        logData.setSequence(sequenceNumber.next(dateVal));

        Throwable thrown = logRecord.getThrown();
        if (thrown != null) {
            String stackTrace = DataFormatHelper.throwableToString(thrown);
            logData.setThrowable(stackTrace);

            String s = thrown.getLocalizedMessage();
            if (s == null) {
                s = thrown.toString();
            }
            logData.setThrowableLocalized(s);
        } else {
            logData.setThrowable(null);
            logData.setThrowableLocalized(null);
        }

        StringBuilder msgBldr = new StringBuilder();
        msgBldr.append(messageVal);
        if (thrown != null) {
            String stackTrace = DataFormatHelper.throwableToString(thrown);
            if (stackTrace != null) {
                msgBldr.append(LINE_SEPARATOR).append(stackTrace);
            }
        }
        logData.setMessage(msgBldr.toString());

        if (routedMessage.getFormattedMsg() != null) {
            logData.setFormattedMsg(routedMessage.getFormattedMsg());
        } else {
            logData.setFormattedMsg(null);
        }

        // cannot pass null to traceData.setObjectId(int i)

        logData.setSourceType(sourceName);

        return logData;
    }

    /**
     * @return the message ID for the given message.
     */
    protected String parseMessageId(String msg) {
        String messageId = null;
        Matcher matcher = messagePattern.matcher(msg);
        if (matcher.find())
            messageId = msg.substring(matcher.start(), matcher.end() - 1);
        return messageId;
    }

    private WsLogRecord getWsLogRecord(LogRecord logRecord) {
        return (logRecord instanceof WsLogRecord) ? (WsLogRecord) logRecord : null;
    }
}
