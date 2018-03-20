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
package com.ibm.ws.http.logging.source;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.logging.AccessLogForwarder;
import com.ibm.wsspi.http.logging.AccessLogRecordData;
import com.ibm.wsspi.http.logging.LogForwarderManager;

/**
 *
 */
public class AccessLogSource implements Source {

    private static final TraceComponent tc = Tr.register(AccessLogSource.class);

    private final String sourceName = "com.ibm.ws.http.logging.source.accesslog";
    private final String location = "memory";
    private static String USER_AGENT_HEADER = "User-Agent";

    private BufferManager bufferMgr = null;
    private AccessLogHandler accessLogHandler;

    protected synchronized void activate(Map<String, Object> configuration) {
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
    public void setBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting buffer manager " + this);
        }
        this.bufferMgr = bufferMgr;
        startSource();
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Un-setting buffer manager " + this);
        }
        //Indication that the buffer will no longer be available
        stopSource();
        this.bufferMgr = null;
    }

    /**
     *
     */
    private void startSource() {
        accessLogHandler = new AccessLogHandler();
        LogForwarderManager.registerAccessLogForwarder(accessLogHandler);
    }

    /**
     *
     */
    private void stopSource() {
        LogForwarderManager.deregisterAccessLogForwarder(accessLogHandler);
        accessLogHandler = null;
    }

    private class AccessLogHandler implements AccessLogForwarder {

        private final AtomicLong seq = new AtomicLong();

        /** {@inheritDoc} */
        @Override
        public void process(AccessLogRecordData recordData) {
            HttpRequestMessage request = recordData.getRequest();
            HttpResponseMessage response = recordData.getResponse();

            if (request != null) {

                GenericData genData = new GenericData();

                long requestStartTimeVal = recordData.getStartTime();
                genData.addPair(LogFieldConstants.IBM_REQUESTSTARTTIME, requestStartTimeVal);
                genData.addPair(LogFieldConstants.IBM_URIPATH, request.getRequestURI());
                genData.addPair(LogFieldConstants.IBM_REQUESTMETHOD, request.getMethod());
                genData.addPair(LogFieldConstants.IBM_QUERYSTRING, request.getQueryString());
                genData.addPair(LogFieldConstants.IBM_REQUESTHOST, recordData.getLocalIP());
                genData.addPair(LogFieldConstants.IBM_REQUESTPORT, recordData.getLocalPort());
                genData.addPair(LogFieldConstants.IBM_REMOTEHOST, recordData.getRemoteAddress());
                genData.addPair(LogFieldConstants.IBM_USERAGENT, request.getHeader(USER_AGENT_HEADER).asString());
                genData.addPair(LogFieldConstants.IBM_REQUESTPROTOCOL, request.getVersion());
                genData.addPair(LogFieldConstants.IBM_BYTESRECEIVED, recordData.getBytesWritten());
                genData.addPair(LogFieldConstants.IBM_RESPONSECODE, response.getStatusCodeAsInt());
                genData.addPair(LogFieldConstants.IBM_ELAPSEDTIME, recordData.getElapsedTime());
                genData.addPair(LogFieldConstants.IBM_DATETIME, recordData.getTimestamp());

                String sequenceVal = requestStartTimeVal + "_" + String.format("%013X", seq.incrementAndGet());
                genData.addPair(LogFieldConstants.IBM_SEQUENCE, sequenceVal);

                genData.setSourceType(sourceName);

                bufferMgr.add(genData);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added a event to buffer " + genData);
                }
            }
        }
    }
}
