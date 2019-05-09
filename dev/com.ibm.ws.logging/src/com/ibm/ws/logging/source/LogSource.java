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

import java.util.Map;
import java.util.logging.LogRecord;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.collector.manager.buffer.BufferManagerEMQHelper;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.collector.CollectorJsonHelpers;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.utils.LogFormatUtils;
import com.ibm.ws.logging.utils.SequenceNumber;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;

public class LogSource implements Source {

    private static final TraceComponent tc = Tr.register(LogSource.class);

    private final String sourceName = "com.ibm.ws.logging.source.message";
    private final String location = "memory";
    private BufferManager bufferMgr = null;
    private final SequenceNumber sequenceNumber = new SequenceNumber();

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

        String messageVal = extractMessage(routedMessage, logRecord);

        logData.setMessageId(null); // needs to be null to satisfy HandlerTest.java testMessageSource()

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
        //Extensions KVP calculated below, but needs to bet set in the correct 'order' further below
        KeyValuePairList extensions = null;
        if (wsLogRecord != null) {
            logData.setCorrelationId(wsLogRecord.getCorrelationId());
            logData.setOrg(wsLogRecord.getOrganization());
            logData.setProduct(wsLogRecord.getProduct());
            logData.setComponent(wsLogRecord.getComponent());

            if (wsLogRecord.getExtensions() != null) {
                extensions = new KeyValuePairList(LogFieldConstants.EXTENSIONS_KVPL);
                Map<String, String> extMap = wsLogRecord.getExtensions();
                for (Map.Entry<String, String> entry : extMap.entrySet()) {
                    CollectorJsonHelpers.handleExtensions(extensions, entry.getKey(), entry.getValue());
                }
            }
        }

        logData.setRawSequenceNumber(sequenceNumber.getRawSequenceNumber());

        Throwable thrown = logRecord.getThrown();
        if (thrown != null) {
            String stackTrace = DataFormatHelper.throwableToString(thrown);
            logData.setThrowable(stackTrace);

            String s = thrown.getLocalizedMessage();
            if (s == null) {
                s = thrown.toString();
            }
            logData.setThrowableLocalized(s);
        }

        logData.setMessage(messageVal);

        if (routedMessage.getFormattedMsg() != null) {
            logData.setFormattedMsg(routedMessage.getFormattedMsg());
        } else {
            logData.setFormattedMsg(null);
        }

        logData.setExtensions(extensions);

        logData.setSourceName(sourceName);

        return logData;
    }

    private WsLogRecord getWsLogRecord(LogRecord logRecord) {
        return (logRecord instanceof WsLogRecord) ? (WsLogRecord) logRecord : null;
    }
}
