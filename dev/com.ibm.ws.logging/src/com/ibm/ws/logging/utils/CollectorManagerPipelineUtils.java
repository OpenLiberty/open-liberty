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
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.internal.impl.ConsoleLogHandler;
import com.ibm.ws.logging.internal.impl.MessageLogHandler;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.wsspi.collector.manager.Handler;

public class CollectorManagerPipelineUtils implements CollectorManagerPipelineBootstrap {
    public static CollectorManagerPipelineUtils collectorMgrPipelineUtils;
    private MessageLogHandler messageLogHandler;
    private ConsoleLogHandler consoleLogHandler;
    private LogSource logSource = null;
    private TraceSource traceSource = null;
    private BufferManagerImpl logConduit = null;
    private BufferManagerImpl traceConduit = null;

    /**
     * Create LogSource, TraceSource and their respective conduits and sets the conduit into the respective sources.
     * In effect this creates the Source + Conduit portion of the pipeline.
     * The rest of the pipline (i.e the handler) is created in the JsonTraceService and will hook into the pipeline
     * there
     * If HPEL or JSR47 TrServices are active, then only the source and conduit/bufferManager portion of the pipeline
     * will be activated in anticpation for consumption by Logstash,LogMet,Audit or GC.
     */
    public CollectorManagerPipelineUtils() {
        if (logSource == null) {
            logSource = new LogSource();
        }
        if (traceSource == null) {
            traceSource = new TraceSource();
        }
        if (logConduit == null) {
            logConduit = new BufferManagerImpl(10000, CollectorConstants.MESSAGES_SOURCE);
        }

        if (traceConduit == null) {
            traceConduit = new BufferManagerImpl(10000, CollectorConstants.TRACE_SOURCE);
        }

        /*
         * Set the conduit/BufferManger into their respective sources
         * Typically, this would have been set via osgi reference/dependencies that were
         * defined in the bnd.bnd file of CollectorManager, but for the Json Logging feature,
         * the LogSource and TraceSource are created earlier than osgi is available and will
         * need to have the Conduit/BufferManager set in manually.
         */
        logSource.setBufferManager(logConduit);
        traceSource.setBufferManager(traceConduit);
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

    /**
     *
     * HandlerUtils is not responsible for creating the Handler.
     * This is the responsibility of the BaseTraceService.
     */
    @Override
    public Handler getMessageLogHandler() {
        if (messageLogHandler != null)
            return messageLogHandler;
        return null;
    }

    /**
     * HandlerUtils is not responsible for creating the Handler.
     * This is the responsibility of the BaseTraceService.
     */
    @Override
    public void setMessageHandler(Handler handler) {
        this.messageLogHandler = (MessageLogHandler) handler;
    }

    @Override
    public Handler getConsoleLogHandler() {
        if (consoleLogHandler != null)
            return consoleLogHandler;
        return null;
    }

    @Override
    public void setConsoleHandler(Handler handler) {
        this.consoleLogHandler = (ConsoleLogHandler) handler;
    }

}
