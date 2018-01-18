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
package com.ibm.ws.http.logging.source;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.Pair;
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
                KeyValuePair requestStartTime = new KeyValuePair("ibm_requestStartTime", Long.toString(recordData.getStartTime()), KeyValuePair.ValueTypes.NUMBER);
                KeyValuePair uriPath = new KeyValuePair("ibm_uriPath", request.getRequestURI(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair requestMethod = new KeyValuePair("ibm_requestMethod", request.getMethod(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair queryString = new KeyValuePair("ibm_queryString", request.getQueryString(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair localIP = new KeyValuePair("ibm_requestHost", recordData.getLocalIP(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair localPort = new KeyValuePair("ibm_requestPort", recordData.getLocalPort(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair remoteHost = new KeyValuePair("ibm_remoteHost", recordData.getRemoteAddress(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair userAgent = new KeyValuePair("ibm_userAgent", request.getHeader("User-Agent").asString(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair requestProtocol = new KeyValuePair("ibm_requestProtocol", request.getVersion(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair responseSize = new KeyValuePair("ibm_bytesReceived", Long.toString(recordData.getBytesWritten()), KeyValuePair.ValueTypes.NUMBER);
                KeyValuePair responseCode = new KeyValuePair("ibm_responseCode", Integer.toString(response.getStatusCodeAsInt()), KeyValuePair.ValueTypes.NUMBER);
                KeyValuePair elapsedTime = new KeyValuePair("ibm_elapsedTime", Long.toString(recordData.getElapsedTime()), KeyValuePair.ValueTypes.NUMBER);
                KeyValuePair timestamp = new KeyValuePair("ibm_datetime", Long.toString(recordData.getTimestamp()), KeyValuePair.ValueTypes.NUMBER);

                String sequenceVal = requestStartTime + "_" + String.format("%013X", seq.incrementAndGet());
                KeyValuePair sequence = new KeyValuePair("ibm_sequence", sequenceVal, KeyValuePair.ValueTypes.STRING);

                GenericData genData = new GenericData();
                ArrayList<Pair> pairs = genData.getPairs();

                pairs.add(requestStartTime);
                pairs.add(uriPath);
                pairs.add(requestMethod);
                pairs.add(queryString);
                pairs.add(localIP);
                pairs.add(localPort);
                pairs.add(remoteHost);
                pairs.add(userAgent);
                pairs.add(requestProtocol);
                pairs.add(responseSize);
                pairs.add(responseCode);
                pairs.add(elapsedTime);
                pairs.add(timestamp);
                pairs.add(sequence);

                genData.setSourceType(sourceName);

                bufferMgr.add(genData);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added a event to buffer " + genData);
                }
            }
        }
    }
}
