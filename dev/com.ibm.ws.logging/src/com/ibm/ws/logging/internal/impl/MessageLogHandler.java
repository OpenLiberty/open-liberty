/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.ibm.ws.health.center.data.HCGCData;
import com.ibm.ws.http.logging.data.AccessLogData;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.CollectorJsonUtils;
import com.ibm.ws.logging.collector.Formatter;
import com.ibm.ws.logging.internal.impl.BaseTraceService.TraceWriter;
import com.ibm.ws.logging.source.FFDCData;
import com.ibm.ws.logging.source.MessageLogData;
import com.ibm.ws.logging.source.TraceLogData;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.SyncrhonousHandler;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 *
 */
public class MessageLogHandler implements SyncrhonousHandler, Formatter {

    private TraceWriter myTraceWriter;
    private String serverHostName = null;
    private String wlpUserDir = null;
    private String serverName = null;
    private final int MAXFIELDLENGTH = -1; //Unlimited field length
    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.MessageLogHandler";

    /** Latch to handle a corner case where modified() might get called before init() */
    private final CountDownLatch latch = new CountDownLatch(1);

    private CollectorManager collectorMgr = null;

    public MessageLogHandler(String serverName, String wlpUserDir) {
        this.serverName = serverName;
        this.wlpUserDir = wlpUserDir;
        try {
            serverHostName = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws UnknownHostException {
                    return InetAddress.getLocalHost().getHostName();
                }
            });

        } catch (PrivilegedActionException pae) {
            serverHostName = "";
        }
    }

    public void modified(LogProviderConfig config) {
        //LogProviderConfigImpl trConfig = (LogProviderConfigImpl) config;
        // Sources?
        //etc?
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    @Override
    public void init(CollectorManager collectorManager) {
        try {
            this.collectorMgr = collectorManager;
            //Get the source Ids from the task map and subscribe to relevant sources
            List<String> hardCodedSources = new ArrayList<String>();
            hardCodedSources.add(CollectorConstants.GC_SOURCE + "|" + CollectorConstants.MEMORY);
            hardCodedSources.add(CollectorConstants.ACCESS_LOG_SOURCE + "|" + CollectorConstants.MEMORY);
            hardCodedSources.add(CollectorConstants.FFDC_SOURCE + "|" + CollectorConstants.MEMORY);
            collectorMgr.subscribe(this, hardCodedSources);
        } catch (Exception e) {

        }
    }

    @Override
    public void setBufferManager(String sourceId, BufferManager bufferMgr) {
        //Not needed in a Syncrhonized Handler
    }

    @Override
    public void unsetBufferManager(String sourceId, BufferManager bufferMgr) {
        //Not needed in a Syncrhonized Handler
    }

    ///DYKC
    public void setFileLogHolder(TraceWriter trw) {
        myTraceWriter = trw;
    }

    /*
     * Call by Conduit/BMI to write to log as JSON
     * DYKC-temp TEMP TEMP TEMP
     */
    public void writeJsonifiedEvent(Object event) {
        String eventSource = getSourceTypeFromDataObject(event);
        String messageOutput = (String) formatEvent(eventSource, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH);
        myTraceWriter.writeRecord(messageOutput);
    }

    private String getSourceTypeFromDataObject(Object event) {
        if (event instanceof MessageLogData) {
            return CollectorConstants.MESSAGES_SOURCE;
        } else if (event instanceof TraceLogData) {
            return CollectorConstants.TRACE_SOURCE;
        } else if (event instanceof AccessLogData) {
            return CollectorConstants.ACCESS_LOG_SOURCE;
        } else if (event instanceof HCGCData) {
            return CollectorConstants.GC_SOURCE;
        } else if (event instanceof FFDCData) {
            return CollectorConstants.FFDC_SOURCE;
        } else {
            return "";
        }

    }

    /*
     * Direct Call by BTS to write straight to Log.
     */
    public void writeToLogNormal(String messageLogFormat) {
        myTraceWriter.writeRecord(messageLogFormat);
    }

    @Override
    public Object formatEvent(String source, String location, Object event, String[] tags, int maxFieldLength) {
        String eventType = CollectorJsonUtils.getEventType(source, location);
        String jsonStr = CollectorJsonUtils.jsonifyEvent(event, eventType, serverName, wlpUserDir, serverHostName, "1.0", tags,
                                                         MAXFIELDLENGTH);
        return jsonStr;
    }

    public void setServername(String serverName) {
        this.serverName = serverName;
    }

    public void setWlpUserDir(String wlpUserDir) {
        this.wlpUserDir = wlpUserDir;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.collector.manager.SyncrhonousHandler#isSynchronous()
     */
    @Override
    public boolean isSynchronous() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.collector.manager.SyncrhonousHandler#synchronousWrite()
     */
    @Override
    public synchronized void synchronousWrite(Object event) {
        String evensourcetType = getSourceTypeFromDataObject(event);
        String messageOutput = (String) formatEvent(evensourcetType, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH);
        myTraceWriter.writeRecord(messageOutput);
    }

}
