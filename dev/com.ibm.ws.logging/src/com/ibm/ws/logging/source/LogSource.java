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
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.KeyValuePairs;
import com.ibm.ws.logging.data.Pair;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.synch.ThreadLocalHandler;
import com.ibm.ws.logging.utils.LogFormatUtils;
import com.ibm.ws.logging.utils.SequenceNumber;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;

public class LogSource implements Source {

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
    @Trivial
    public void publish(RoutedMessage routedMessage) {
        //Publish the message if it is not coming from a handler thread
        if (!ThreadLocalHandler.get()) {
            LogRecord logRecord = routedMessage.getLogRecord();
            if (logRecord != null && bufferMgr != null) {
                bufferMgr.add(parse(routedMessage, logRecord));
            }
        }
    }

//    public MessageLogData parse(RoutedMessage routedMessage, LogRecord logRecord) {
//        String message = routedMessage.getFormattedVerboseMsg();
//        if (message == null)
//            message = logRecord.getMessage();
//        long date = logRecord.getMillis();
//        String messageId = null;
//        if (message != null)
//            messageId = parseMessageId(message);
//        int threadId = (int) Thread.currentThread().getId();//logRecord.getThreadID();
//        String loggerName = logRecord.getLoggerName();
//        String logLevel = LogFormatUtils.mapLevelToType(logRecord);
//        String logLevelRaw = LogFormatUtils.mapLevelToRawType(logRecord);
//        String methodName = logRecord.getSourceMethodName();
//        String className = logRecord.getSourceClassName();
//        Map<String, String> extensions = null;
//        if (logRecord instanceof WsLogRecord)
//            extensions = ((WsLogRecord) logRecord).getExtensions();
//        String sequence = sequenceNumber.next(date);
//        //String sequence = date + "_" + String.format("%013X", seq.incrementAndGet());
//        Throwable throwable = logRecord.getThrown();
//
//        return new MessageLogData(date, threadId, loggerName, logLevel, logLevelRaw, messageId, message, methodName, className, extensions, sequence, throwable);
//
//    }

    public GenericData parse(RoutedMessage routedMessage, LogRecord logRecord) {

        String messageVal = routedMessage.getFormattedVerboseMsg();

        if (messageVal == null) {
            messageVal = logRecord.getMessage();
        }

        long dateVal = logRecord.getMillis();
        KeyValuePair date = new KeyValuePair("ibm_datetime", Long.toString(dateVal), KeyValuePair.ValueTypes.NUMBER);

        String messageIdVal = null;

        if (messageVal != null) {
            messageIdVal = parseMessageId(messageVal);
        }

        KeyValuePair messageId = new KeyValuePair("ibm_messageId", messageIdVal, KeyValuePair.ValueTypes.STRING);

        int threadIdVal = (int) Thread.currentThread().getId();//logRecord.getThreadID();
        KeyValuePair threadId = new KeyValuePair("ibm_threadId", Integer.toString(threadIdVal), KeyValuePair.ValueTypes.NUMBER);
        KeyValuePair loggerName = new KeyValuePair("module", logRecord.getLoggerName(), KeyValuePair.ValueTypes.STRING);
        KeyValuePair logLevel = new KeyValuePair("severity", LogFormatUtils.mapLevelToType(logRecord), KeyValuePair.ValueTypes.STRING);
        KeyValuePair logLevelRaw = new KeyValuePair("loglevel", LogFormatUtils.mapLevelToRawType(logRecord), KeyValuePair.ValueTypes.STRING);
        KeyValuePair methodName = new KeyValuePair("ibm_methodName", logRecord.getSourceMethodName(), KeyValuePair.ValueTypes.STRING);
        KeyValuePair className = new KeyValuePair("ibm_className", logRecord.getSourceClassName(), KeyValuePair.ValueTypes.STRING);
        KeyValuePair levelValue = new KeyValuePair("levelValue", Integer.toString(logRecord.getLevel().intValue()), KeyValuePair.ValueTypes.NUMBER);

        KeyValuePairs extensions = new KeyValuePairs();
        Map<String, String> extMap = null;
        ArrayList<KeyValuePair> extList = extensions.getKeyValuePairs();
        if (logRecord instanceof WsLogRecord) {
            extMap = ((WsLogRecord) logRecord).getExtensions();
            for (Map.Entry<String, String> entry : extMap.entrySet()) {
                KeyValuePair extEntry = new KeyValuePair(entry.getKey(), entry.getValue(), KeyValuePair.ValueTypes.STRING);
                extList.add(extEntry);
            }
        }

        KeyValuePair sequence = new KeyValuePair("sequence", sequenceNumber.next(dateVal), KeyValuePair.ValueTypes.STRING);
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

        KeyValuePair message = new KeyValuePair("message", msgBldr.toString(), KeyValuePair.ValueTypes.STRING);

        GenericData genData = new GenericData();
        ArrayList<Pair> pairs = genData.getPairs();

        pairs.add(message);
        pairs.add(date);
        pairs.add(messageId);
        pairs.add(threadId);
        pairs.add(loggerName);
        pairs.add(logLevel);
        pairs.add(logLevelRaw);
        pairs.add(methodName);
        pairs.add(className);
        pairs.add(extensions);
        pairs.add(sequence);
        pairs.add(levelValue);

//        //get format for trace

        genData.setSourceType(sourceName);

        return genData;

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

    /**
     * @return
     */
    private WsLogRecord getWsLogRecord(LogRecord logRecord) {
        try {
            return (WsLogRecord) logRecord;
        } catch (ClassCastException ex) {
            return null;
        }
    }
}
