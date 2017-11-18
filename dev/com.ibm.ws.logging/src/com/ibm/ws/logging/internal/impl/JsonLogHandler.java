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
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.CollectorJsonUtils;
import com.ibm.ws.logging.collector.Formatter;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.SyncrhonousHandler;

/**
 *
 */
public abstract class JsonLogHandler implements SyncrhonousHandler, Formatter {

    protected String serverHostName = null;
    protected String serverName = null;
    protected String wlpUserDir = null;
    protected String wlpServerName = null;

    protected final int MAXFIELDLENGTH = -1; //Unlimited field length
    protected static volatile boolean isInit = false;;

    protected static final String ENV_VAR_CONTAINERHOST = "CONTAINER_HOST";
    protected static final String ENV_VAR_CONTAINERNAME = "CONTAINER_NAME";

    List<String> sourcesList = new ArrayList<String>();

    protected CollectorManager collectorMgr = null;

    @Override
    public void init(CollectorManager collectorManager) {
        try {
            this.collectorMgr = collectorManager;
            collectorMgr.subscribe(this, convertToSourceIDList(sourcesList));
            isInit = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JsonLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {

        this.wlpServerName = serverName;
        this.wlpUserDir = wlpUserDir;

        this.sourcesList = sourcesList;

        //Resolve server name to be the DOCKER Container name or the wlp server name.
        String containerName = System.getenv(ENV_VAR_CONTAINERNAME);
        if (containerName == null || containerName.equals("") || containerName.length() == 0) {
            serverName = wlpServerName;
        } else {
            serverName = containerName;
        }

        //Resolve server name to be the DOCKER HOST name or the cannonical host name.
        String containerHost = System.getenv(ENV_VAR_CONTAINERHOST);
        if (containerName == null || containerName.equals("") || containerName.length() == 0) {
            try {
                serverHostName = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws UnknownHostException {
                        return InetAddress.getLocalHost().getCanonicalHostName();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                serverHostName = "";
            }
        } else {
            serverName = containerName;
        }

    }

    public void modified(List<String> newSources) {

        if (collectorMgr == null || isInit == false) {
            this.sourcesList = newSources;
            return;
        }

        try {

            // old sources
            ArrayList<String> oldSources = new ArrayList<String>(sourcesList);
            //sources to remove -> In Old Sources, the difference between oldSource and newSource
            ArrayList<String> sourcesToRemove = new ArrayList<String>(oldSources);
            sourcesToRemove.removeAll(newSources);
            collectorMgr.unsubscribe(this, convertToSourceIDList(sourcesToRemove));

            //sources to Add -> In New Sources, the difference bewteen newSource and oldSource
            ArrayList<String> sourcesToAdd = new ArrayList<String>(newSources);
            sourcesToAdd.removeAll(oldSources);
            collectorMgr.subscribe(this, convertToSourceIDList(sourcesToAdd));

            sourcesList = newSources; //new master sourcesList

        } catch (Exception e) {
            e.printStackTrace();
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

    @Override
    public Object formatEvent(String source, String location, Object event, String[] tags, int maxFieldLength) {
        String eventType = CollectorJsonUtils.getEventType(source, location);
        String jsonStr = CollectorJsonUtils.jsonifyEvent(event, eventType, wlpServerName, wlpUserDir, serverHostName, "1.1", tags,
                                                         MAXFIELDLENGTH);
        return jsonStr;
    }

    public void setWlpServerName(String serverName) {
        this.wlpServerName = serverName;
    }

    public void setWlpUserDir(String wlpUserDir) {
        this.wlpUserDir = wlpUserDir;
    }

    protected List<String> convertToSourceIDList(List<String> sourceList) {
        List<String> sourceIDList = new ArrayList<String>();
        for (String source : sourceList) {
            String sourceName = getSourceName(source);
            if (!sourceName.equals("")) {
                sourceIDList.add(getSourceName(source) + "|" + CollectorConstants.MEMORY);
            }
        }
        return sourceIDList;
    }

    protected String getSourceName(String source) {
        if (source.equals(CollectorConstants.MESSAGES_CONFIG_VAL))
            return CollectorConstants.MESSAGES_SOURCE;
        else if (source.equals(CollectorConstants.FFDC_CONFIG_VAL))
            return CollectorConstants.FFDC_SOURCE;
        else if (source.equals(CollectorConstants.TRACE_CONFIG_VAL))
            return CollectorConstants.TRACE_SOURCE;
        else if (source.equalsIgnoreCase(CollectorConstants.ACCESS_CONFIG_VAL))
            return CollectorConstants.ACCESS_LOG_SOURCE;
        return "";
    }
}
