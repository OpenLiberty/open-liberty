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
import com.ibm.ws.logging.internal.impl.SpecialHandler;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.wsspi.collector.manager.Handler;

public class HandlerUtils implements CollectorManagerBootStrap {
    public static HandlerUtils myHandlerUtils;
    private SpecialHandler baseTraceServiceJSONHandler;
    private LogSource logSource = null;
    private TraceSource traceSource = null;
    private BufferManagerImpl logConduit = null;
    private BufferManagerImpl traceConduit = null;

    public static LogSource retrieveLogSource() {
        SpecialHandler baseTraceServiceJSONHandler = SpecialHandler.getInstance();
        return baseTraceServiceJSONHandler.getLogSource();
    }

    /**
     * Create LogSource, TraceSource and their respective conduits.
     * Handler is set specifically by BaseTraceService
     */
    public HandlerUtils() {
        System.out.println("HandlerUtils.java - I AM HANDLER UTILS AND I AM NOW ALIVE");
        //DYKC-temp create only LogSource for now.
        if (logSource == null) {
            logSource = new LogSource();
        }
        if (traceSource == null) {
            traceSource = new TraceSource();
        }
        if (logConduit == null) {
            logConduit = new BufferManagerImpl(10000, "com.ibm.ws.logging.source.message");
        }
        System.out.println("HandlerUtils.java = created LogConduit/BufferManager + " + logConduit.toString());
        if (traceConduit == null) {
            traceConduit = new BufferManagerImpl(10000, "com.ibm.ws.logging.source.trace");
        }
        logSource.setBufferManager(logConduit); //DYKC-temp Definitely need to change taht method name....
    }

    public static synchronized HandlerUtils getInstance() {
        if (myHandlerUtils == null) {
            myHandlerUtils = new HandlerUtils();
        }
        return myHandlerUtils;
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
        return null;
    }

    /*
     * HandlerUtils is not responsible for creating the Handler.
     * This is the responsibility of the BaseTraceService.
     */
    @Override
    public void setHandler(Handler handler) {
        this.baseTraceServiceJSONHandler = (SpecialHandler) handler;
    }

}
