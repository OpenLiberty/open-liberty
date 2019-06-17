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
import com.ibm.ws.logging.collector.CollectorJsonHelpers;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.synch.ThreadLocalHandler;
import com.ibm.ws.logging.utils.LogFormatUtils;
import com.ibm.ws.logging.utils.SequenceNumber;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;

public class TraceSource implements Source {
    private static final TraceComponent tc = Tr.register(TraceSource.class);
    private final SequenceNumber sequenceNumber = new SequenceNumber();
    private final String sourceName = "com.ibm.ws.logging.source.trace";
    private final String location = "memory";
    private BufferManager bufferMgr = null;
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
    public void publish(RoutedMessage routedMessage, Object id) {
        //Publish the trace if it is not coming from a handler thread
        if (!ThreadLocalHandler.get()) {
            if (routedMessage.getLogRecord() != null && bufferMgr != null) {
                bufferMgr.add(parse(routedMessage, id));
            }
        }
    }

    public LogTraceData parse(RoutedMessage routedMessage, Object id) {

        LogTraceData traceData = new LogTraceData();
        LogRecord logRecord = routedMessage.getLogRecord();

        long datetimeValue = logRecord.getMillis();
        traceData.setDatetime(datetimeValue);
        traceData.setMessageId(null); // needs to be null to satisfy HandlerTest.java testMessageSource()
        traceData.setThreadId(logRecord.getThreadID());
        traceData.setModule(logRecord.getLoggerName());
        traceData.setSeverity(LogFormatUtils.mapLevelToType(logRecord));
        traceData.setLoglevel(LogFormatUtils.mapLevelToRawType(logRecord));
        traceData.setMethodName(logRecord.getSourceMethodName());
        traceData.setClassName(logRecord.getSourceClassName());
        traceData.setLevelValue(logRecord.getLevel().intValue());

        String threadName = Thread.currentThread().getName();
        traceData.setThreadName(threadName);

        WsLogRecord wsLogRecord = getWsLogRecord(logRecord);
        //Extensions KVP calculated below, but needs to bet set in the correct 'order' further below
        KeyValuePairList extensions = null;
        if (wsLogRecord != null) {
            traceData.setCorrelationId(wsLogRecord.getCorrelationId());
            traceData.setOrg(wsLogRecord.getOrganization());
            traceData.setProduct(wsLogRecord.getProduct());
            traceData.setComponent(wsLogRecord.getComponent());
            if (wsLogRecord.getExtensions() != null) {
                extensions = new KeyValuePairList(LogFieldConstants.EXTENSIONS_KVPL);
                Map<String, String> extMap = wsLogRecord.getExtensions();
                for (Map.Entry<String, String> entry : extMap.entrySet()) {
                    CollectorJsonHelpers.handleExtensions(extensions, entry.getKey(), entry.getValue());
                }
            }
        }

        traceData.setRawSequenceNumber(sequenceNumber.getRawSequenceNumber());

        String verboseMessage = routedMessage.getFormattedVerboseMsg();
        if (verboseMessage == null) {
            traceData.setMessage(logRecord.getMessage());
        } else {
            traceData.setMessage(verboseMessage);
        }

        traceData.setExtensions(extensions);

        if (id != null) {
            int objid = System.identityHashCode(id);
            traceData.setObjectId(objid);
        }

        traceData.setSourceName(sourceName);

        return traceData;
    }

    private WsLogRecord getWsLogRecord(LogRecord logRecord) {
        return (logRecord instanceof WsLogRecord) ? (WsLogRecord) logRecord : null;
    }
}
