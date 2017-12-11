/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
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

    /** {@inheritDoc} */
    @Override
    public void publish(RoutedMessage routedMessage) {
        //Publish the message if it is not coming from a handler thread
        if (!ThreadLocalHandler.get()) {
            LogRecord logRecord = routedMessage.getLogRecord();
            if (logRecord != null) {
                if (bufferMgr != null) {
                    bufferMgr.add(parse(routedMessage, logRecord));
                }
            }
        }
    }

    public TraceLogData parse(RoutedMessage routedMessage, LogRecord logRecord) {
        String message = routedMessage.getFormattedVerboseMsg();
        if (message == null)
            message = logRecord.getMessage();
        long timestamp = logRecord.getMillis();
        int threadId = logRecord.getThreadID();
        String loggerName = logRecord.getLoggerName();
        String logLevel = LogFormatUtils.mapLevelToType(logRecord);
        String logLevelRaw = LogFormatUtils.mapLevelToRawType(logRecord);
        String methodName = logRecord.getSourceMethodName();
        String className = logRecord.getSourceClassName();
        Map<String, String> extensions = null;
        if (logRecord instanceof WsLogRecord)
            extensions = ((WsLogRecord) logRecord).getExtensions();
        String sequence = sequenceNumber.next(timestamp);
        //String sequence = timestamp + "_" + String.format("%013X", seq.incrementAndGet());

        return new TraceLogData(timestamp, threadId, loggerName, logLevel, logLevelRaw, message, methodName, className, extensions, sequence);

    }
}
