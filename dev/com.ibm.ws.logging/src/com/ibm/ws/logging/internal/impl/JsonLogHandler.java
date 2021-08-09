/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
import com.ibm.ws.logging.collector.CollectorJsonHelpers;
import com.ibm.ws.logging.collector.CollectorJsonUtils;
import com.ibm.ws.logging.collector.Formatter;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

/**
 * An abstract class that defines the common functionality of a json handler services
 */
public abstract class JsonLogHandler implements SynchronousHandler, Formatter {
    protected String serverHostName = null;
    protected String serverName = null;
    protected String wlpUserDir = null;
    protected String wlpServerName = null;

    protected final int MAXFIELDLENGTH = -1; //Unlimited field length
    protected static volatile boolean isInit = false;;

    protected static final String ENV_VAR_CONTAINERHOST = "CONTAINER_HOST";
    protected static final String ENV_VAR_CONTAINERNAME = "CONTAINER_NAME";

    protected static volatile boolean appsWriteJson = false;

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

    /**
     * Constructor for the JsonLoghandler which will establish server information needed
     * to fill in the fields of the JSON data object
     *
     * @param serverName The wlp servername
     * @param wlpUserDir The wlp user directory
     * @param sourcesList The first sourceList to subscribe to collectorManager with
     */
    public JsonLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {

        this.wlpServerName = serverName;
        this.wlpUserDir = wlpUserDir;

        this.sourcesList = sourcesList;

        //Resolve server name to be the DOCKER Container name or the wlp server name.
        String containerName = System.getenv(ENV_VAR_CONTAINERNAME);
        if (containerName == null || containerName.equals("") || containerName.length() == 0) {
            this.serverName = wlpServerName;
        } else {
            this.serverName = containerName;
        }

        //Resolve server name to be the DOCKER HOST name or the cannonical host name.
        String containerHost = System.getenv(ENV_VAR_CONTAINERHOST);
        if (containerHost == null || containerHost.equals("") || containerHost.length() == 0) {
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
            serverHostName = containerHost;
        }

        CollectorJsonHelpers.setHostName(serverHostName);
        CollectorJsonHelpers.setServerName(this.serverName);
        CollectorJsonHelpers.setWlpUserDir(wlpUserDir);

    }

    /**
     * Without osgi, this modified method is called explicility from the update method in
     * JsonTrService
     *
     * @param newSources List of the new source list from config
     */
    public void modified(List<String> newSources) {

        if (collectorMgr == null || isInit == false) {
            this.sourcesList = newSources;
            return;
        }

        try {
            //Old sources
            ArrayList<String> oldSources = new ArrayList<String>(sourcesList);

            //Sources to remove -> In Old Sources, the difference between oldSource and newSource
            ArrayList<String> sourcesToRemove = new ArrayList<String>(oldSources);
            sourcesToRemove.removeAll(newSources);
            collectorMgr.unsubscribe(this, convertToSourceIDList(sourcesToRemove));

            //Sources to Add -> In New Sources, the difference bewteen newSource and oldSource
            ArrayList<String> sourcesToAdd = new ArrayList<String>(newSources);
            sourcesToAdd.removeAll(oldSources);
            collectorMgr.subscribe(this, convertToSourceIDList(sourcesToAdd));

            sourcesList = newSources; //new primary sourcesList
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Setter to set the writer for the handler
     *
     * @param writer Object used to write out
     */
    public abstract void setWriter(Object writer);

    @Override
    public void setBufferManager(String sourceId, BufferManager bufferMgr) {
        //Not needed in a Synchronized Handler
    }

    @Override
    public void unsetBufferManager(String sourceId, BufferManager bufferMgr) {
        //Not needed in a Synchronized Handler
    }

    @Override
    public Object formatEvent(String source, String location, Object event, String[] tags, int maxFieldLength) {

        String eventType = CollectorJsonUtils.getEventType(source, location);
        String jsonStr = CollectorJsonUtils.jsonifyEvent(event, eventType, serverName, wlpUserDir, serverHostName, "JSON", tags,
                                                         MAXFIELDLENGTH);
        return jsonStr;
    }

    public void setWlpServerName(String serverName) {
        this.wlpServerName = serverName;
    }

    public void setWlpUserDir(String wlpUserDir) {
        this.wlpUserDir = wlpUserDir;
    }

    /*
     * Given the sourceList in 'config' form, return the sourceID
     * Proper sourceID format is <source> | <location>
     */
    protected List<String> convertToSourceIDList(List<String> sourceList) {
        List<String> sourceIDList = new ArrayList<String>();
        for (String source : sourceList) {
            String sourceName = getSourceName(source);
            if (!sourceName.equals("")) {
                if (!sourceName.contains("audit")) {
                    sourceIDList.add(getSourceName(source) + "|" + CollectorConstants.MEMORY);
                } else {
                    sourceIDList.add(getSourceName(source) + "|" + CollectorConstants.SERVER);
                }
            }
        }
        return sourceIDList;
    }

    /*
     * Get the fully qualified source string from the config value
     */
    protected String getSourceName(String source) {
        if (source.equals(CollectorConstants.MESSAGES_CONFIG_VAL))
            return CollectorConstants.MESSAGES_SOURCE;
        else if (source.equals(CollectorConstants.FFDC_CONFIG_VAL))
            return CollectorConstants.FFDC_SOURCE;
        else if (source.equals(CollectorConstants.TRACE_CONFIG_VAL))
            return CollectorConstants.TRACE_SOURCE;
        else if (source.equalsIgnoreCase(CollectorConstants.ACCESS_CONFIG_VAL)) {
            return CollectorConstants.ACCESS_LOG_SOURCE;
        } else if (source.equalsIgnoreCase(CollectorConstants.AUDIT_CONFIG_VAL)) {
            return CollectorConstants.AUDIT_LOG_SOURCE;
        }

        return "";
    }

    protected String getSourceNameFromDataObject(Object event) {

        GenericData genData = (GenericData) event;
        String sourceName = genData.getSourceName();

        if (sourceName.equals(CollectorConstants.MESSAGES_SOURCE)) {
            return CollectorConstants.MESSAGES_SOURCE;
        } else if (sourceName.equals(CollectorConstants.TRACE_SOURCE)) {
            return CollectorConstants.TRACE_SOURCE;
        } else if (sourceName.equals(CollectorConstants.ACCESS_LOG_SOURCE)) {
            return CollectorConstants.ACCESS_LOG_SOURCE;
        } else if (sourceName.equals(CollectorConstants.FFDC_SOURCE)) {
            return CollectorConstants.FFDC_SOURCE;
        } else if (sourceName.contains(CollectorConstants.AUDIT_LOG_SOURCE)) {
            return CollectorConstants.AUDIT_LOG_SOURCE;
        } else {
            return "";
        }
    }

    protected static boolean isJSON(String message) {
        return message != null && message.startsWith("{") && message.endsWith("}");

    }

    /**
     * Set apps that write json to true or false
     *
     * @param appsWriteJson Allow apps to write JSON to System.out/System.err
     */
    public void setAppsWriteJson(boolean appsWriteJson) {
        JsonLogHandler.appsWriteJson = appsWriteJson;
    }

}
