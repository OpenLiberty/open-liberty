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
import com.ibm.ws.logging.data.AccessLogData;
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

                AccessLogData accessLogData = new AccessLogData();

                long requestStartTimeVal = recordData.getStartTime();
                accessLogData.setRequestStartTime(requestStartTimeVal);
                accessLogData.setUriPath(request.getRequestURI());
                accessLogData.setRequestMethod(request.getMethod());
                accessLogData.setQueryString(request.getQueryString());
                accessLogData.setRequestHost(recordData.getLocalIP());
                accessLogData.setRequestPort(recordData.getLocalPort());
                accessLogData.setRemoteHost(recordData.getRemoteAddress());
                accessLogData.setUserAgent(request.getHeader(USER_AGENT_HEADER).asString());
                accessLogData.setRequestProtocol(request.getVersion());
                accessLogData.setBytesReceived(recordData.getBytesWritten());
                accessLogData.setResponseCode(response.getStatusCodeAsInt());
                accessLogData.setElapsedTime(recordData.getElapsedTime());
                accessLogData.setDatetime(recordData.getTimestamp());

                String sequenceVal = requestStartTimeVal + "_" + String.format("%013X", seq.incrementAndGet());
                accessLogData.setSequence(sequenceVal);

                accessLogData.setSourceName(sourceName);

                bufferMgr.add(accessLogData);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added a event to buffer " + accessLogData);
                }
            }
        }
    }
}
