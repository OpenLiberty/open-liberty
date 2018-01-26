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

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.LogRecord;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.RoutedMessage;
//import com.ibm.ws.logging.WsTraceHandler;
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

public class TraceSource implements Source {

    private static final TraceComponent tc = Tr.register(TraceSource.class);

    private final String sourceName = "com.ibm.ws.logging.source.trace";
    private final String location = "memory";
    private BufferManager bufferMgr = null;
    private final SequenceNumber sequenceNumber = new SequenceNumber();
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
    public void publish(RoutedMessage routedMessage, Object id) {
        //Publish the message if it is not coming from a handler thread
        if (!ThreadLocalHandler.get()) {
            LogRecord logRecord = routedMessage.getLogRecord();
            if (logRecord != null) {
                if (bufferMgr != null) {
                    bufferMgr.add(parse(routedMessage, logRecord, id));
                }
            }
        }
    }

//    public TraceLogData parse(RoutedMessage routedMessage, LogRecord logRecord) {
//        String message = routedMessage.getFormattedVerboseMsg();
//        if (message == null)
//            message = logRecord.getMessage();
//        long timestamp = logRecord.getMillis();
//        int threadId = logRecord.getThreadID();
//        String loggerName = logRecord.getLoggerName();
//        String logLevel = LogFormatUtils.mapLevelToType(logRecord);
//        String logLevelRaw = LogFormatUtils.mapLevelToRawType(logRecord);
//        String methodName = logRecord.getSourceMethodName();
//        String className = logRecord.getSourceClassName();
//        Map<String, String> extensions = null;
//        if (logRecord instanceof WsLogRecord)
//            extensions = ((WsLogRecord) logRecord).getExtensions();
//        String sequence = sequenceNumber.next(timestamp);
//        //String sequence = timestamp + "_" + String.format("%013X", seq.incrementAndGet());
//
//        return new TraceLogData(timestamp, threadId, loggerName, logLevel, logLevelRaw, message, methodName, className, extensions, sequence);
//
//    }

    public GenericData parse(RoutedMessage routedMessage, LogRecord logRecord, Object id) {
//        Object id
        KeyValuePair message = null;
        String verboseMessage = routedMessage.getFormattedVerboseMsg();
        if (verboseMessage == null) {
            message = new KeyValuePair("message", logRecord.getMessage(), KeyValuePair.ValueTypes.STRING);
        } else {
            message = new KeyValuePair("message", routedMessage.getFormattedVerboseMsg(), KeyValuePair.ValueTypes.STRING);
        }

        KeyValuePair datetime = new KeyValuePair("ibm_datetime", Long.toString(logRecord.getMillis()), KeyValuePair.ValueTypes.NUMBER);
        KeyValuePair threadId = new KeyValuePair("ibm_threadId", Integer.toString(logRecord.getThreadID()), KeyValuePair.ValueTypes.NUMBER);
        KeyValuePair loggerName = new KeyValuePair("module", logRecord.getLoggerName(), KeyValuePair.ValueTypes.STRING);
        KeyValuePair logLevel = new KeyValuePair("severity", LogFormatUtils.mapLevelToType(logRecord), KeyValuePair.ValueTypes.STRING);
        KeyValuePair logLevelRaw = new KeyValuePair("logLevel", LogFormatUtils.mapLevelToRawType(logRecord), KeyValuePair.ValueTypes.STRING);
        KeyValuePair methodName = new KeyValuePair("ibm_methodName", logRecord.getSourceMethodName(), KeyValuePair.ValueTypes.STRING);
        KeyValuePair className = new KeyValuePair("ibm_className", logRecord.getSourceClassName(), KeyValuePair.ValueTypes.STRING);
        String sequenceNum = sequenceNumber.next(Long.parseLong(datetime.getValue()));
        KeyValuePair sequence = new KeyValuePair("ibm_sequence", sequenceNum, KeyValuePair.ValueTypes.STRING);
        KeyValuePair levelValue = new KeyValuePair("levelValue", Integer.toString(logRecord.getLevel().intValue()), KeyValuePair.ValueTypes.NUMBER);

        KeyValuePairs extensions = new KeyValuePairs();
        ArrayList<KeyValuePair> extList = extensions.getKeyValuePairs();
        Map<String, String> extMap = null;
        if (logRecord instanceof WsLogRecord) {
            extMap = ((WsLogRecord) logRecord).getExtensions();
        }

        for (Map.Entry<String, String> entry : extMap.entrySet()) {
            KeyValuePair extEntry = new KeyValuePair(entry.getKey(), entry.getValue(), KeyValuePair.ValueTypes.STRING);
            extList.add(extEntry);
        }

        GenericData genData = new GenericData();
        ArrayList<Pair> pairs = genData.getPairs();

        pairs.add(message);
        pairs.add(datetime);
        pairs.add(threadId);
        pairs.add(loggerName);
        pairs.add(logLevel);
        pairs.add(logLevelRaw);
        pairs.add(methodName);
        pairs.add(className);
        pairs.add(sequence);
        pairs.add(extensions);
        pairs.add(levelValue);

        if (id != null) {
            String objId = generateObjectId(id);
            KeyValuePair objectId = new KeyValuePair("objectId", objId, KeyValuePair.ValueTypes.STRING);
            pairs.add(objectId);
        }
        //get format for trace
        WsLogRecord wsLogRecord = getWsLogRecord(logRecord);
        if (wsLogRecord != null) {
            KeyValuePair corrId = new KeyValuePair("correlationId", wsLogRecord.getCorrelationId(), KeyValuePair.ValueTypes.STRING);
            KeyValuePair org = new KeyValuePair("org", wsLogRecord.getOrganization(), KeyValuePair.ValueTypes.STRING);
            KeyValuePair prod = new KeyValuePair("product", wsLogRecord.getOrganization(), KeyValuePair.ValueTypes.STRING);
            KeyValuePair component = new KeyValuePair("component", wsLogRecord.getComponent(), KeyValuePair.ValueTypes.STRING);
            KeyValuePair wsSourceThreadName = new KeyValuePair("wsSourceThreadName", wsLogRecord.getReporterOrSourceThreadName(), KeyValuePair.ValueTypes.STRING);
            pairs.add(corrId);
            pairs.add(org);
            pairs.add(prod);
            pairs.add(component);
            pairs.add(wsSourceThreadName);
        }
        genData.setSourceType(sourceName);
        return genData;

    }

    private String generateObjectId(Object id) {
        String objId;

        objId = Integer.toHexString(System.identityHashCode(id));
        if (objId.length() < 8) {
            StringBuilder builder = new StringBuilder();
            builder.append("00000000");
            builder.append(objId);
            objId = builder.substring(builder.length() - 8);
        }
        return objId;
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
