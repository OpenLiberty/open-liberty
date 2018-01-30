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
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.LogTraceSourceGenericData;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.synch.ThreadLocalHandler;
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

    /**
     * Log the given log record.
     *
     * @param routedMessage The LogRecord along with various message formats.
     */
    @Override
    @Trivial
    public void publish(RoutedMessage routedMessage) {
        //Publish the message if it is not coming from a handler thread
        if (!ThreadLocalHandler.get()) {

            LogRecord logRecord = routedMessage.getLogRecord();
//            if (logRecord != null && bufferMgr != null) {
//<<<<<<< HEAD
//                bufferMgr.add(parse(routedMessage, logRecord));
//=======
            GenericData parsedMessage = parse(routedMessage);
            if (!BufferManager.EMQRemovedFlag && extractMessage(routedMessage, logRecord).startsWith("CWWKF0011I")) {
                BufferManager.removeEMQTrigger();
                BufferManager.EMQRemovedFlag = true;
            }
            bufferMgr.add(parsedMessage);
        }
    }

//    <<<<<<<HEAD
////    public MessageLogData parse(RoutedMessage routedMessage, LogRecord logRecord) {
////        String message = routedMessage.getFormattedVerboseMsg();
////        if (message == null)
////            message = logRecord.getMessage();
////        long date = logRecord.getMillis();
////        String messageId = null;
////        if (message != null)
////            messageId = parseMessageId(message);
////        int threadId = (int) Thread.currentThread().getId();//logRecord.getThreadID();
////        String loggerName = logRecord.getLoggerName();
////        String logLevel = LogFormatUtils.mapLevelToType(logRecord);
////        String logLevelRaw = LogFormatUtils.mapLevelToRawType(logRecord);
////        String methodName = logRecord.getSourceMethodName();
////        String className = logRecord.getSourceClassName();
////        Map<String, String> extensions = null;
////        if (logRecord instanceof WsLogRecord)
////            extensions = ((WsLogRecord) logRecord).getExtensions();
////        String sequence = sequenceNumber.next(date);
////        //String sequence = date + "_" + String.format("%013X", seq.incrementAndGet());
////        Throwable throwable = logRecord.getThrown();
////
////        return new MessageLogData(date, threadId, loggerName, logLevel, logLevelRaw, messageId, message, methodName, className, extensions, sequence, throwable);
////
////    }
//
//    public GenericData parse(RoutedMessage routedMessage, LogRecord logRecord) {
//
//        String messageVal = routedMessage.getFormattedVerboseMsg();
//
//        if (messageVal == null) {
//            messageVal = logRecord.getMessage();
//        }
//
//        long dateVal = logRecord.getMillis();
//        KeyValuePair date = new KeyValuePair("ibm_datetime", Long.toString(dateVal), KeyValuePair.ValueTypes.NUMBER);
//=======

    private String extractMessage(RoutedMessage routedMessage, LogRecord logRecord) {
        String messageVal = routedMessage.getFormattedVerboseMsg();
        if (messageVal == null) {
            messageVal = logRecord.getMessage();
        }
        return messageVal;
    }

    public GenericData parse(RoutedMessage routedMessage) {

        LogTraceSourceGenericData genData = new LogTraceSourceGenericData();
        LogRecord logRecord = routedMessage.getLogRecord();
        String messageVal = extractMessage(routedMessage, logRecord);

        long dateVal = logRecord.getMillis();
        genData.addPair("ibm_datetime", dateVal);

        String messageIdVal = null;

        if (messageVal != null) {
            messageIdVal = parseMessageId(messageVal);
        }

        genData.addPair("ibm_messageId", messageIdVal);

        int threadIdVal = (int) Thread.currentThread().getId();//logRecord.getThreadID();
        genData.addPair("ibm_threadId", threadIdVal);
        genData.addPair("module", logRecord.getLoggerName());
        genData.addPair("severity", LogFormatUtils.mapLevelToType(logRecord));
        genData.addPair("loglevel", LogFormatUtils.mapLevelToRawType(logRecord));
        genData.addPair("ibm_methodName", logRecord.getSourceMethodName());
        genData.addPair("ibm_className", logRecord.getSourceClassName());
        genData.addPair("levelValue", logRecord.getLevel().intValue());

        WsLogRecord wsLogRecord = getWsLogRecord(logRecord);

        if (wsLogRecord != null) {
            genData.addPair("correlationId", wsLogRecord.getCorrelationId());
            genData.addPair("org", wsLogRecord.getOrganization());
            genData.addPair("product", wsLogRecord.getProduct());
            genData.addPair("component", wsLogRecord.getComponent());
//            genData.addPair("wsSourceThreadName", wsLogRecord.getReporterOrSourceThreadName());
        }

        String threadName = Thread.currentThread().getName();
        genData.addPair("threadName", threadName);

        KeyValuePairList extensions = new KeyValuePairList();
        Map<String, String> extMap = null;
        if (logRecord instanceof WsLogRecord) {
            extMap = ((WsLogRecord) logRecord).getExtensions();
            for (Map.Entry<String, String> entry : extMap.entrySet()) {
                extensions.addPair(entry.getKey(), entry.getValue());
            }
        }

        genData.addPairs(extensions);
        genData.addPair("sequence", sequenceNumber.next(dateVal));
        //String sequence = date + "_" + String.format("%013X", seq.incrementAndGet());

        Throwable thrown = logRecord.getThrown();
        StringBuilder msgBldr = new StringBuilder();
        msgBldr.append(messageVal);
        if (thrown != null) {
            String stackTrace = DataFormatHelper.throwableToString(thrown);
            if (stackTrace != null) {
                msgBldr.append(LINE_SEPARATOR).append(stackTrace);
            }
        }
        genData.addPair("message", msgBldr.toString());
        genData.setLevelValue(logRecord.getLevel().intValue());
        genData.setSourceType(sourceName);

        return genData;

    }

    private WsLogRecord getWsLogRecord(LogRecord logRecord) {
        try {
            return (WsLogRecord) logRecord;
        } catch (ClassCastException ex) {
            return null;
        }
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

}
