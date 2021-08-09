/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.logprovider;

import java.util.logging.LogRecord;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.WsTraceRouter;
import com.ibm.wsspi.logging.MessageRouter;

/**
 * Interface for pluggable Tr implementation.
 * 
 * Services that implement this interface and register as TrService providers
 * will be delegated to by the static methods in com.ibm.websphere.ras.Tr.
 */
public interface TrService {
    /**
     * Called to initialize the TrService implementation. In OSGi environments,
     * this is driven by the activation of the RAS bundle. Outside of OSGi, Tr
     * will call init with the first call to Tr.getDelegate()
     */
    void init(LogProviderConfig config);

    /**
     * Print the input message and arguments if the provided trace component
     * allows audit level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void audit(TraceComponent tc, String msg, Object... args);

    /**
     * Print the input message and arguments if the provided trace component
     * allows debug level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void debug(TraceComponent tc, String msg, Object... args);

    void debug(TraceComponent tc, Object id, String msg, Object... objs);

    /**
     * Print the input message and arguments if the provided trace component
     * allows dump level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void dump(TraceComponent tc, String msg, Object... args);

    /**
     * Print the input message and arguments if the provided trace component
     * allows entry level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void entry(TraceComponent tc, String msg, Object... args);

    void entry(TraceComponent tc, Object id, String methodName, Object... objs);

    /**
     * Print the input message and arguments if the provided trace component
     * allows error level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void error(TraceComponent tc, String msg, Object... args);

    /**
     * Print the input message and arguments if the provided trace component
     * allows event level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void event(TraceComponent tc, String msg, Object... args);

    void event(TraceComponent tc, Object id, String msg, Object... objs);

    /**
     * Print the input message and arguments if the provided trace component
     * allows exit level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void exit(TraceComponent tc, String msg, Object o);

    void exit(TraceComponent tc, Object id, String methodName, Object o);

    void exit(TraceComponent tc, String msg);

    void exit(TraceComponent tc, Object id, String methodName);

    /**
     * Print the input message and arguments if the provided trace component
     * allows fatal level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void fatal(TraceComponent tc, String msg, Object... args);

    /**
     * Print the input message and arguments if the provided trace component
     * allows info level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void info(TraceComponent tc, String msg, Object... args);

    /**
     * Register a new trace component with the service.
     * 
     * @param tc
     */
    void register(TraceComponent tc);

    /**
     * Print the input message and arguments if the provided trace component
     * allows warning level tracing.
     * 
     * @param tc
     * @param msg
     * @param args
     */
    void warning(TraceComponent tc, String msg, Object... args);

    /**
     * Stop delegate service
     */
    void stop();

    /**
     * Update the trace service with values contained in map. This map should be
     * updated with any value changes (e.g. with the actual value used in case
     * of an error).
     * 
     * @param logProviderConfig
     */
    void update(LogProviderConfig logProviderConfig);

    /**
     * Setter for a MessageRouter implementation.
     */
    void setMessageRouter(MessageRouter msgRouter);

    /**
     * Unset a MessageRouter implementation.
     */
    void unsetMessageRouter(MessageRouter msgRouter);

    /**
     * Setter for a TraceRouter implementation.
     */
    void setTraceRouter(WsTraceRouter traceRouter);

    /**
     * Unset a TraceRouter implementation.
     */
    void unsetTraceRouter(WsTraceRouter traceRouter);

    /**
     * Publishes log records to this service.
     * 
     * @param logRecord
     */
    void publishLogRecord(LogRecord logRecord);
    
}
