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

import com.ibm.ws.collector.CollectorConstants;
import com.ibm.ws.collector.CollectorJsonUtils;
import com.ibm.ws.collector.Formatter;
import com.ibm.ws.logging.internal.impl.BaseTraceService.TraceWriter;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.MessageLogData;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Handler;

/**
 *
 */
public class SpecialHandler implements Handler, Formatter {

    private TraceWriter myTRW;
    private BaseTraceFormatter myBTF;
    public static SpecialHandler mySpecialHandler;
    private LogSource ls;
    private String serverHostName = null;
    private String wlpUserDir = null;
    private String serverName = null;

    private SpecialHandler() {
        //gather necessary information/data for JSONIFYING

        //hostname -> localhost
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

        //
    }

    public static synchronized SpecialHandler getInstance() {
        if (mySpecialHandler == null) {
            mySpecialHandler = new SpecialHandler();
        }
        return mySpecialHandler;
    }

    public void setLogSource(LogSource ls) {
        if (ls == null) {
            System.out.println("SpecialHandler - setLogSource: ls is null");
        } else {
            System.out.println("SpecialHandler - setLogSource: SETTING LS");
            this.ls = ls;
        }

    }

    public LogSource getLogSource() {
        if (ls == null)
            System.out.println("SpecialHandler - getLogSource: ls is null");
        return ls;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.collector.manager.Handler#getHandlerName()
     */
    @Override
    public String getHandlerName() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.collector.manager.Handler#init(com.ibm.wsspi.collector.manager.CollectorManager)
     */
    @Override
    public void init(CollectorManager collectorManager) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.collector.manager.Handler#setBufferManager(java.lang.String, com.ibm.wsspi.collector.manager.BufferManager)
     */
    @Override
    public void setBufferManager(String sourceId, BufferManager bufferMgr) {
        System.out.println("Specialhandler.java - settingBuffermanager from " + bufferMgr.toString());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.collector.manager.Handler#unsetBufferManager(java.lang.String, com.ibm.wsspi.collector.manager.BufferManager)
     */
    @Override
    public void unsetBufferManager(String sourceId, BufferManager bufferMgr) {
        // TODO Auto-generated method stub

    }

    ///DYKC
    public void setFileLogHolder(TraceWriter trw) {
        myTRW = trw;
    }

    public void setBaseTraceFormatter(BaseTraceFormatter btf) {
        myBTF = btf;
    }

    public void writeToLog(Object event) {
        MessageLogData mld = (MessageLogData) event;
        //String messageLogFormat = myBTF.formatMessage(logRecord); //mld.getMessage();
        formatEvent("source", "", event, null, 1000);
        ///myTRW.writeRecord("");
    }

    public void writeToLogNormal(String messageLogFormat) {
        myTRW.writeRecord(messageLogFormat);
    }

    @Override
    public Object formatEvent(String source, String location, Object event, String[] tags, int maxFieldLength) {
        String jsonStr = CollectorJsonUtils.jsonifyEvent(event, CollectorConstants.MESSAGES_LOG_EVENT_TYPE, serverName, wlpUserDir, serverHostName, "1.0", tags,
                                                         maxFieldLength);
        myTRW.writeRecord(jsonStr);
        return null;
    }

    public void setServername(String serverName) {
        this.serverName = serverName;
    }

    public void setWlpUserDir(String wlpUserDir) {
        this.wlpUserDir = wlpUserDir;
    }

}
