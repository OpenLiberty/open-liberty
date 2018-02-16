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

import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.Formatter;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.ws.logging.internal.impl.BaseTraceService.TraceWriter;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

/**
 *
 */
public class MessageLogHandler extends JsonLogHandler implements SynchronousHandler, Formatter {

    private TraceWriter traceWriter;
    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.MessageLogHandler";

    private String format = LoggingConstants.DEFAULT_MESSAGE_FORMAT;
    private BaseTraceFormatter formatter = null;

    public MessageLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {
        super(serverName, wlpUserDir, sourcesList);
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
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
         * Given an 'object' we must determine what type of log event it originates from.
         * Knowing that it is a *Data object, we can figure what type of source it is.
         */
        GenericData genData = null;
        //check if event is a LogTraceData
        if (event instanceof LogTraceData) {
            genData = ((LogTraceData) event).getGenData();
        } else if (event instanceof GenericData) {
            genData = (GenericData) event;
        }

        String evensourcetType = getSourceTypeFromDataObject(genData);
        String messageOutput = null;
        if (format.equals(LoggingConstants.JSON_FORMAT)) {
            messageOutput = (String) formatEvent(evensourcetType, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
        } else if (format.equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT) && formatter != null) {
            messageOutput = formatter.messageLogFormat(genData);

        }
        if (messageOutput != null && traceWriter != null) {
            traceWriter.writeRecord(messageOutput);
        }

    }

    public BaseTraceFormatter getFormatter() {
        return formatter;
    }

    public void setFormatter(BaseTraceFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * @param format the format to set
     */
    public void setFormat(String format) {
        this.format = format;
    }

}
