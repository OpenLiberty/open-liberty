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

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.health.center.data.HCGCData;
import com.ibm.ws.http.logging.data.AccessLogData;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.CollectorJsonUtils;
import com.ibm.ws.logging.collector.Formatter;
import com.ibm.ws.logging.source.FFDCData;
import com.ibm.ws.logging.source.MessageLogData;
import com.ibm.ws.logging.source.TraceLogData;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.SyncrhonousHandler;

/**
 *
 */
public class ConsoleLogHandler implements SyncrhonousHandler, Formatter {

    private PrintStream streamWriter;
    private String serverHostName = null;
    private String wlpUserDir = null;
    private String serverName = null;
    private final int MAXFIELDLENGTH = -1; //Unlimited field length
    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.MessageLogHandler";
    List<String> sourcesList = new ArrayList<String>();
    private CollectorManager collectorMgr = null;

    public ConsoleLogHandler(String serverName, String wlpUserDir) {
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

    public void modified(List<String> newSources) {
        try {

            // old sources
            ArrayList<String> oldSources = new ArrayList<String>(sourcesList);

            //sources to remove -> In Old Sources, the difference between oldSource and newSource
            ArrayList<String> sourcesToRemove = new ArrayList<String>(oldSources);
            sourcesToRemove.removeAll(newSources);
            collectorMgr.unsubscribe(this, sourcesToRemove);

            //sources to Add -> In New Sources, the difference bewteen newSource and oldSource
            ArrayList<String> sourcesToAdd = new ArrayList<String>(newSources);
            newSources.removeAll(oldSources);
            collectorMgr.subscribe(this, sourcesList);

            sourcesList = newSources; //new master sourcesList
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    @Override
    public void init(CollectorManager collectorManager) {
        try {
            this.collectorMgr = collectorManager;
            //DYKC-temp
            //Get the source Ids from the config passed in by JsonTrService
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

    public void setStreamWrtier(PrintStream streamWriter) {
        this.streamWriter = streamWriter;
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

    @Override
    public boolean isSynchronous() {
        return true;
    }

    @Override
    public synchronized void synchronousWrite(Object event) {
        String evensourcetType = getSourceTypeFromDataObject(event);
        String messageOutput = (String) formatEvent(evensourcetType, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH);
        streamWriter.println(messageOutput);
    }

}
