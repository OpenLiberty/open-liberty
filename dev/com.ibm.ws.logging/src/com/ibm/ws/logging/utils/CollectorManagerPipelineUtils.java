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
package com.ibm.ws.logging.utils;

import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.logging.internal.impl.MessageLogHandler;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.wsspi.collector.manager.Handler;

public class CollectorManagerPipelineUtils implements CollectorManagerPipelineBootstrap {
    public static CollectorManagerPipelineUtils collectorMgrPipelineUtils;
    private MessageLogHandler jsonTraceServiceMessageHandler;
    private LogSource logSource = null;
    private TraceSource traceSource = null;
    private BufferManagerImpl logConduit = null;
    private BufferManagerImpl traceConduit = null;
    private boolean isJsonTrService = false;

    /**
     * Create LogSource, TraceSource and their respective conduits.
     */
    public CollectorManagerPipelineUtils() {
        if (logSource == null) {
            logSource = new LogSource();
        }
        if (traceSource == null) {
            traceSource = new TraceSource();
        }
        if (logConduit == null) {
            logConduit = new BufferManagerImpl(10000, "com.ibm.ws.logging.source.message");
        }
        //System.out.println("HandlerUtils.java = created LogConduit/BufferManager + " + logConduit.toString()); //DYKC-debug
        if (traceConduit == null) {
            traceConduit = new BufferManagerImpl(10000, "com.ibm.ws.logging.source.trace");
        }
        logSource.setBufferManager(logConduit); //DYKC-temp methodName?
    }

    public static synchronized CollectorManagerPipelineUtils getInstance() {
        if (collectorMgrPipelineUtils == null) {
            collectorMgrPipelineUtils = new CollectorManagerPipelineUtils();
        }
        return collectorMgrPipelineUtils;
    }

    @Override
    public LogSource getLogSource() {
        return logSource;
    }

    @Override
    public TraceSource getTraceSource() {
        return traceSource;
    }

    @Override
    public BufferManagerImpl getLogConduit() {
        return logConduit;
    }

    @Override
    public BufferManagerImpl getTraceConduit() {
        return traceConduit;
    }

    /*
     * HandlerUtils is not responsible for creating the Handler.
     * This is the responsibility of the BaseTraceService.
     */
    @Override
    public Handler getLogHandler() {
        if (jsonTraceServiceMessageHandler != null)
            return jsonTraceServiceMessageHandler;
        return null;
    }

    /*
     * HandlerUtils is not responsible for creating the Handler.
     * This is the responsibility of the BaseTraceService.
     */
    @Override
    public void setHandler(Handler handler) {
        this.jsonTraceServiceMessageHandler = (MessageLogHandler) handler;
    }

    public boolean getJsonTrService() {
        return isJsonTrService;
    }

    public void setJsonTrService(boolean value) {
        this.isJsonTrService = value;
    }

}
