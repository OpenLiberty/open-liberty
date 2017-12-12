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

import java.util.List;

import com.ibm.ws.logging.internal.impl.BaseTraceService.TraceWriter;
import com.ibm.ws.logging.temp.collector.CollectorConstants;
import com.ibm.ws.logging.temp.collector.Formatter;
import com.ibm.wsspi.collector.manager.SyncrhonousHandler;

/**
 *
 */
public class MessageLogHandler extends JsonLogHandler implements SyncrhonousHandler, Formatter {

    private TraceWriter traceWriter;
    /*
     * Needed to address a synchronization issue between the syncrhonousWrite method and FileLogHolder
     */
    private volatile Object sync;
    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.MessageLogHandler";

    public MessageLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {
        super(serverName, wlpUserDir, sourcesList);
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    public void setSync(Object sync) {
        this.sync = sync;
    }

    public void setFileLogHolder(TraceWriter trw) {
        traceWriter = trw;
    }

    @Override
    public void setWriter(Object writer) {
        this.traceWriter = (TraceWriter) writer;
    }

    @Override
    public void synchronousWrite(Object event) {
        /*
         * Needed to address a synchronization issue between the syncrhonousWrite method and FileLogHolder
         */

        /*
         * Given an 'object' we must determine what type of log event it originates from.
         * Knowing that it is a *Data object, we can figure what type of source it is.
         */
        String evensourcetType = getSourceTypeFromDataObject(event);
        String messageOutput = (String) formatEvent(evensourcetType, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH);
        synchronized (sync) {
            traceWriter.writeRecord(messageOutput);
        }
    }

}
