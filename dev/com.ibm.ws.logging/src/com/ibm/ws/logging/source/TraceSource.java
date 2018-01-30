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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsTraceHandler;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.synch.ThreadLocalHandler;
import com.ibm.ws.logging.utils.LogFormatUtils;
import com.ibm.ws.logging.utils.SequenceNumber;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;

public class TraceSource implements Source, WsTraceHandler {

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

    public GenericData parse(RoutedMessage routedMessage, LogRecord logRecord, Object id) {

        GenericData genData = new GenericData();
//        LogRecord logRecord = routedMessage.getLogRecord();
        String verboseMessage = routedMessage.getFormattedVerboseMsg();
        if (verboseMessage == null) {
            genData.addPair("message", logRecord.getMessage());
        } else {
            genData.addPair("message", verboseMessage);
        }

        long datetimeValue = logRecord.getMillis();
        genData.addPair("ibm_datetime", datetimeValue);
        genData.addPair("ibm_threadId", logRecord.getThreadID());
        genData.addPair("module", logRecord.getLoggerName());
        genData.addPair("severity", LogFormatUtils.mapLevelToType(logRecord));
        genData.addPair("logLevel", LogFormatUtils.mapLevelToRawType(logRecord));
        genData.addPair("ibm_methodName", logRecord.getSourceMethodName());
        genData.addPair("ibm_className", logRecord.getSourceClassName());
        String sequenceNum = sequenceNumber.next(datetimeValue);
        genData.addPair("ibm_sequence", sequenceNum);
        genData.addPair("levelValue", logRecord.getLevel().intValue());

        String threadName = Thread.currentThread().getName();
        genData.addPair("threadName", threadName);

        if (id != null) {
            Integer objid = System.identityHashCode(id);
            genData.addPair("objectId", objid);
        }
        WsLogRecord wsLogRecord = getWsLogRecord(logRecord);

        if (wsLogRecord != null) {
            genData.addPair("correlationId", wsLogRecord.getCorrelationId());
            genData.addPair("org", wsLogRecord.getOrganization());
            genData.addPair("product", wsLogRecord.getProduct());
            genData.addPair("component", wsLogRecord.getComponent());
        }

        KeyValuePairList extensions = new KeyValuePairList();
        Map<String, String> extMap = null;
        if (logRecord instanceof WsLogRecord) {
            extMap = ((WsLogRecord) logRecord).getExtensions();
        }

        for (Map.Entry<String, String> entry : extMap.entrySet()) {
            extensions.addPair(entry.getKey(), entry.getValue());
        }
        genData.addPairs(extensions);
        genData.setLevelValue(logRecord.getLevel().intValue());
        genData.setSourceType(sourceName);
        return genData;
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

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.logging.WsTraceHandler#publish(com.ibm.ws.logging.RoutedMessage)
     */
    @Override
    public void publish(RoutedMessage routedMessage) {
        // TODO Auto-generated method stub

    }

}
